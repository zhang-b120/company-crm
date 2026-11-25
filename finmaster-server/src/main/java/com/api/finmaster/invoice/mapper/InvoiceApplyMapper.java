package com.qykj.finmaster.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.invoice.domain.InvoiceApply;
import com.qykj.finmaster.invoice.domain.resp.GetInvoiceApplyListResp;
import com.qykj.finmaster.invoice.domain.resp.GetInvoiceApplyPageListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 发票申请信息Mapper接口
 */
@Mapper
public interface InvoiceApplyMapper extends BaseMapper<InvoiceApply> {

    List<GetInvoiceApplyListResp> getInvoiceApplyList(@Param("params") Map<String, Object> params);

    List<GetInvoiceApplyPageListResp> getInvoiceApplyPageList(@Param("params") Map<String, Object> params);
} 