package com.zidong.dao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 聊天记录分页查询条件：渠道、开始 / 结束时间均可为空（为空表示不限） */
@Getter
@Setter
public class ChatHistoryPageQueryDto extends PageDto {

    /** 来源渠道：0.调试 1.API访问 2.免登陆窗口 …，为空表示不限 */
    private Integer channel;

    /** 开始时间（含），匹配 created_at，为空表示不限 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /** 结束时间（含），匹配 created_at，为空表示不限 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;
}
