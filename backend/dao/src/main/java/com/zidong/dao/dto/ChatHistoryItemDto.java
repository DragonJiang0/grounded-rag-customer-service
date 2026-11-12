package com.zidong.dao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 聊天记录中的单条消息，用于 ChatHistory.history 字段（JSON 数组）的序列化与反序列化 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryItemDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色：user / assistant / system 等 */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息时间，纯文本格式 yyyy-MM-dd HH:mm:ss */
    private String createTime;
}
