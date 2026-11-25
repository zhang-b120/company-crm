package com.qykj.finmaster.order.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 服务转移工单上下文响应：聚合源公司 / 目标公司 / 源合同 / 源服务 / 目标服务 等富字段，
 * 供工单详情页渲染完整信息块。
 *
 * @Author zhb
 * @Date 2026-05-13
 */
@Data
public class ServiceTransferContextResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** service_transfer_item 主键 */
    private Long itemId;

    /** 关联工单 id */
    private Long workOrderId;

    /** 转移月数（参考值） */
    private Integer transferMonths;

    /** 目标公司服务的新 actualEndDate */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetActualEndDate;

    /** 处理状态：PENDING / DONE / FAILED */
    private String processStatus;

    /** 处理结果备注（失败时含错误信息） */
    private String processRemark;

    /** 转移原因（从工单 meta 取） */
    private String refundReason;

    /** 源公司信息 */
    private CompanyBrief sourceCompany;

    /** 目标公司信息 */
    private CompanyBrief targetCompany;

    /** 源合同信息 */
    private ContractBrief sourceContract;

    /** 源公司服务（剩余期来源） */
    private ServiceBrief sourceService;

    /** 目标公司服务（接收方） */
    private ServiceBrief targetService;

    /** 业务名（源 / 目标必须一致） */
    private String businessName;

    @Data
    public static class CompanyBrief implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long id;
        private String companyName;
        private String taxNumber;
        private String legalName;
        private String legalPhone;
        private String companyType;
        private String companyTypeName;
        private String customerName;
    }

    @Data
    public static class ContractBrief implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long id;
        private String contractNo;
        /** NEW / RENEW */
        private String contractType;
        /** DRAFT / COMPLETED / TERMINATED / DISUSED */
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd") private Date contractStartTime;
        @JsonFormat(pattern = "yyyy-MM-dd") private Date contractEndTime;
        private BigDecimal contractPrice;
    }

    @Data
    public static class ServiceBrief implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long companyServiceId;
        private String serviceName;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate effectiveDate;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate endDate;
        @JsonFormat(pattern = "yyyy-MM-dd") private LocalDate actualEndDate;
        /** ACTIVE / CANCELLED */
        private String status;
    }
}
