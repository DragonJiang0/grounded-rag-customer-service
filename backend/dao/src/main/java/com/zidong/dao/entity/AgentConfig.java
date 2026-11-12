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

/** Agent 配置 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("agent_config")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AgentConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 助手名称 */
    private String name;

    /** 头像文字 */
    private String avatar;

    /** 欢迎语 */
    private String welcome;

    /** 未命中时的兜底回复 */
    private String fallback;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
