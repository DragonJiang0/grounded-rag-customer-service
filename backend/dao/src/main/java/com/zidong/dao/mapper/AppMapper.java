package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.App;

import java.util.List;

/** 应用 */
public interface AppMapper extends BaseMapper<App> {

    /** 返回全部应用，按主键升序 */
    default List<App> selectAllOrdered() {
        return selectList(Wrappers.<App>lambdaQuery()
                .orderByAsc(App::getId));
    }

    /** 按调用凭证 AppKey 查询应用（AppKey 全局唯一），查不到返回 null */
    default App findByAppKey(String appKey) {
        return selectOne(Wrappers.<App>lambdaQuery()
                .eq(App::getAppKey, appKey));
    }
}
