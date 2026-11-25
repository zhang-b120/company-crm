package com.qykj.finmaster.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.payment.domain.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录Mapper接口
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
} 