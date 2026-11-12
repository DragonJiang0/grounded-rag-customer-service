package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话记忆（对应 langchain4j 的 {@code ChatMemoryStore}）。
 *
 * <p>一个 {@code (appId, memoryId)} 一行，行内 {@code messages} 存整个消息列表的 JSON 快照——
 * langchain4j 的 {@code ChatMemoryStore} 语义就是「整段读、整段写」，
 * 用一行整存比把每条消息拆行更简单，也不会出现半截会话。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("chat_memory")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMemoryRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 会话记忆 ID（taskId），与应用一起唯一确定一行 */
    private String memoryId;

    /** 所属应用 ID；读写按 (appId, memoryId) 定位一行 */
    private Long appId;

    /** 消息列表 JSON（langchain4j ChatMessageSerializer 序列化结果） */
    private String messages;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
