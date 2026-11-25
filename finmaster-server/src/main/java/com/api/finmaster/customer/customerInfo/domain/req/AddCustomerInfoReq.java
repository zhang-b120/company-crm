package com.qykj.finmaster.customer.customerInfo.domain.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class AddCustomerInfoReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -6144383540372787371L;

    /** 客户名称 */
    private String customerName;

    /** 客户类型（1个人 2企业） */
    private Integer customerType;

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

    private String ownerUser;

    private String ownerUserName;
}
