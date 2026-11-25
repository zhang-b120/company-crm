package com.qykj.finmaster.oa.trip.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 出差行程段 oa_business_trip_leg
 */
@Data
@TableName("oa_business_trip_leg")
public class OaBusinessTripLeg implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出差申请ID */
    @TableField("trip_id")
    private Long tripId;

    /** 行程顺序 */
    @TableField("leg_order")
    private Integer legOrder;

    /** 出发地 */
    @TableField("departure")
    private String departure;

    /** 到达地 */
    @TableField("arrival")
    private String arrival;

    /** 出发时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("departure_time")
    private Date departureTime;

    /** 到达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("arrival_time")
    private Date arrivalTime;

    /** 交通方式 */
    @TableField("transportation")
    private String transportation;

    /** 预估费用 */
    @TableField("estimated_cost")
    private BigDecimal estimatedCost;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
