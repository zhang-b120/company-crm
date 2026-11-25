package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyRenewListWithServiceReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = -7043512533785531362L;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 管家负责人名称（多选）
     */
    private List<String> butlerOwnerUserName;

    /**
     * 开始时间
     */
    private String companyType;

    /**
     * 开始时间
     */
    private LocalDate start;

    /**
     * 结束时间
     */
    private LocalDate end;

    /**
     * 服务名称（多选；为空时默认查代理记账 + 发票服务）
     */
    private java.util.List<String> serviceName;

    /**
     * 公司ID（精确：续费任务详情按 companyId 反查公司服务）
     */
    private Long companyId;

    /**
     * 剩余服务时长上限（月），筛选 ≤ 该值的记录
     */
    private Integer residueServiceIntervalMax;

}
