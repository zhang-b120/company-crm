package com.qykj.finmaster.data.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.data.domain.req.GetPaymentDataDetailPageReq;
import com.qykj.finmaster.data.domain.req.GetPaymentDataReq;
import com.qykj.finmaster.data.domain.resp.ExportLeadAnalysisDataResp;
import com.qykj.finmaster.data.domain.resp.ExportPaymentDataDetailPageResp;
import com.qykj.finmaster.data.domain.resp.GetPaymentDataDetailPageResp;
import com.qykj.finmaster.data.domain.resp.GetPaymentDataResp;
import com.qykj.finmaster.data.mapper.PaymentDataMapper;
import com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentDataServiceImpl {

    @Autowired
    private PaymentDataMapper paymentDataMapper;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;

    /**
     * 获取线索
     * @param req
     * @return
     */
    public List<GetPaymentDataResp> list(GetPaymentDataReq req) {
        List<GetPaymentDataResp> respList = new ArrayList<>();
        Map<String, Object> params = req.toBeanMap();
        //默认为昨天的时间
        Date start = DateUtils.fewDaysAgoDateStart(GlobalConstant.Number.ONE_INT);
        Date end = DateUtils.fewDaysAgoDateEnd(GlobalConstant.Number.ONE_INT);
        if(ObjectUtils.isNotEmpty(req.getStart())){
            start = DateUtils.localDateTimeToDate(req.getStart().atStartOfDay());
        }
        if(ObjectUtils.isNotEmpty(req.getEnd())){
            end = DateUtils.localDateTimeToDate(req.getEnd().atTime(23, 59, 59, 999));
        }
        params.put("start", start);
        params.put("end", end);
        List<GetPaymentDataResp> paymentDataList = paymentDataMapper.list(params);
        if(CollUtil.isEmpty(paymentDataList)){
            return respList;
        }
        Map<String, BigDecimal> collect = paymentDataList.stream().collect(Collectors.toMap(GetPaymentDataResp::getPaymentType, GetPaymentDataResp::getPaymentAmount));
        //新增一个类型为全部的对象，并确保在首位
        GetPaymentDataResp resp = new GetPaymentDataResp();
        resp.setPaymentTypeName("全部");
        resp.setPaymentAmount(paymentDataList
                .stream()
                .map(GetPaymentDataResp::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        respList.add(resp);
        //获取字典数据（部门21使用PAYMENT_TYPE_LMSS字典）
        String dictCode = (req.getDeptId() != null && req.getDeptId() == 21L) ? DictConstants.PAYMENT_TYPE_LMSS : DictConstants.PAYMENT_TYPE;
        List<GetDictDataListResp> dictDataListByTypeCode = sysDictDataService.getDictDataListByTypeCode(dictCode);
        if(CollUtil.isNotEmpty(dictDataListByTypeCode)){
            dictDataListByTypeCode.forEach(item -> {
                GetPaymentDataResp data = new GetPaymentDataResp();
                data.setPaymentType(item.getValue());
                data.setPaymentTypeName(item.getLabel());
                if(MapUtils.isNotEmpty(collect) && ObjectUtils.isNotEmpty(collect.get(item.getValue()))){
                    data.setPaymentAmount(collect.get(item.getValue()));
                }else{
                    data.setPaymentAmount(new BigDecimal(GlobalConstant.Number.ZERO_INT));
                }
                respList.add(data);
            });
        }
        return respList;
    }

    /**
     * 获取字典数据
     * @param code
     * @return
     */
    private Map<String, String> getDictDataMap(String code){
        Map<String, String> dataMap = sysDictDataService.getDictDataMapByTypeCode(code);
        return dataMap;
    }

    private Map<String, String> getDictDataMapByTypeCode(List<String> codeList){
        Map<String, String> dataMap = sysDictDataService.getDictDataMapByTypeCode(codeList);
        return dataMap;
    }

    /**
     * 获取支付数据明细
     * @param req
     * @return
     */
    public List<GetPaymentDataDetailPageResp> getPaymentDataDetailData(GetPaymentDataDetailPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        //默认为昨天的时间
        Date start = DateUtils.fewDaysAgoDateStart(GlobalConstant.Number.ONE_INT);
        Date end = DateUtils.fewDaysAgoDateEnd(GlobalConstant.Number.ONE_INT);
        if(ObjectUtils.isNotEmpty(req.getStart())){
            start = DateUtils.localDateTimeToDate(req.getStart().atStartOfDay());
        }
        if(ObjectUtils.isNotEmpty(req.getEnd())){
            end = DateUtils.localDateTimeToDate(req.getEnd().atTime(23, 59, 59, 999));
        }
        params.put("start", start);
        params.put("end", end);
        return paymentDataDetailData(params, req.getDeptId());
    }

    /**
     * 获取支付明细数据
     * @param params
     * @param deptId
     * @return
     */
    private List<GetPaymentDataDetailPageResp> paymentDataDetailData(Map<String, Object> params, Long deptId){
        List<GetPaymentDataDetailPageResp> respList = paymentDataMapper.paymentDataDetailPage(params);
        if(ObjectUtils.isEmpty(respList)){
            return Collections.emptyList();
        }
        //获取字典数据（部门21使用PAYMENT_TYPE_LMSS字典）
        String paymentDictCode = (deptId != null && deptId == 21L) ? DictConstants.PAYMENT_TYPE_LMSS : DictConstants.PAYMENT_TYPE;
        Map<String, String> dictDataMap = getDictDataMapByTypeCode(Arrays.asList(paymentDictCode, DictConstants.ORDER_DETAIL_TYPE));
        respList.forEach(item -> {
            if(MapUtils.isNotEmpty(dictDataMap)){
                if(StringUtils.isNotEmpty(item.getPaymentType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getPaymentType())))
                    item.setPaymentTypeName(dictDataMap.get(item.getPaymentType()));
                if(StringUtils.isNotEmpty(item.getOrderDetailType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getOrderDetailType())))
                    item.setOrderDetailTypeName(dictDataMap.get(item.getOrderDetailType()));
            }
        });
        return respList;
    }

    /**
     * 导出支付数据明细
     * @param req
     * @param response
     * @return
     */
    public void exportPaymentDataDetailPage(GetPaymentDataDetailPageReq req, HttpServletResponse response) {
        Map<String, Object> params = req.toBeanMap();
        //默认为昨天的时间
        Date start = DateUtils.fewDaysAgoDateStart(GlobalConstant.Number.ONE_INT);
        Date end = DateUtils.fewDaysAgoDateEnd(GlobalConstant.Number.ONE_INT);
        if(ObjectUtils.isNotEmpty(req.getStart())){
            start = DateUtils.localDateTimeToDate(req.getStart().atStartOfDay());
        }
        if(ObjectUtils.isNotEmpty(req.getEnd())){
            end = DateUtils.localDateTimeToDate(req.getEnd().atTime(23, 59, 59, 999));
        }
        params.put("start", start);
        params.put("end", end);
        List<ExportPaymentDataDetailPageResp> exportList = CopierCopyUtils.convertTo(this.paymentDataDetailData(params, req.getDeptId()), ExportPaymentDataDetailPageResp.class);
        ExcelUtil<ExportPaymentDataDetailPageResp> util = new ExcelUtil<>(ExportPaymentDataDetailPageResp.class);
        util.exportExcel(response, exportList, "支付明细数据");
    }
}
