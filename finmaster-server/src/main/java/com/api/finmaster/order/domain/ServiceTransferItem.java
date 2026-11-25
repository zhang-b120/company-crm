package com.qykj.finmaster.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 服务转移工单·明细行（K=1 简化版，合同驱动）：1 工单 = 1 条转移对。
 * 不再创建 0 元退款 Order / 续费 Order / 续费 Contract；
 * Node 3 完成时直接：源 company_service CANCELLED + 该 (公司,业务) 下所有活合同 TERMINATE +
 * 目标 company_service.actualEndDate 改为用户填的 target_actual_end_date。
 *
 * @Author zhb
 * @Date 2026-05-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_transfer_item")
public class ServiceTransferItem extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户 ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 关联 SERVICE_TRANSFER 工单 id（插入时为空，工单创建后回填） */
    @TableField("work_order_id")
    private Long workOrderId;

    /** 源合同 id（入口为合同列表，用户点击的那条合同） */
    @TableField("source_contract_id")
    private Long sourceContractId;

    /** 源合同明细 id（可选） */
    @TableField("source_contract_detail_id")
    private Long sourceContractDetailId;

    /** 源公司 id */
    @TableField("source_company_id")
    private Long sourceCompanyId;

    /** 源业务 id */
    @TableField("source_business_id")
    private Long sourceBusinessId;

    /** 源 company_service id（剩余期来源） */
    @TableField("source_company_service_id")
    private Long sourceCompanyServiceId;

    /** 目标公司 id（接收方） */
    @TableField("target_company_id")
    private Long targetCompanyId;

    /** 目标 company_service id（actualEndDate 顺延） */
    @TableField("target_company_service_id")
    private Long targetCompanyServiceId;

    /** 系统参考用·源服务剩余月数（仅记录，不参与 Node 3 计算） */
    @TableField("transfer_months")
    private Integer transferMonths;

    /** 目标 company_service 的新 actualEndDate（用户在弹窗填写，Node 3 据此覆盖） */
    @TableField("target_actual_end_date")
    private LocalDate targetActualEndDate;

    /** 处理状态：PENDING / DONE / FAILED */
    @TableField("process_status")
    private String processStatus;

    /** Node 3 钩子处理结果备注 */
    @TableField("process_remark")
    private String processRemark;

    /** 逻辑删除（0-否 1-是） */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
