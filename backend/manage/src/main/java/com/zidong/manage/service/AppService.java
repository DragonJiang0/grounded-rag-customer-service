package com.zidong.manage.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.common.exception.BizException;
import com.zidong.dao.dto.AddAppDto;
import com.zidong.dao.dto.EditAppDto;
import com.zidong.dao.entity.App;
import com.zidong.dao.entity.AppAgent;
import com.zidong.dao.entity.AppChatOptions;
import com.zidong.dao.entity.AppCorpusGroup;
import com.zidong.dao.entity.CorpusGroup;
import com.zidong.dao.vo.AppDetailVO;
import com.zidong.dao.vo.AppVO;
import com.zidong.dao.mapper.AppAccessWindowMapper;
import com.zidong.dao.mapper.AppAgentMapper;
import com.zidong.dao.mapper.AppApiAccessMapper;
import com.zidong.dao.mapper.AppChatOptionsMapper;
import com.zidong.dao.mapper.AppCorpusGroupMapper;
import com.zidong.dao.mapper.AppMapper;
import com.zidong.dao.mapper.ChatHistoryMapper;
import com.zidong.dao.mapper.ChatMemoryMapper;
import com.zidong.dao.mapper.AppPromptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 应用管理 */
@Service
@RequiredArgsConstructor
public class AppService {

    /** AppKey 前缀，形如 ak_8f3c1d2e9b4a */
    private static final String APP_KEY_PREFIX = "ak_";
    /** AppKey 随机部分长度 */
    private static final int APP_KEY_SUFFIX_LENGTH = 12;
    /** AppKey 生成重试次数：撞上唯一键的概率极低，留几次兜底 */
    private static final int APP_KEY_MAX_ATTEMPTS = 5;
    /** 新应用默认最大输出 token 数 */
    private static final int DEFAULT_MAX_OUTPUT_TOKENS = 4096;

    /** AppKey 随机字符集：0-9 与小写字母 */
    private static final char[] ALPHANUM =
            "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AppMapper appMapper;
    private final AppChatOptionsMapper appChatOptionsMapper;
    private final AppApiAccessMapper appApiAccessMapper;
    private final AppAccessWindowMapper appAccessWindowMapper;
    private final AppPromptMapper appPromptMapper;
    private final AppCorpusGroupMapper appCorpusGroupMapper;
    private final AppAgentMapper appAgentMapper;
    private final ChatHistoryMapper chatHistoryMapper;
    private final ChatMemoryMapper chatMemoryMapper;
    private final CorpusGroupService groupService;

