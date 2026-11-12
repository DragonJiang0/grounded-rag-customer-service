package com.zidong.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zidong.dao.entity.DemoLogistics;

import java.util.List;

/** 物流跟踪演示 */
public interface DemoLogisticsMapper extends BaseMapper<DemoLogistics> {

    /**
     * 按物流单号查询，查不到返回 null。
     *
     * @param trackingNo 物流单号，业务唯一
     */
    default DemoLogistics findByTrackingNo(String trackingNo) {
        return selectOne(Wrappers.<DemoLogistics>lambdaQuery()
                .eq(DemoLogistics::getTrackingNo, trackingNo));
    }

    /**
     * 按订单号查询物流记录，按主键升序。
     *
     * @param orderNo 关联的订单号
     */
    default List<DemoLogistics> selectByOrderNo(String orderNo) {
        return selectList(Wrappers.<DemoLogistics>lambdaQuery()
                .eq(DemoLogistics::getOrderNo, orderNo)
                .orderByAsc(DemoLogistics::getId));
    }
}
