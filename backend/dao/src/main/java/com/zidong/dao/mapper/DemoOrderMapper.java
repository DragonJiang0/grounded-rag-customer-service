package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.DemoOrder;

/** 订单查询演示 */
public interface DemoOrderMapper extends BaseMapper<DemoOrder> {

    /**
     * 按订单号查询订单，查不到返回 null。
     *
     * @param orderNo 订单号，业务唯一
     */
    default DemoOrder findByOrderNo(String orderNo) {
        return selectOne(Wrappers.<DemoOrder>lambdaQuery()
                .eq(DemoOrder::getOrderNo, orderNo));
    }
}
