package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** 语料问题 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("corpus_question")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CorpusQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属语料 */
    private Long corpusId;

    /** 冗余的所属语料分组，便于直接按分组过滤 */
    private Long groupId;

    /** 问题文本 */
    private String question;
}
