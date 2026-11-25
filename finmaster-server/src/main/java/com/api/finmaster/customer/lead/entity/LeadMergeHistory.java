package com.qykj.finmaster.customer.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 线索合并审计 lead_merge_history
 * 每次合并写一条快照，便于排查与回滚。
 */
@Data
@TableName("lead_merge_history")
public class LeadMergeHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = -8501248172310992301L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 保留的目标线索ID */
    @TableField("target_lead_id")
    private Long targetLeadId;

    /** 被合并的源线索ID（已软删） */
    @TableField("source_lead_id")
    private Long sourceLeadId;

    /** 源线索关键字段快照 JSON */
    @TableField("source_lead_snapshot")
    private String sourceLeadSnapshot;

    /** 源线索下客户关键字段快照 JSON 数组 */
    @TableField("customer_snapshot")
    private String customerSnapshot;

    /** 源线索下客户公司关键字段快照 JSON 数组 */
    @TableField("cc_snapshot")
    private String ccSnapshot;

    @TableField("operator")
    private String operator;

    @TableField("operator_name")
    private String operatorName;

    @TableField("create_time")
    private Date createTime;
}
