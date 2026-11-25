package com.qykj.finmaster.customer.lead.entity.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-05-29
 * @Description
 */
@Data
public class AddLeadReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -8353931335456311622L;

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

    /** 下次跟进时间 */
    private LocalDateTime nextFollowTime;

    /** 备注 */
    private String remark;

    /** 线索类型 字典 */
    private String type;
}