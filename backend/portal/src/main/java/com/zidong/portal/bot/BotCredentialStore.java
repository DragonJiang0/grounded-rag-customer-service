package com.zidong.portal.bot;

import com.zidong.dao.entity.BotCredential;
import com.zidong.dao.mapper.BotCredentialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 凭证共享存储（多行）：每个扫码用户一行，落在 {@code bot_credential} 表。
 *
 * <p>多实例下的约定：leader 扫码成功后写入，其它实例（含故障接管后的新 leader）只读恢复，
 * 全集群共用同一份 token，不会互相踢下线；游标也按行维护，随 leader 一起交接。</p>
 *
 * <p>取代了旧版 {@code CredentialsStore} 单凭证接口（db/file 双实现）：
 * 多 bot 模型下凭证必须集中存 DB 才能多行共享，本地文件不再支持。</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BotCredentialStore {

    private final BotCredentialMapper botCredentialMapper;

    /** 全部有效凭证（token 非空且未被标记失效），监听线程按行启动长轮询 */
    public List<BotCredential> loadValid() {
        try {
            return botCredentialMapper.selectValidList();
        } catch (Exception e) {
            log.warn("读取有效 Bot 凭证列表失败", e);
            return List.of();
        }
    }

    /** 是否至少存在一个有效凭证 */
    public boolean hasValid() {
        return !loadValid().isEmpty();
    }

    /** 按机器人账号 ID 查凭证：入站消息 to_user_id 即 ilink_bot_id */
    public BotCredential findByBotId(String ilinkBotId) {
        if (!StringUtils.hasText(ilinkBotId)) {
            return null;
        }
        try {
            return botCredentialMapper.selectByBotId(ilinkBotId);
        } catch (Exception e) {
            log.warn("按 ilink_bot_id 读取 Bot 凭证失败: {}", ilinkBotId, e);
            return null;
        }
    }

    /**
     * 扫码确认后的凭证落库：按 {@code ilink_bot_id} 幂等——同一用户重复扫码更新已有行（token 轮换、
     * 游标重置），不同用户各自成行，互不影响。
     *
     * @return 该凭证行的 id
     */
    public Long upsertConfirmed(BotCredential candidate) {
        LocalDateTime now = LocalDateTime.now();
        candidate.setInvalid(Boolean.FALSE);
        candidate.setUpdatedAt(now);

        BotCredential existing = StringUtils.hasText(candidate.getIlinkBotId())
                ? botCredentialMapper.selectByBotId(candidate.getIlinkBotId()) : null;
        if (existing == null) {
            botCredentialMapper.insert(candidate);
            return candidate.getId();
        }
        existing.setBotToken(candidate.getBotToken());
        existing.setBaseurl(candidate.getBaseurl());
        existing.setIlinkUserId(candidate.getIlinkUserId());
        if (candidate.getAppId() != null) {
            existing.setAppId(candidate.getAppId());
        }
        // 换上新 token 即视为有效：新扫码会建立新会话，游标从零开始
        existing.setInvalid(Boolean.FALSE);
        existing.setUpdatesBuf(null);
        existing.setUpdatedAt(now);
        botCredentialMapper.updateById(existing);
        return existing.getId();
    }

    /** 标记凭证无效：失败只记日志，不影响调用方的清理动作 */
    public void markInvalid(Long id) {
        try {
            botCredentialMapper.markInvalid(id);
        } catch (Exception e) {
            log.warn("标记 bot_credential 无效失败: id={}", id, e);
        }
    }

    /** 只清 token 不删行：保留 app_id 等绑定信息 */
    public void clearToken(Long id) {
        try {
            botCredentialMapper.clearToken(id);
            log.warn("已清除 Bot 登录凭证（保留绑定信息）: id={}", id);
        } catch (Exception e) {
            log.warn("清除 Bot 登录凭证失败: id={}", id, e);
        }
    }

    /** 保存本会话的 get_updates 游标：仅 leader 写入，随行交接给下一个 leader */
    public void saveUpdatesBuf(Long id, String updatesBuf) {
        if (updatesBuf == null) {
            return;
        }
        try {
            botCredentialMapper.saveUpdatesBuf(id, updatesBuf);
        } catch (Exception e) {
            log.warn("保存长轮询游标失败（不影响本次拉取）: id={}", id, e);
        }
    }
}
