package com.qykj.finmaster.cs.profile.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@TableName("cs_communication_record")
@EqualsAndHashCode(callSuper = false)
public class CsCommunicationRecord extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("company_id")
    private Long companyId;

    /** 沟通方式 PHONE/WECHAT/VISIT/EMAIL/OTHER */
    @TableField("communicate_type")
    private String communicateType;

    @TableField("communicate_time")
    private LocalDateTime communicateTime;

    @TableField("content")
    private String content;

    @TableField("operator_id")
    private Long operatorId;

    @TableField("operator_name")
    private String operatorName;

    @TableLogic
    private Integer deleted;
}
