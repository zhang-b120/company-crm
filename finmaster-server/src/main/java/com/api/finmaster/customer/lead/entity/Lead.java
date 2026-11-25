package com.qykj.finmaster.customer.lead.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 线索管理对象 sale_lead
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sale_lead")
public class Lead extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -7608652933739227731L;
    /** 线索ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 线索名称 */
    @TableField("lead_name")
    private String leadName;

    /** 联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话 */
    @TableField("phone")
    private String phone;

    /** 微信号 */
    @TableField("wechat")
    private String wechat;

    /** 电子邮箱 */
    @TableField("email")
    private String email;

    /** 公司名称 */
    @TableField("company_name")
    private String companyName;

    /** 线索来源（1小程序注册 2引流 3抖音 4小红书 5官网 6客户介绍 small_produce小程序注册） */
    @TableField("source")
    private String source;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    @TableField("status")
    private Integer status;

    /** 负责人用户名 */
    @TableField("owner_user")
    private String ownerUser;

    /** 负责人真实姓名 */
    @TableField("owner_user_name")
    private String ownerUserName;
    /** 下次跟进时间 */
    @TableField("next_follow_time")
    private LocalDateTime nextFollowTime;

    /** 最近跟进内容 */
    @TableField("last_follow_content")
    private String lastFollowContent;

    /** 最近跟进时间 */
    @TableField("last_follow_time")
    private Date lastFollowTime;

    /** 客户类型（1个人 2企业） */
    @TableField("customer_type")
    private Integer customerType;

    /** 创建者 */
    @TableField("create_by")
    private String createBy;

    /** 创建者昵称 */
    @TableField("create_by_name")
    private String createByName;

    /** 创建时间 */
    @TableField("create_time")
    private Date createTime;

    /** 更新者 */
    @TableField("update_by")
    private String updateBy;

    /** 更新者昵称 */
    @TableField("update_by_name")
    private String updateByName;

    /** 更新时间 */
    @TableField("update_time")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 是否删除（0否 1是） */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /** 是否有效（0否 1是） */
    @TableField("is_valid")
    private Integer isValid;

    /** 被自动释放的次数（每次定时回收 +1） */
    @TableField("release_count")
    private Integer releaseCount;

    /** 最近一次被自动释放的时间（与 lastFollowTime 区分：本字段只由系统回收任务写） */
    @TableField("last_release_time")
    private Date lastReleaseTime;

    /** open_id */
    @TableField("open_id")
    private String openId;

    /** 线索类型 字典 */
    @TableField("type")
    private String type;

    /** 推荐码 */
    @TableField("recommend_code")
    private Long recommendCode;
}
