package com.qykj.finmaster.oa.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 付款申请Mapper接口
 */
@Mapper
public interface OaPaymentMapper extends BaseMapper<OaPayment> {
}
