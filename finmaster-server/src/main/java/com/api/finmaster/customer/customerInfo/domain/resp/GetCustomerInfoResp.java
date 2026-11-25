package com.qykj.finmaster.customer.customerInfo.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
@Data
public class GetCustomerInfoResp implements Serializable {
    private static final long serialVersionUID = -3365434504913613362L;
    /** 客户ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户名称 */
    private String customerName;


    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 行业 */
    private String industry;

    /** 来源 */
    private String source;

    /** 线索ID */
    private Long leadId;

    /** 线索名称 */
    private String leadName;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    private Integer status;

    /** 下次跟进时间 */
    private Date nextFollowTime;

    /** 最近跟进内容 */
    private String lastFollowContent;

    /** 最近跟进时间 */
    private Date lastFollowTime;

    /** 备注 */
    private String remark;

    private String ownerUserName;
}
