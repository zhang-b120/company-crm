package com.qykj.finmaster.customer.lead.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class GetLeadPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = 992824959528104152L;

    /** 线索名称 */
    private String leadName;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 微信号 */
    private String wechat;

    /** 公司名称 */
    private String companyName;

    /** 线索来源（1小程序注册 2引流 3抖音 4小红书 5官网 6客户介绍 small_produce小程序注册） */
    private String source;

    /** 线索类型（字典） */
    private String type;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    private Integer status;

    /** 负责人真实姓名 */
    private String ownerUserName;

    /** 最近跟进内容 */
    private String lastFollowContent;

    /** 创建者昵称 */
    private String createByName;

    /** 是否有效（0否 1是） */
    private Integer isValid;

    /**
     * 是否成交
     */
    private Boolean isTransaction;

    /**
     * 沟通次数
     */
    private Integer followCount;


    private String beginLastFollowTime;
    private String endLastFollowTime;

    private String beginCreateTime;
    private String endCreateTime;

    /**
     * 待分配筛选：1=只看 ownerUser 为空的线索
     */
    private Integer unassigned;

    /**
     * 待回收筛选：1=只看接近自动回收阈值的线索（基于 sys_config 配置的 noFollowDays 阈值）
     * 取阈值的 70% 作为预警起点
     */
    private Integer recyclable;

    /**
     * 再分配筛选：1=只看 release_count > 0 且当前已重新分配负责人的线索
     */
    private Integer reassigned;

}