package com.zidong.dao.vo;

import lombok.Getter;
import lombok.Setter;

/** 解绑机器人结果 */
@Getter
@Setter
public class BotResetVO {

    /** 是否解绑成功 */
    private Boolean success;

    /** 被重置回待处理的消息任务条数 */
    private Integer resetTasks;
}
