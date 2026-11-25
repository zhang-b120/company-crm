package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 小程序员工接口请求体（save / list / delete 共用）
 */
@Data
public class UaCompanyEmployeeReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "openId 不能为空")
    private String openId;

    /** save / list 必传，delete 可空（delete 用 employeeId 反查公司归属） */
    private Long companyId;

    /** saveByTaxNumber 端点使用：按税号定位公司（companyId 与 taxNumber 二选一） */
    private String taxNumber;

    /** save 单条新增/更新时使用 */
    private CompanyEmployee employee;

    /** save 批量保存时使用（一次替换该公司全部员工） */
    private List<CompanyEmployee> employees;

    /** delete 用 */
    private Long employeeId;
}
