package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zidong.dao.entity.BotMessageTask;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Bot 入站消息：幂等入库 + 跨实例抢占的状态机。
 *
 * <p>抢占分两步（先查候选再条件更新）而不是一条 {@code UPDATE ... LIMIT 1}，
 * 是为了拿到被抢占行的完整内容去跑业务；并发安全由第二步的 {@code status} 条件保证。</p>
 */
public interface BotMessageTaskMapper extends BaseMapper<BotMessageTask> {

    /** 取一条待处理（或失败可重试）的消息，按 id 升序保证先进先出 */
    @Select("""
            SELECT * FROM bot_message
             WHERE status IN (0, 3) AND retry_count < #{maxRetry}
             ORDER BY id ASC
             LIMIT 1
            """)
    BotMessageTask findOnePending(@Param("maxRetry") int maxRetry);

    /**
     * 抢占：把状态改成处理中。
     *
     * @param expectedStatus 期望的当前状态（0 或 3），并发下只有先到者能改成功
     * @return 1 表示抢占成功
     */
    @Update("""
            UPDATE bot_message
               SET status = 1, owner = #{owner}, updated_at = NOW()
             WHERE id = #{id} AND status = #{expectedStatus}
            """)
    int claim(@Param("id") Long id, @Param("expectedStatus") int expectedStatus, @Param("owner") String owner);

    /** 处理成功 */
    @Update("UPDATE bot_message SET status = 2, updated_at = NOW() WHERE id = #{id}")
    int markDone(@Param("id") Long id);

    /** 处理失败：次数 +1，未达上限时状态回落到 3（待重试） */
    @Update("""
            UPDATE bot_message
               SET status = 3, retry_count = retry_count + 1, error_msg = #{error}, owner = #{owner}, updated_at = NOW()
             WHERE id = #{id}
            """)
    int markFailed(@Param("id") Long id, @Param("error") String error, @Param("owner") String owner);

    /**
     * 重置卡死的任务：处理中但长时间没更新（持有者已崩溃/被摘除）。
     *
     * @param stuckSeconds 超过该秒数未更新即视为卡死
     */
    @Update("""
            UPDATE bot_message
               SET status = 0, owner = NULL, updated_at = NOW()
             WHERE status = 1
               AND updated_at < DATE_SUB(NOW(), INTERVAL #{stuckSeconds} SECOND)
            """)
    int resetStuck(@Param("stuckSeconds") long stuckSeconds);

    /** 解绑时重置全部处理中任务：回到待处理，等重新绑定后由监听实例消费 */
    @Update("""
            UPDATE bot_message
               SET status = 0, owner = NULL, updated_at = NOW()
             WHERE status = 1
            """)
    int resetRunning();

    /**
     * 清理已完成的历史消息：删除完成时间（{@code updated_at}）超过保留天数的记录。
     *
     * <p>只按条件删除、不按 id 逐条删，重复执行只会删到 0 行，天然幂等，
     * 多实例同时调度也不会相互影响。</p>
     *
     * @param retentionDays 保留天数
     * @return 实际删除行数
     */
    @Delete("""
            DELETE FROM bot_message
             WHERE status = 2
               AND updated_at < DATE_SUB(NOW(), INTERVAL #{retentionDays} DAY)
            """)
    int deleteDoneBefore(@Param("retentionDays") int retentionDays);
}
