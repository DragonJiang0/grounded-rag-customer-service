package com.zidong.dao.vo;

import com.zidong.dao.entity.App;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/** 应用列表项 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class AppVO extends AppDetailVO {

    private static final long serialVersionUID = 1L;

    // TODO 发布渠道：渠道落库后填真实数据，元素类型换成渠道对象即可，字段名保持不变
    /** 发布渠道：本期没有落库，统一返回空列表 */
    private List<String> publishChannels = List.of();

    /** 从应用实体构建列表项 */
    public static AppVO of(App app) {
        AppVO vo = new AppVO();
        copy(app, vo);
        return vo;
    }
}
