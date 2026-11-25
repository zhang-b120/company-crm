package com.qykj.finmaster.customer.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.resp.GetCompanyListWithServiceResp;
import com.qykj.finmaster.customer.company.domain.resp.GetCompanyRecordListWithServiceResp;
import com.qykj.finmaster.customer.company.domain.resp.GetCompanyRenewListWithServiceResp;
import com.qykj.finmaster.customer.company.domain.resp.GetCompanyServiceInfo;
import com.qykj.finmaster.customer.company.domain.resp.RenewalStatDetailRow;
import com.qykj.finmaster.customer.company.domain.resp.RenewalStatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyServiceMapper extends BaseMapper<CompanyService> {

    List<GetCompanyListWithServiceResp> getCompanyListWithService(@Param("params") Map<String, Object> params);

    List<GetCompanyRecordListWithServiceResp> getCompanyRecordListWithService(@Param("params") Map<String, Object> params);

    List<GetCompanyRenewListWithServiceResp> getCompanyRenewListWithService(@Param("params") Map<String, Object> params);

    List<GetCompanyServiceInfo> getCompanyServiceByCompany(@Param("companyId") Long companyId);

    /** 续费任务详情专用：按 companyId 查公司全部服务（不过滤数据权限） */
    List<GetCompanyRenewListWithServiceResp> listAllServicesByCompanyIdForRenewal(@Param("companyId") Long companyId);

    /**
     * 续费统计：按 actual_end_date 的年月分组，统计每月公司数(DISTINCT company_id)与合同金额(SUM(contract_price))。
     * 只返回区间内有数据的月份，补零由 Service 完成。口径对齐 getCompanyRenewListWithService（合同 join / 公司状态排除 / actual_end_date 区间）。
     */
    List<RenewalStatRow> getRenewalStats(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                         @Param("butlerOwnerUserName") List<String> butlerOwnerUserName,
                                         @Param("serviceName") List<String> serviceName);

    /**
     * 续费统计下钻明细：按 actual_end_date 区间返回公司服务明细（按服务逐行）。
     * WHERE 与 getRenewalStats 逐条一致，保证明细行数 = 该月「服务数量」。
     */
    List<RenewalStatDetailRow> getRenewalStatDetail(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                                    @Param("butlerOwnerUserName") List<String> butlerOwnerUserName,
                                                    @Param("serviceName") List<String> serviceName);
}