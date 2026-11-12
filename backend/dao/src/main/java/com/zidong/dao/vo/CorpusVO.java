package com.zidong.dao.vo;

import com.zidong.dao.entity.Corpus;
import com.zidong.dao.entity.CorpusQuestion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/** 语料详情 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class CorpusVO extends Corpus {

    private static final long serialVersionUID = 1L;

    /** 该语料下的问题，按主键（录入顺序）升序 */
    private List<CorpusQuestion> questions = List.of();
}
