package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zidong.common.constant.PromptConstants;
import com.zidong.common.exception.BizException;
import com.zidong.common.result.PageResult;
import com.zidong.dao.dto.AddPasswordWindowDto;
import com.zidong.dao.dto.ChatHistoryPageQueryDto;
import com.zidong.dao.dto.SaveAppChatMemoryDto;
import com.zidong.dao.dto.SaveAppChatOptionsDto;
import com.zidong.dao.dto.SaveAppPromptDto;
import com.zidong.dao.dto.UpdateBotCredentialScannerDto;
import com.zidong.dao.entity.AppAccessWindow;
import com.zidong.dao.entity.AppPasswordWindow;
import com.zidong.dao.entity.BotCredential;
import com.zidong.dao.entity.ChatHistory;
import com.zidong.dao.vo.AppSettingVO;
import com.zidong.dao.vo.BotCredentialVO;
import com.zidong.dao.vo.PasswordWindowVO;
import com.zidong.dao.mapper.AppAccessWindowMapper;
import com.zidong.dao.mapper.AppChatOptionsMapper;
import com.zidong.dao.mapper.AppPasswordWindowMapper;
import com.zidong.dao.mapper.AppPromptMapper;
import com.zidong.dao.mapper.BotCredentialMapper;
import com.zidong.dao.mapper.ChatHistoryMapper;
import com.zidong.dao.support.ChatHistorySegmentMerger;
import com.zidong.manage.support.PageResults;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 应用设置（提示词等） */
@Service
@RequiredArgsConstructor
public class AppSettingService {

    /** 账号密码窗口的初始密码，创建后不返回给前端 */
    private static final String DEFAULT_PASSWORD = "1234llm";

    /** chat_history.channel：免登陆窗口。标签为 app_access_window.id */
    private static final int CHANNEL_WINDOW = 2;

    /** chat_history.channel：账号密码登录窗口。标签为 app_password_window.id */
    private static final int CHANNEL_ACCOUNT = 3;

    /** chat_history.channel：微信个人号。标签为 bot_credential.id */
    private static final int CHANNEL_WECHAT_PERSONAL = 4;

    private final AppPromptMapper appPromptMapper;
    private final AppChatOptionsMapper appChatOptionsMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final AppAccessWindowMapper appAccessWindowMapper;
    private final AppPasswordWindowMapper passwordWindowMapper;
    private final BotCredentialMapper botCredentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppService appService;

    /**
     * 保存应用提示词。
     *
     * <p>一个应用只保留一条提示词：app_prompt.app_id 上有唯一键，
     * 已存在时由 MySQL 的 ON DUPLICATE KEY UPDATE 直接覆盖，无需先查再判断新增 / 修改。
     *
     * @param request 保存请求，提示词必须含必需占位符，且 appId 对应的应用必须存在
     */
    @Transactional
    public void savePrompt(SaveAppPromptDto request) {
        appService.checkExists(request.getAppId());
        appPromptMapper.saveOrUpdateByAppId(request.toSave());
    }

    /**
     * 应用设置详情。
     *
     * <p>app_id 上有唯一键，一个应用最多一条提示词，未配置时返回空的提示词内容。
     *
     * @param appId 应用 ID，必须存在
     * @return 应用设置详情
     */
    public AppSettingVO info(Long appId) {
        appService.checkExists(appId);
        return AppSettingVO.of(appPromptMapper.findLatestByAppId(appId),
                appChatOptionsMapper.findByAppId(appId));
    }

    /**
     * 保存应用模型对话参数。
     *
     * <p>按 appId 更新 app_chat_options 中的采样温度、nucleus 采样与最大输出 token。
     *
     * @param request 保存请求，appId 对应的应用必须存在
     */
    @Transactional
    public void saveChatOpts(SaveAppChatOptionsDto request) {
        appService.checkExists(request.getAppId());
        appChatOptionsMapper.saveByAppId(request);
    }

