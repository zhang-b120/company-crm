package com.qykj.finmaster.workorder.domain.resp;

import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CompanyLegalPerson;
import com.qykj.finmaster.customer.company.domain.CompanyTaxCredential;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 派工弹窗 BOOKKEEPING 分支预填返回
 * 前端一次性拉到公司/法人、股东、员工、税务凭证；行业等天眼查字段单独再拉
 */
@Data
public class BookkeepingDispatchPreloadResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private CustomerCompany companyInfo;
    private List<CompanyLegalPerson> shareholders;
    private List<CompanyEmployee> employees;
    private CompanyTaxCredential taxCredential;
}
