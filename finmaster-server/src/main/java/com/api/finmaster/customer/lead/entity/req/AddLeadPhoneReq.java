package com.qykj.finmaster.customer.lead.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * PC 线索"加手机号"请求
 *  - phone / openId / wechat 至少一项
 *  - 触发后端家族合并
 */
@Data
public class AddLeadPhoneReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -6731043812938184123L;

    /** 目标线索ID（保留方） */
    private Long leadId;

    /** 手机号 */
    private String phone;

    /** openId */
    private String openId;

    /** 微信号 */
    private String wechat;

    /** 备注（合并前销售可填，留痕） */
    private String remark;
}
