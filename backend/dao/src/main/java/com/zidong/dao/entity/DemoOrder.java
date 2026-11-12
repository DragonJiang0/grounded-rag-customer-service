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
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单查询演示用的订单数据 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("demo_order")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DemoOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 订单号，业务唯一 */
    private String orderNo;

    /** 下单人 */
    private String userName;

    /** 订单状态：待支付 / 已支付 / 备货中 / 已发货 / 已完成 */
    private String status;

    /** 订单金额 */
    private BigDecimal totalAmount;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
