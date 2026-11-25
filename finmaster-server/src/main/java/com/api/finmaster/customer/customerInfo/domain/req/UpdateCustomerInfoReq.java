package com.qykj.finmaster.customer.customerInfo.domain.req;

import cn.hutool.core.date.DateTime;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
@Data
public class UpdateCustomerInfoReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 6145770848622570603L;

    private Long id;
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
    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    private Integer status;

    /** 下次跟进时间 */
    private DateTime nextFollowTime;
    /** 备注 */
    private String remark;
}
