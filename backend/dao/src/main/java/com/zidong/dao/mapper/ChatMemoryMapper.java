package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.ChatMemoryRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/** 会话记忆 */
public interface ChatMemoryMapper extends BaseMapper<ChatMemoryRecord> {

    /** 按 (应用, 会话) 取记录，查不到返回 null */
    default ChatMemoryRecord findByAppIdAndMemoryId(Long appId, String memoryId) {
        return selectOne(Wrappers.<ChatMemoryRecord>lambdaQuery()
                .eq(ChatMemoryRecord::getAppId, appId)
                .eq(ChatMemoryRecord::getMemoryId, memoryId));
    }

    /**
     * 整段覆盖写入：不存在则插入，存在则更新。
     *
     * <p>记忆每轮都会被整体重写，这里用 {@code INSERT ... ON DUPLICATE KEY UPDATE} 让
     * 「读—改—写」压成一条语句，避免先 select 再 update 在并发下把旧快照写回去。
     * 时间戳交给数据库 {@code NOW()}，省掉一次应用侧取值。</p>
     *
     * <p>判重键是 {@code (app_id, memory_id)}，与查询保持一致：同一应用下的同一会话落到同一行。
     * {@code app_id} 本身已是键的一部分，UPDATE 里无需再维护。</p>
     *
     * @param appId    所属应用 ID
     * @param memoryId 会话记忆 ID
     * @param messages 消息列表 JSON
     * @return 受影响行数（插入 1，更新 2，无变化 0）
     */
    @Insert("""
            INSERT INTO chat_memory (app_id, memory_id, messages, created_at, updated_at)
            VALUES (#{appId}, #{memoryId}, #{messages}, NOW(), NOW())
            ON DUPLICATE KEY UPDATE messages = VALUES(messages), updated_at = NOW()
            """)
    int upsert(@Param("appId") Long appId, @Param("memoryId") String memoryId, @Param("messages") String messages);

    /** 删除某应用下的一条会话记忆：清空单个会话时共用 */
    default int deleteByAppIdAndMemoryId(Long appId, String memoryId) {
        return delete(Wrappers.<ChatMemoryRecord>lambdaQuery()
                .eq(ChatMemoryRecord::getAppId, appId)
                .eq(ChatMemoryRecord::getMemoryId, memoryId));
    }

    /** 清空某应用的全部会话记忆：删除应用时共用 */
    default int deleteByAppId(Long appId) {
        return delete(Wrappers.<ChatMemoryRecord>lambdaQuery()
                .eq(ChatMemoryRecord::getAppId, appId));
    }
}
