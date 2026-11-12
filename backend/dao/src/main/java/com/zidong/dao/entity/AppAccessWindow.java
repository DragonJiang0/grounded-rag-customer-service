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

/** 应用免密窗口（免登录直达对话的链接凭证） */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("app_access_window")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppAccessWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属应用 ID */
    private Long appId;

    /** 免密窗口名称，同一应用内唯一，供管理端识别 */
    private String name;

    /** 免密窗口唯一标识，全局唯一，后端生成，用于拼接免密打开链接 */
    private String windowKey;


    /** 最后一次使用时间，未使用过为 null */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastUsedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
