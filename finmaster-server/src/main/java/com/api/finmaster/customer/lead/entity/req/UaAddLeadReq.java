package com.qykj.finmaster.customer.lead.entity.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-29
 * @Description
 */
@Data
public class UaAddLeadReq implements Serializable {

    private static final long serialVersionUID = -1509794463164141824L;

    /**
     * 联系人名称
     */
    private String contactName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 微信号
     */
    private String wechat;

    /**
     * 微信openId
     */
    private String openId;
}