    /**
     * 保存应用对话配置（会话记忆开关与轮数）。
     *
     * <p>按 appId 只更新 app_chat_options 中的对话记忆相关字段，采样温度等模型参数不受影响。
     *
     * @param request 保存请求，appId 对应的应用必须存在
     */
    @Transactional
    public void saveChatMemory(SaveAppChatMemoryDto request) {
        appService.checkExists(request.getAppId());
        appChatOptionsMapper.saveMemoryByAppId(request);
    }

    /**
     * 删除应用提示词。
     *
     * <p>删除后该应用回到「未配置提示词」状态：命中答案时直接返回标准答案，不再调用大模型润色。
     * 应用本身必须存在；应用本来就没有提示词时按成功处理，重复删除不会报错。
     *
     * @param appId 应用 ID，必须存在
     */
    @Transactional
    public void deletePrompt(Long appId) {
        appService.checkExists(appId);
        appPromptMapper.deleteByAppId(appId);
    }

    /**
     * 创建账号密码窗口，返回新增记录 ID。
     *
     * <p>只接收窗口名称与登录账号，密码由服务端统一初始化为默认口令并以 BCrypt 密文落库，
     * 创建后窗口即为启用状态；同一应用内窗口名与登录账号都不能重复。
     *
     * @param appId   应用 ID，必须存在
     * @param request 新增请求，含窗口名称与登录账号
     * @return 新增窗口的记录 ID
     */
    @Transactional
    public Long createPasswordWindow(Long appId, AddPasswordWindowDto request) {
        appService.checkExists(appId);
        String name = request.getName().trim();
        String username = request.getUsername().trim();
        // 同一应用内窗口名唯一
        ensureWindowNameUnique(appId, name);
        // 同一应用内登录账号唯一
        ensureWindowUsernameUnique(appId, username);

        AppPasswordWindow window = request.toSave(appId,
                passwordEncoder.encode(DEFAULT_PASSWORD));
        passwordWindowMapper.insert(window);
        return window.getId();
    }

    /**
     * 切换账号密码窗口的启用/禁用状态：调用一次即反转。
     *
     * <p>启用变禁用、禁用变启用，前端无需先查当前状态；窗口必须存在。
     *
     * @param id 窗口 ID，必须存在
     */
    @Transactional
    public void togglePasswordWindowStatus(Long id) {
        AppPasswordWindow window = passwordWindowMapper.selectById(id);
        if (window == null) {
            throw BizException.notFound("账号密码窗口不存在：" + id);
        }
        // 当前为启用则改为禁用，其余（含禁用、历史脏数据）一律改为启用
        Integer nextStatus = Objects.equals(window.getStatus(), AppPasswordWindow.STATUS_ENABLED)
                ? AppPasswordWindow.STATUS_DISABLED
                : AppPasswordWindow.STATUS_ENABLED;
        passwordWindowMapper.updateStatus(id, nextStatus);
    }

    /**
     * 删除账号密码窗口（连同其登录账号）。
     *
     * <p>窗口必须存在，否则抛业务异常。
     *
     * @param id 窗口 ID，必须存在
     */
    @Transactional
    public void deletePasswordWindow(Long id) {
        AppPasswordWindow window = passwordWindowMapper.selectById(id);
        if (window == null) {
            throw BizException.notFound("账号密码窗口不存在：" + id);
        }
        passwordWindowMapper.deleteById(id);
    }

    /**
     * 重置账号密码窗口的登录密码。
     *
     * <p>重置为初始默认口令（1234llm）并以 BCrypt 密文落库，与创建时的初始密码一致；
     * 窗口必须存在，窗口的启用/禁用状态不受影响。
     *
     * @param id 窗口 ID，必须存在
     */
    @Transactional
    public void resetPasswordWindowPassword(Long id) {
        AppPasswordWindow window = passwordWindowMapper.selectById(id);
        if (window == null) {
            throw BizException.notFound("账号密码窗口不存在：" + id);
        }
        passwordWindowMapper.updatePassword(id, passwordEncoder.encode(DEFAULT_PASSWORD));
    }

