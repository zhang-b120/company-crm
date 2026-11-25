package com.qykj.finmaster.system.taxPlaceLocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * @Author zhb
 * @Date 2026-01-22
 * @Description 税局所在地-用户
 */
@Data
public class TaxPlaceLocationUser extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 税局所在地ID
     */
    @TableField(value = "tax_place_location_id")
    private Long taxPlaceLocationId;

    /**
     * 用户ID
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 财务用户ID
     */
    @TableField(value = "financial_user_id")
    private Long financialUserId;
}