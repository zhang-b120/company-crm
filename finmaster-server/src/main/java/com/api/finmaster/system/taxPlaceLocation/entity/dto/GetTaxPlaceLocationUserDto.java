package com.qykj.finmaster.system.taxPlaceLocation.entity.dto;

import lombok.Data;

/**
 * @Author zhb
 * @Date 2026-01-22
 * @Description
 */
@Data
public class GetTaxPlaceLocationUserDto {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 财务用户ID
     */
    private Long financialUserId;

    /**
     * crm用户名
     */
    private String userName;

    /**
     * crm姓名
     */
    private String realName;

    /**
     * 省份
     */
    private String province;
}