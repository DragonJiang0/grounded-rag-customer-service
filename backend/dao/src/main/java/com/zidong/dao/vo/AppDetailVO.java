package com.zidong.dao.vo;

import com.zidong.dao.entity.App;
import com.zidong.dao.entity.CorpusGroup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/** 应用详情 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AppDetailVO extends App {

    private static final long serialVersionUID = 1L;

    /** 该应用绑定的语料分组，按绑定顺序 */
    private List<CorpusGroup> corpusGroups = List.of();

    /** 该应用绑定的 Agent 名称，按绑定顺序；multiAgentEnabled 为 false 时可能为空 */
    private List<String> agents = List.of();

    /** 从应用实体构建详情 */
    public static AppDetailVO of(App app) {
        AppDetailVO vo = new AppDetailVO();
        copy(app, vo);
        return vo;
    }

    /** 复制基本信息到目标对象 */
    protected static void copy(App source, App target) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setAppKey(source.getAppKey());
        target.setMultiAgentEnabled(source.getMultiAgentEnabled());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
    }
}
