package com.zidong.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 物流跟踪演示用的物流数据 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("demo_logistics")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemoLogistics implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 物流单号，业务唯一 */
    private String trackingNo;

    /** 关联的订单号 */
    private String orderNo;

    /** 承运商 */
    private String carrier;

    /** 物流状态：已揽收 / 运输中 / 派送中 / 已签收 */
    private String status;

    /** 最新位置 */
    private String location;

    /** 预计送达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expectedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
