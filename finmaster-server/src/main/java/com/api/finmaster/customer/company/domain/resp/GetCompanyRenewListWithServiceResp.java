package com.qykj.finmaster.customer.company.domain.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyRenewListWithServiceResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -1453535316506715947L;

    /**
     * 公司ID
     */
    private Long id;

    /**
     * 公司服务ID
     */
    private Long companyServiceId;

    /**
     * 服务名称名称
     */
    private String serviceName;

    /**
     * 公司名称
     */
    private String companyName;

    /** 所属客户 */
    private String customerName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 管家负责人
     */
    private String butlerOwnerUserName;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 公司类型名称
     */
    private String companyTypeName;

    /**
     * 合同开始日期
     */
    private LocalDate effectiveDate;

    /**
     * 合同结束日期
     */
    private LocalDate endDate;

    /**
     * 实际结束日期
     */
    private LocalDate actualEndDate;

    /**
     * 已服务时长（月）
     */
    private Integer serviceInterval;

    /**
     * 剩余服务时长
     */
    private Integer residueServiceInterval;

    /**
     * 最近续费类型 0-正常续费 1-提前续费 2-延期续费 3-未续费
     */
    private Integer lastRenewType;

    /**
     * 最近续费备注
     */
    private String lastRenewRemark;

    /** 最近一次续费金额（company_service_renew.renew_amount） */
    private java.math.BigDecimal lastRenewAmount;

    /** 来源合同 ID（cs.source_id → contract.id），续费时按此查产品明细 */
    private Long contractId;

    /** 来源合同编号（cs.source_id → contract.contract_no） */
    private String contractNo;

    /** 来源合同金额（contract.contract_price） */
    private java.math.BigDecimal contractPrice;

    /** 该合同包含的服务内容（contract_detail.product_name 拼接） */
    private String contractServices;

    /** 业务ID（cs.business_id），用于续费完成时回填订单明细的 business_id */
    private Long businessId;

    /** 服务维护ID（cs.service_id） */
    private Long serviceId;

    /** 关联最近一条续费工单 ID（mapper 反查 work_order） */
    private Long workOrderId;
    /** 关联工单号 */
    private String workOrderTaskNo;
    /** 工单状态：DRAFT/PENDING/RUNNING/WAITING_CHILD/SUSPENDED/DONE/CANCELED */
    private String workOrderStatus;
}