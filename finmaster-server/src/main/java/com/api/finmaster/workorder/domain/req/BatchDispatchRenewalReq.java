package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 批量下发续费工单
 * 在公司续费列表多选公司后调用
 */
@Data
public class BatchDispatchRenewalReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "请至少选择一家公司")
    private List<RenewalTarget> targets;

    /** 统一指派给同一处理人 */
    @NotBlank(message = "请选择处理人")
    private String assignedOwnerUser;

    /** 任务截止时间（可选） */
    private Date dueTime;

    /** 备注（统一附在每条任务上） */
    private String remark;

    @Data
    public static class RenewalTarget implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 公司 ID（必填） */
        private Long companyId;
        /** 客户 ID（可选） */
        private Long customerId;
        /** 关联老合同 ID（可选；用于详情页展示原合同） */
        private Long contractId;
        /** 公司服务 ID（可选；传入后任务 sourceType=COMPANY_SERVICE / sourceId=companyServiceId，支持按服务追溯多次续费） */
        private Long companyServiceId;
        /** 公司名称（仅用于自动生成 title） */
        private String companyName;
    }
}
