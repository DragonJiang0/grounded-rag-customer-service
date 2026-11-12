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

/** 应用 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("app")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class App implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 应用名称，唯一 */
    private String name;

    /** 应用描述 */
    private String description;

    /** 调用凭证 AppKey，全局唯一 */
    private String appKey;

    /**
     * 是否启用多 Agent：true 启用（按用户意图在多个 Agent 间选择），false 关闭（固定走默认 Agent）。
     *
     * <p>开关按应用维度生效，不同应用互不影响；未配置（null）时按关闭处理。
     */
    private Boolean multiAgentEnabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
