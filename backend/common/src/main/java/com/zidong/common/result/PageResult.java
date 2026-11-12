package com.zidong.common.result;

import java.util.List;

/** 分页结果 */
public record PageResult<T>(List<T> records, long total, long current, long size) {

    public static <T> PageResult<T> of(List<T> records, long total, long current, long size) {
        return new PageResult<>(records, total, current, size);
    }

    public static <T> PageResult<T> empty(long current, long size) {
        return new PageResult<>(List.of(), 0L, current, size);
    }
}
