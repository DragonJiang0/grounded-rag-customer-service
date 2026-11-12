package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zidong.dao.entity.BotLeader;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Bot 主节点租约：全部操作都是针对单行（id=1）的原子语句，
 * 时间一律用数据库 {@code NOW()}，避免各实例时钟不一致导致选主错乱。
 */
public interface BotLeaderMapper extends BaseMapper<BotLeader> {

    /**
     * 初始化租约行：多实例并发启动也只会有一行生效。
     * <p>到期时间必须给「过去」，否则插入当秒内 {@code expire_time < NOW()} 不成立，首个实例会白等一轮。</p>
     */
    @Insert("INSERT IGNORE INTO bot_leader (id, instance_id, expire_time, updated_at) VALUES (1, '', '1970-01-01 00:00:00', NOW())")
    int initRow();

    /**
     * 抢占租约：只有「本来就是自己持有」或「租约已过期」才更新成功。
     *
     * @param instanceId   本实例 ID
     * @param leaseSeconds 租约时长（秒）
     * @return 1 表示当选，0 表示已有其它实例持有
     */
    @Update("""
            UPDATE bot_leader
               SET instance_id = #{instanceId},
                   expire_time = DATE_ADD(NOW(), INTERVAL #{leaseSeconds} SECOND),
                   updated_at  = NOW()
             WHERE id = 1
               AND (instance_id = #{instanceId} OR expire_time <= NOW())
            """)
    int acquire(@Param("instanceId") String instanceId, @Param("leaseSeconds") long leaseSeconds);

    /**
     * 续租：leader 心跳，只有仍是自己持有时才生效。
     *
     * @return 1 表示续租成功，0 表示租约已被其它实例抢走
     */
    @Update("""
            UPDATE bot_leader
               SET expire_time = DATE_ADD(NOW(), INTERVAL #{leaseSeconds} SECOND),
                   updated_at  = NOW()
             WHERE id = 1 AND instance_id = #{instanceId}
            """)
    int renew(@Param("instanceId") String instanceId, @Param("leaseSeconds") long leaseSeconds);

    /**
     * 确认租约是否仍在自己手里。
     *
     * <p>续租的 {@code UPDATE} 在「新旧值完全相同」时，部分驱动/连接参数下受影响行数会返回 0
     * （例如 JDBC 设置了 {@code useAffectedRows=true}），不能只凭它判断失去 leader。
     * 续租返回 0 时用本方法再确认一次，避免把健康的主节点误降级。</p>
     */
    @Select("SELECT COUNT(*) FROM bot_leader WHERE id = 1 AND instance_id = #{instanceId} AND expire_time > NOW()")
    int countOwned(@Param("instanceId") String instanceId);

    /** 主动让位：把到期时间置为当前，其它实例可立即接管 */
    @Update("UPDATE bot_leader SET expire_time = NOW(), updated_at = NOW() WHERE id = 1 AND instance_id = #{instanceId}")
    int release(@Param("instanceId") String instanceId);

    /** 取当前租约持有者，表未初始化时返回 null */
    @Select("SELECT * FROM bot_leader WHERE id = 1")
    BotLeader selectLeader();

    /** 保存长轮询游标：随 leader 交接，重启后不重拉历史消息 */
    @Update("UPDATE bot_leader SET updates_buf = #{updatesBuf}, updated_at = NOW() WHERE id = 1 AND instance_id = #{instanceId}")
    int saveUpdatesBuf(@Param("instanceId") String instanceId, @Param("updatesBuf") String updatesBuf);
}
