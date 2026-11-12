package com.zidong.dao.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;
import lombok.Setter;

/** 分页请求基类 */
@Getter
@Setter
public class PageDto {

    /** 页码，从 1 开始 */
    private long page = 1L;

    /** 每页条数，单页上限由分页插件兜底 */
    private long size = 10L;

    /** 转成 MyBatis-Plus 的分页对象 */
    public <T> Page<T> toPage() {
        return new Page<>(page < 1 ? 1L : page, size < 1 ? 10L : size);
    }
}
