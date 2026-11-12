package com.zidong.manage.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zidong.common.result.PageResult;

import java.util.List;
import java.util.function.Function;

/** 分页对象转换工具 */
public final class PageResults {

    private PageResults() {
    }

    public static <E, V> PageResult<V> of(IPage<E> page, Function<E, V> mapper) {
        List<V> records = page.getRecords().stream().map(mapper).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 直接返回实体 */
    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }
}
