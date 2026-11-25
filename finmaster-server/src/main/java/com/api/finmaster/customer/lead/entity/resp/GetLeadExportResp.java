package com.qykj.finmaster.customer.lead.entity.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class GetLeadExportResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -2488688464946884050L;

    /** 线索名称 */
    @Excel(title = "线索名称", field = "leadName")
    private String leadName;

    /** 联系人 */
    @Excel(title = "联系人", field = "contactName")
    private String contactName;

    /** 联系电话 */
    @Excel(title = "联系电话", field = "phone")
    private String phone;

    /** 微信号 */
    @Excel(title = "微信号", field = "wechat")
    private String wechat;

    /** 电子邮箱 */
    @Excel(title = "电子邮箱", field = "email")
    private String email;

    /** 公司名称 */
    @Excel(title = "公司名称", field = "companyName")
    private String companyName;

    /** 线索来源（1小程序注册 2引流 3抖音 4小红书 5官网 6客户介绍 small_produce小程序注册） */
    @Excel(title = "线索来源", field = "source", readConverterExp = "1=小程序注册,2=引流,3=抖音,4=小红书,5=官网,6=客户介绍,small_produce=小程序注册")
    private String source;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    @Excel(title = "状态", field = "status", readConverterExp = "1=未跟进,2=跟进中,3=已转化,4=已失效")
    private Integer status;

    /** 负责人用户名 */
    @Excel(title = "负责人用户名", field = "ownerUser")
    private String ownerUser;

    /** 负责人真实姓名 */
    @Excel(title = "负责人姓名", field = "ownerUserName")
    private String ownerUserName;
    /** 下次跟进时间 */
    @Excel(title = "下次跟进时间", field = "nextFollowTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowTime;

    /** 最近跟进内容 */
    @Excel(title = "最近跟进内容", field = "lastFollowContent")
    private String lastFollowContent;

    /** 最近跟进时间 */
    @Excel(title = "最近跟进时间", field = "lastFollowTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date lastFollowTime;

    /** 客户类型（1个人 2企业） */
    @Excel(title = "客户类型", field = "customerType", readConverterExp = "1=个人,2=企业")
    private Integer customerType;

    /** 创建者昵称 */
    @Excel(title = "创建人", field = "createByName")
    private String createByName;

    /** 创建时间 */
    @Excel(title = "创建时间", field = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 是否删除（0否 1是） */
    @Excel(title = "删除状态", field = "deleted", readConverterExp = "0=正常,1=已删除")
    private Integer deleted;

    /** 是否有效（0否 1是） */
    @Excel(title = "是否有效", field = "isValid", readConverterExp = "0=无效,1=有效")
    private Integer isValid;

    /** 线索类型 字典 */
    @Excel(title = "线索类型", field = "type", readConverterExp = "keep_funds=留资,cancel_company=取消公司")
    private String type;

    /** 推荐码 */
    @Excel(title = "推荐码", field = "recommendCode")
    private Long recommendCode;
}