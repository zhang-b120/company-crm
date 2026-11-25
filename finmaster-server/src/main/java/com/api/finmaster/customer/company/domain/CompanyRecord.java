package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 客户公司信息对象 customer_company
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_record")
public class CompanyRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -6784563723470539345L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 跟进类型 字典
     */
    @TableField("follow_type")
    private String followType;

    /**
     * 下次跟进时间
     */
    @TableField("next_follow_time")
    private LocalDateTime nextFollowTime;

    /**
     * 跟进时间
     */
    @TableField("follow_time")
    private LocalDateTime followTime;

    /**
     * 跟进内容
     */
    @TableField("follow_content")
    private String followContent;

    /**
     * 重要程度 1-低 2-中 3-高
     */
    @TableField("importance")
    private Integer importance;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 