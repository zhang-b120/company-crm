package com.qykj.finmaster.customer.customerInfo.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
@Data
public class GetCustomerInfoPageListResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 4161552487914874672L;
    /** 客户ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户名称 */
    @Excel(title = "客户名称", field = "customerName")
    private String customerName;

    /** 客户类型（1个人 2企业） */
    @Excel(title = "客户类型", field = "customerType", readConverterExp = "1=私海,2=公海")
    private Integer customerType;

    /** 联系人 */
    @Excel(title = "联系人", field = "contactName")
    private String contactName;

    /** 联系电话 */
    @Excel(title = "联系电话", field = "phone")
    private String phone;

    /** 电子邮箱 */
    @Excel(title = "电子邮箱", field = "email")
    private String email;

    /** 行业 */
    @Excel(title = "行业", field = "industry")
    private String industry;

    /** 来源 */
    @Excel(title = "来源", field = "source", readConverterExp = "1=小程序注册,2=引流,3=抖音,4=小红书,5=官网,6=客户介绍,small_produce=小程序注册")
    private String source;

    /** 线索ID */
    @Excel(title = "线索ID", field = "leadId")
    private Long leadId;

    /** 线索名称 */
    @Excel(title = "线索名称", field = "leadName")
    private String leadName;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    @Excel(title = "状态", field = "status", readConverterExp = "1=未跟进,2=跟进中,3=已转化,4=已失效")
    private Integer status;

    /** 下次跟进时间 */
    @Excel(title = "下次跟进时间", field = "nextFollowTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date nextFollowTime;

    /** 最近跟进内容 */
    @Excel(title = "最近跟进内容", field = "lastFollowContent")
    private String lastFollowContent;

    /** 最近跟进时间 */
    @Excel(title = "最近跟进时间", field = "lastFollowTime", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastFollowTime;

    /** 备注 */
    @Excel(title = "备注", field = "remark")
    private String remark;

    /** open_id */
    private String openId;

    private String ownerUser;

    private String ownerUserName;

    private String createBy;

    private String createByName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 是否成交
     */
    private Boolean isTransaction;

    /**
     * 距上次沟通间隔天数
     */
    private Long lastFollowInterval;

    /**
     * 沟通次数
     */
    private Integer followCount;
}