    /**
     * 账号密码窗口列表，不分页，按创建时间倒序。
     *
     * <p>只返回窗口名称、登录账号等展示字段，不返回密码。
     *
     * @param appId 应用 ID，必须存在
     * @return 该应用下的账号密码窗口列表，最新创建的在前
     */
    public List<PasswordWindowVO> passwordWindowList(Long appId) {
        appService.checkExists(appId);
        return passwordWindowMapper.selectByAppIdOrderByCreatedDesc(appId).stream()
                .map(PasswordWindowVO::of)
                .toList();
    }

    /**
     * 微信个人号（Bot 登录凭证）列表，不分页，最新绑定的在前。
     *
     * <p>只返回渲染列表需要的字段，不返回 bot_token 等敏感信息；
     * token 为空或已被标记失效的凭证 status 为 0（未登录），需重新扫码。
     *
     * @param appId 应用 ID，必须存在
     * @return 该应用下绑定的个人微信号列表
     */
    public List<BotCredentialVO> botCredentialList(Long appId) {
        appService.checkExists(appId);
        return botCredentialMapper.selectByAppId(appId).stream()
                .map(BotCredentialVO::of)
                .toList();
    }

    /**
     * 更新微信个人号的扫码人（人工维护的展示名称）。
     *
     * <p>按凭证 ID 覆盖 scanner_name，传空串表示清空；凭证必须存在，否则抛业务异常。
     *
     * @param id      凭证 ID，必须存在
     * @param request 更新请求，扫码人可为空串
     */
    @Transactional
    public void updateBotCredentialScanner(Long id, UpdateBotCredentialScannerDto request) {
        if (botCredentialMapper.selectById(id) == null) {
            throw BizException.notFound("微信个人号不存在：" + id);
        }
        String scannerName = request.getScannerName() == null ? "" : request.getScannerName().trim();
        botCredentialMapper.updateScannerName(id, scannerName);
    }

    /**
     * 分页查询聊天记录。
     *
     * <p>按 appId 过滤，可再按渠道、时间范围（created_at）进一步筛选，最新的在前。
     *
     * <p>库里一个会话可能因为 history 超长被拆成多行（conversation_id 相同、seq 递增），
     * 分页必须落在「会话」上：先取当前页的 conversation_id，再取这些会话的全部分段，
     * 在内存里按 seq 合并成一条会话返回。这样 total、页大小、返回字段与页面约定都不变。
     *
     * @param appId   应用 ID（来自 URL）
     * @param request 分页与筛选条件
     * @return 分页结果
     */
    public PageResult<ChatHistory> chatHistoryPage(Long appId, ChatHistoryPageQueryDto request) {
        Page<ChatHistory> page = request.toPage();
        long total = chatHistoryMapper.countConversationByQuery(appId, request);
        page.setTotal(total);
        if (total == 0) {
            page.setRecords(List.of());
            return PageResults.of(page);
        }

        long offset = (page.getCurrent() - 1) * page.getSize();
        List<String> conversationIds = chatHistoryMapper.selectConversationIdsByQuery(appId, request, offset, page.getSize());
        if (conversationIds.isEmpty()) {
            page.setRecords(List.of());
            return PageResults.of(page);
        }

        List<ChatHistory> records = ChatHistorySegmentMerger.merge(
                chatHistoryMapper.selectSegmentsByConversationIds(appId, conversationIds));
        // 标签只存接入实体 id，页面要展示名称：按渠道批量反查后回填 tagName
        fillTagName(records);
        // 合并会按首次出现顺序输出，这里还原成「当前页会话顺序」（最近的会话在前）
        Map<String, Integer> order = new HashMap<>();
        for (int i = 0; i < conversationIds.size(); i++) {
            order.put(conversationIds.get(i), i);
        }
        records.sort(Comparator.comparingInt(
                (ChatHistory record) -> order.getOrDefault(record.getConversationId(), Integer.MAX_VALUE)));
        page.setRecords(records);
        return PageResults.of(page);
    }

    /* ------------------------------------------------------------ 内部方法 */

