package com.qykj.finmaster.cs.profile.mapper;

import com.qykj.finmaster.cs.profile.domain.FinancialAccountSetInfo;
import com.qykj.finmaster.cs.profile.domain.FinancialIndividualTaxInfo;
import com.qykj.finmaster.cs.profile.domain.FinancialTaxOfficer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FinancialProfileMapper {

    /** 按税号批量查询账套基本信息 */
    List<FinancialAccountSetInfo> getAccountSetsByTaxNumbers(@Param("taxNumbers") List<String> taxNumbers);

    /** 按账套ID批量查询办税员列表 */
    List<FinancialTaxOfficer> getTaxOfficersByAccountSetIds(@Param("accountSetIds") List<Long> accountSetIds);

    /** 按账套ID查询该公司的办税员列表（单个公司使用） */
    List<FinancialTaxOfficer> getTaxOfficersByAccountSetId(@Param("accountSetId") Long accountSetId);

    /** 按账套ID批量查询最新个税验证记录 */
    List<FinancialIndividualTaxInfo> getLatestIndividualTaxByAccountSetIds(@Param("accountSetIds") List<Long> accountSetIds);

    /** 按税号获取账套ID */
    Long getAccountSetIdByTaxNumber(@Param("taxNumber") String taxNumber);
}
