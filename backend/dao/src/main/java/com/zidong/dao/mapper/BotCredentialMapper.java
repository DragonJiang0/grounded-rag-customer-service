package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zidong.dao.entity.BotCredential;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 微信 Bot 登录凭证（多行：每个扫码用户一行，同一 app 可被多个用户绑定）。
 */
public interface BotCredentialMapper extends BaseMapper<BotCredential> {

    /**
     * 加载全部有效凭证：token 非空且未被标记失效。
     * 监听线程据此为每个会话启动独立的长轮询。
     */
    @Select("""
            SELECT * FROM bot_credential
             WHERE bot_token IS NOT NULL AND bot_token != ''
               AND invalid = 0
             ORDER BY id
            """)
    List<BotCredential> selectValidList();

    /**
     * 按应用 ID 查凭证：管理台渲染「个人微信号」列表用，最新绑定的在前。
     */
    @Select("SELECT * FROM bot_credential WHERE app_id = #{appId} ORDER BY id DESC")
    List<BotCredential> selectByAppId(@Param("appId") Long appId);

    /**
     * 按机器人账号 ID 查凭证：入站消息的 to_user_id 即 ilink_bot_id，据此路由 app_id 与回复客户端。
     */
    @Select("SELECT * FROM bot_credential WHERE ilink_bot_id = #{ilinkBotId} LIMIT 1")
    BotCredential selectByBotId(@Param("ilinkBotId") String ilinkBotId);

    /**
     * 标记凭证无效：getupdates 持续异常（如 ret=null）时由监听线程调用，
     * 之后启动/恢复会跳过该 token，需重新扫码。
     *
     * @return 1 表示标记成功，0 表示凭证行不存在
     */
    @Update("""
            UPDATE bot_credential
               SET invalid = 1,
                   updated_at = NOW()
             WHERE id = #{id}
            """)
    int markInvalid(@Param("id") Long id);

    /**
     * 只清登录态、不删行：bot_token 置空串（视为未登录），
     * 保留 app_id 等绑定信息，避免「清凭证 → 重新扫码」后绑定丢失。
     *
     * @return 1 表示清除成功，0 表示凭证行不存在
     */
    @Update("""
            UPDATE bot_credential
               SET bot_token = '',
                   baseurl = NULL,
                   invalid = 0,
                   updated_at = NOW()
             WHERE id = #{id}
            """)
    int clearToken(@Param("id") Long id);

    /**
     * 更新扫码人（人工维护的展示名称），传空串表示清空。
     *
     * @return 1 表示更新成功，0 表示凭证行不存在
     */
    @Update("""
            UPDATE bot_credential
               SET scanner_name = #{scannerName},
                   updated_at = NOW()
             WHERE id = #{id}
            """)
    int updateScannerName(@Param("id") Long id, @Param("scannerName") String scannerName);

    /**
     * 保存本会话的 get_updates 游标（仅 leader 写入），随行交接给下一个 leader。
     *
     * @return 1 表示更新成功，0 表示凭证行不存在
     */
    @Update("""
            UPDATE bot_credential
               SET updates_buf = #{updatesBuf},
                   updated_at = NOW()
             WHERE id = #{id}
            """)
    int saveUpdatesBuf(@Param("id") Long id, @Param("updatesBuf") String updatesBuf);
}