    /**
     * 回填对话记录的 tagName：处理免登陆窗口、账号密码登录窗口与微信个人号三个渠道。
     *
     * <ul>
     *     <li>免登陆窗口（channel=2）：tag 为 {@code app_access_window.id}，名称取 {@code name}；</li>
     *     <li>账号密码登录窗口（channel=3）：tag 为 {@code app_password_window.id}，名称取 {@code username}；</li>
     *     <li>微信个人号（channel=4）：tag 为 {@code bot_credential.id}，名称取 {@code scanner_name}。</li>
     * </ul>
     *
     * <p>按渠道把 tag 分组后各查一次库，避免逐条查询；查不到的记录 tagName 保持为空。
     *
     * @param records 合并后的会话列表，直接就地回填
     */
    private void fillTagName(List<ChatHistory> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Set<Long> windowTags = new HashSet<>();
        Set<Long> accountTags = new HashSet<>();
        Set<Long> botTags = new HashSet<>();
        for (ChatHistory record : records) {
            if (record.getTag() == null) {
                continue;
            }
            if (Objects.equals(record.getChannel(), CHANNEL_WINDOW)) {
                windowTags.add(record.getTag());
            } else if (Objects.equals(record.getChannel(), CHANNEL_ACCOUNT)) {
                accountTags.add(record.getTag());
            } else if (Objects.equals(record.getChannel(), CHANNEL_WECHAT_PERSONAL)) {
                botTags.add(record.getTag());
            }
        }
        if (windowTags.isEmpty() && accountTags.isEmpty() && botTags.isEmpty()) {
            return;
        }

        Map<Long, String> windowNames = new HashMap<>();
        if (!windowTags.isEmpty()) {
            for (AppAccessWindow window : appAccessWindowMapper.selectByIds(windowTags)) {
                windowNames.put(window.getId(), window.getName());
            }
        }
        Map<Long, String> accountNames = new HashMap<>();
        if (!accountTags.isEmpty()) {
            for (AppPasswordWindow window : passwordWindowMapper.selectByIds(accountTags)) {
                accountNames.put(window.getId(), window.getUsername());
            }
        }
        Map<Long, String> botNames = new HashMap<>();
        if (!botTags.isEmpty()) {
            for (BotCredential credential : botCredentialMapper.selectByIds(botTags)) {
                botNames.put(credential.getId(), credential.getScannerName());
            }
        }

        for (ChatHistory record : records) {
            if (record.getTag() == null) {
                continue;
            }
            if (Objects.equals(record.getChannel(), CHANNEL_WINDOW)) {
                record.setTagName(windowNames.get(record.getTag()));
            } else if (Objects.equals(record.getChannel(), CHANNEL_ACCOUNT)) {
                record.setTagName(accountNames.get(record.getTag()));
            } else if (Objects.equals(record.getChannel(), CHANNEL_WECHAT_PERSONAL)) {
                record.setTagName(botNames.get(record.getTag()));
            }
        }
    }

    /**
     * 提示词必须同时包含 {question} 与 {answer}，否则下游无法正确包装返回。
     *
     * <p>保存与调试接口共用同一套校验。
     *
     * @param content 提示词内容
     */
    public void checkPlaceholders(String content) {
        List<String> missing = PromptConstants.missingPlaceholders(content);
        if (!missing.isEmpty()) {
            throw BizException.badRequest("提示词必须包含 "
                    + PromptConstants.QUESTION_PLACEHOLDER + " 与 " + PromptConstants.ANSWER_PLACEHOLDER
                    + " 变量，否则无法正确包装返回，当前缺少：" + String.join("、", missing));
        }
    }

    /** 校验窗口名在同一应用内唯一 */
    private void ensureWindowNameUnique(Long appId, String name) {
        Long count = passwordWindowMapper.selectCount(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId)
                .eq(AppPasswordWindow::getName, name));
        if (count != null && count > 0) {
            throw BizException.conflict("窗口名称已存在：" + name);
        }
    }

    /** 校验登录账号在同一应用内唯一 */
    private void ensureWindowUsernameUnique(Long appId, String username) {
        Long count = passwordWindowMapper.selectCount(Wrappers.<AppPasswordWindow>lambdaQuery()
                .eq(AppPasswordWindow::getAppId, appId)
                .eq(AppPasswordWindow::getUsername, username));
        if (count != null && count > 0) {
            throw BizException.conflict("登录账号已存在：" + username);
        }
    }
}
