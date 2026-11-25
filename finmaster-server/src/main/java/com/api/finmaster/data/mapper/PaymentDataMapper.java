package com.qykj.finmaster.data.mapper;

import com.qykj.finmaster.data.domain.resp.GetPaymentDataDetailPageResp;
import com.qykj.finmaster.data.domain.resp.GetPaymentDataResp;
import com.qykj.finmaster.data.domain.resp.OrderOverviewItemResp;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PaymentDataMapper {

    List<GetPaymentDataResp> list(@Param("params") Map<String, Object> params);

    List<GetPaymentDataDetailPageResp> paymentDataDetailPage(@Param("params") Map<String, Object> params);

    /** 按日统计订单成交和退款数据 */
    List<OrderOverviewItemResp> getOrderOverviewDaily(@Param("params") Map<String, Object> params);

    /** 按月统计订单成交和退款数据 */
    List<OrderOverviewItemResp> getOrderOverviewMonthly(@Param("params") Map<String, Object> params);
}
