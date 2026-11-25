package com.qykj.finmaster.workorder.domain.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CompanyLegalPerson;
import com.qykj.finmaster.customer.company.domain.CompanyTaxCredential;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 代账派工专用负载：一次性携带公司/法人、股东、员工、税务凭证 4 块数据
 * 提交时后端事务里先 upsert 这 4 块，再创建 work_order
 *
 * 加 ignoreUnknown：前端 form.bk 可能挂一些 UI-only 临时字段（如 legalIdFiles / businessLicenseFiles，
 * 它们走单独的附件上传通道），允许多余字段静默忽略，不再因为前端临时字段抛 UnrecognizedPropertyException
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookkeepingDispatchPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公司基本信息 + 法人信息（合并一张表） */
    private CustomerCompany companyInfo;

    /** 股东列表（全量替换当前 company_legal_person 数据） */
    private List<CompanyLegalPerson> shareholders;

    /** 员工名册（全量替换当前 company_employee 数据） */
    private List<CompanyEmployee> employees;

    /** 税务凭证（个税 / 电子税务局 密码 + APP 下载状态） */
    private CompanyTaxCredential taxCredential;
}
