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

/** 语料 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("corpus")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Corpus implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 语料名称 */
    private String name;

    /** 命中后返回的答案 */
    private String answer;

    /** 所属语料分组，可为空（未分组） */
    private Long groupId;

    /** true 启用 false 停用，停用后不参与匹配 */
    private Boolean enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