    @Transactional
    public void create(AddAppDto request) {
        String name = request.getName().trim();
        ensureNameUnique(name, null);
        // 任一分组不存在直接抛业务异常
        request.getCorpusGroupIds().forEach(groupService::checkExists);

        App app = request.toSave(nextAppKey());
        appMapper.insert(app);

        appCorpusGroupMapper.insert(request.toRelations(app.getId()));

        // Agent 绑定：未选则跳过，避免 MyBatis-Plus 对空集合执行 insert
        List<AppAgent> agentRelations = request.toAgentRelations(app.getId());
        if (!CollectionUtils.isEmpty(agentRelations)) {
            appAgentMapper.insert(agentRelations);
        }

        // 新应用默认落一条对话参数：仅指定最大输出 token，其余留空表示不覆盖、回落到模型默认值
        LocalDateTime now = LocalDateTime.now();
        appChatOptionsMapper.insert(AppChatOptions.builder()
                .appId(app.getId())
                .maxOutputTokens(DEFAULT_MAX_OUTPUT_TOKENS)
                .memoryEnabled(true)
                .memoryRounds(1)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    @Transactional
    public void update(EditAppDto request) {
        App app = checkExists(request.getId());
        // 校验唯一时排除自己，否则只改描述不动名称也会被判成重复
        ensureNameUnique(request.getName().trim(), request.getId());
        // 任一分组不存在直接抛业务异常
        request.getCorpusGroupIds().forEach(groupService::checkExists);

        App updated = request.update(app);
        List<AppAgent> agentRelations = request.toAgentRelations(app.getId());
        // 开关与 Agent 列表的联动在 controller 已校验，这里按最终值再兜一次，
        // 避免留下「开着多 Agent 但没有可选 Agent」的应用
        if (Boolean.TRUE.equals(updated.getMultiAgentEnabled()) && agentRelations.isEmpty()) {
            throw new BizException("启用多 Agent 后请至少选择一个 Agent");
        }

        appMapper.updateById(updated);

        // 绑定关系整组替换最省事：先清空旧的，再按提交顺序重建
        appCorpusGroupMapper.deleteByAppId(app.getId());
        appCorpusGroupMapper.insert(request.toRelations(app.getId()));

        // Agent 绑定同分组：整组替换
        appAgentMapper.deleteByAppId(app.getId());
        if (!CollectionUtils.isEmpty(agentRelations)) {
            appAgentMapper.insert(agentRelations);
        }
    }

    /** 应用详情 */
    public AppDetailVO detail(Long id) {
        App app = checkExists(id);

        // TODO 查询应用：详情这里查了「应用 + 绑定关系 + 分组」三次，后续可合并成一条 SQL 直接返回
        List<Long> groupIds = appCorpusGroupMapper.selectByAppIds(List.of(id)).stream()
                .map(AppCorpusGroup::getGroupId)
                .toList();
        Map<Long, CorpusGroup> groupById = groupService.mapByIds(groupIds);

        AppDetailVO vo = AppDetailVO.of(app);
        vo.setCorpusGroups(groupIds.stream()
                .map(groupById::get)
                .filter(Objects::nonNull)
                .toList());
        // Agent 绑定：供编辑页回显多 Agent 勾选
        vo.setAgents(appAgentMapper.listAgentNamesByAppId(id));
        return vo;
    }

    /** 应用列表 */
    public List<AppVO> listAll() {

        //TODO 一条sql聚合所有直接返回
        List<App> apps = appMapper.selectAllOrdered();
        if (CollectionUtils.isEmpty(apps)) {
            return List.of();
        }

        // 关联关系与分组都一次查完，再在内存里拼，避免按应用逐条查
        List<Long> appIds = apps.stream().map(App::getId).toList();
        Map<Long, List<Long>> groupIdsByApp = appCorpusGroupMapper.selectByAppIds(appIds).stream()
                .collect(Collectors.groupingBy(AppCorpusGroup::getAppId,
                        Collectors.mapping(AppCorpusGroup::getGroupId, Collectors.toList())));

        Map<Long, CorpusGroup> groupById = groupService.mapByIds(
                groupIdsByApp.values().stream().flatMap(List::stream).distinct().toList());

        return apps.stream().map(app -> {
            AppVO vo = AppVO.of(app);
            vo.setCorpusGroups(groupIdsByApp.getOrDefault(app.getId(), List.of()).stream()
                    .map(groupById::get)
                    .filter(Objects::nonNull)
                    .toList());
            return vo;
        }).toList();
    }

    @Transactional
    public void delete(Long id) {
        checkExists(id);
        // 分组绑定跟着应用一起删，避免留下游离关联
        appCorpusGroupMapper.deleteByAppId(id);
        // Agent 绑定同理
        appAgentMapper.deleteByAppId(id);
        // 对话参数配置同理，随应用一并清理
        appChatOptionsMapper.deleteByAppId(id);
        // API 访问接入同理，删应用时一并回收，避免残留可用的 API Key
        appApiAccessMapper.deleteByAppId(id);
        // 免密窗口同理，删应用时一并回收，避免残留可用的免密链接
        appAccessWindowMapper.deleteByAppId(id);
        // 提示词同理，随应用一并清理
        appPromptMapper.deleteByAppId(id);
        // 聊天记录同理，随应用一并清理，避免留下无归属的历史会话
        chatHistoryMapper.deleteByAppId(id);
        // 会话记忆同理，按 app_id 清掉该应用下所有会话的历史，避免残留占用
        chatMemoryMapper.deleteByAppId(id);
        appMapper.deleteById(id);
    }

    /** 校验应用存在 */
    public App checkExists(Long id) {
        App app = appMapper.selectById(id);
        if (app == null) {
            throw BizException.notFound("应用不存在：" + id);
        }
        return app;
    }

    /* ------------------------------------------------------------ 内部方法 */

    /** 校验应用名称唯一 */
    private void ensureNameUnique(String name, Long excludeId) {
        Long count = appMapper.selectCount(Wrappers.<App>lambdaQuery()
                .eq(App::getName, name)
                .ne(excludeId != null, App::getId, excludeId));
        if (count != null && count > 0) {
            throw BizException.conflict("应用名称已存在：" + name);
        }
    }

    /** 生成全局唯一的 AppKey */
    private String nextAppKey() {
        for (int attempt = 0; attempt < APP_KEY_MAX_ATTEMPTS; attempt++) {
            StringBuilder appKey = new StringBuilder(APP_KEY_PREFIX);
            for (int i = 0; i < APP_KEY_SUFFIX_LENGTH; i++) {
                appKey.append(ALPHANUM[RANDOM.nextInt(ALPHANUM.length)]);
            }
            String candidate = appKey.toString();
            Long count = appMapper.selectCount(Wrappers.<App>lambdaQuery()
                    .eq(App::getAppKey, candidate));
            if (count == null || count == 0) {
                return candidate;
            }
        }
        throw BizException.conflict("AppKey 生成失败，请重试");
    }
}
