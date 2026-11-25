package com.qykj.finmaster.customer.lead.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class GetLeadInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 3029373015047222750L;
    /** 线索ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 线索名称 */
    private String leadName;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 微信号 */
    private String wechat;

    /** 电子邮箱 */
    private String email;

    /** 公司名称 */
    private String companyName;

    /** 线索来源（1小程序注册 2引流 3抖音 4小红书 5官网 6客户介绍 small_produce小程序注册） */
    private String source;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    private Integer status;

    /** 负责人用户名 */
    private String ownerUser;

    /** 负责人真实姓名 */
    private String ownerUserName;

    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;

    /** 最近跟进内容 */
    private String lastFollowContent;

    /** 最近跟进时间 */
    private Date lastFollowTime;

    /** 客户类型（1个人 2企业） */
    private Integer customerType;

    /** 创建者 */
    private String createBy;

    /** 创建者昵称 */
    private String createByName;

    /** 创建时间 */
    private Date createTime;

    /** 更新者 */
    private String updateBy;

    /** 更新者昵称 */
    private String updateByName;

    /** 更新时间 */
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 是否删除（0否 1是） */
    private Integer deleted;

    /** 是否有效（0否 1是） */
    private Integer isValid;

    /** open_id */
    private String openId;

    /** 线索类型 字典 */
    private String type;
}