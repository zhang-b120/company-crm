package com.qykj.finmaster.data.mapper;


import com.qykj.finmaster.customer.lead.entity.dto.GetLeadDataDTO;
import com.qykj.finmaster.data.domain.dto.SaleLeadOrCustomerDataDto;
import com.qykj.finmaster.data.domain.dto.SaleOrderDataDto;
import com.qykj.finmaster.order.domain.dto.GetOrderDetailDataDTO;
import com.qykj.finmaster.system.appletUser.domain.dto.GetAppletUserDataDTO;
import com.qykj.finmaster.system.user.entity.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface LeadAnalysisMapper {

    List<User> getDepartmentUser(@Param("departmentId") Long departmentId);

    List<GetOrderDetailDataDTO> getOrderDetailData(@Param("params") Map<String, Object> params);

    List<GetAppletUserDataDTO> getAppletUserData(@Param("params") Map<String, Object> params);

    List<GetLeadDataDTO> getLeadData(@Param("params") Map<String, Object> params);

    List<GetLeadDataDTO> getPromotionLeadData(@Param("params") Map<String, Object> params);

    List<GetOrderDetailDataDTO> getPromotionOrderDetailData(@Param("params") Map<String, Object> params);

    List<SaleLeadOrCustomerDataDto> getSaleAllLeadList(@Param("params") Map<String, Object> params);

    List<SaleLeadOrCustomerDataDto> getSaleTransactionLeadList(@Param("params") Map<String, Object> params);

    List<SaleLeadOrCustomerDataDto> getSaleAllCustomerList(@Param("params") Map<String, Object> params);

    List<SaleLeadOrCustomerDataDto> getSaleTransactionCustomerList(@Param("params") Map<String, Object> params);

    List<SaleOrderDataDto> getSaleOrderList(@Param("params") Map<String, Object> params);

    List<SaleLeadOrCustomerDataDto> getSalesAllOrderByLead(@Param("params") Map<String, Object> params);
}
