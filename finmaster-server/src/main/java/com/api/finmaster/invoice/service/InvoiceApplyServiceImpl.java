package com.qykj.finmaster.invoice.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.invoice.domain.InvoiceApply;
import com.qykj.finmaster.invoice.domain.InvoiceApplyDetail;
import com.qykj.finmaster.invoice.domain.InvoiceTitle;
import com.qykj.finmaster.invoice.domain.dto.InvoiceApplyDetailInfo;
import com.qykj.finmaster.invoice.domain.req.GetInvoiceApplyListReq;
import com.qykj.finmaster.invoice.domain.req.GetInvoiceApplyPageListReq;
import com.qykj.finmaster.invoice.domain.req.SaveInvoiceApplyReq;
import com.qykj.finmaster.invoice.domain.req.UpdateApplyInvoiceStatusReq;
import com.qykj.finmaster.invoice.domain.resp.GetInvoiceApplyListResp;
import com.qykj.finmaster.invoice.domain.resp.GetInvoiceApplyPageListResp;
import com.qykj.finmaster.invoice.manager.impl.InvoiceApplyDetailManagerImpl;
import com.qykj.finmaster.invoice.manager.impl.InvoiceApplyManagerImpl;
import com.qykj.finmaster.invoice.manager.impl.InvoiceTitleManagerImpl;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 发票申请信息Service实现类
 */
@Service
public class InvoiceApplyServiceImpl {

    @Autowired
    private InvoiceApplyManagerImpl invoiceApplyManager;
    @Autowired
    private InvoiceTitleManagerImpl invoiceTitleManager;
    @Autowired
    private InvoiceApplyDetailManagerImpl invoiceApplyDetailManager;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;

    /**
     * 保存发票信息
     * @param req
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult saveInvoiceApply(SaveInvoiceApplyReq req) {
        if(ObjectUtils.isEmpty(req)){
            return AjaxResult.error(ResultMsg.INVOICE_APPLY_NOT_DATA);
        }
        if(CollUtil.isEmpty(req.getDetailInfoList())){
            return AjaxResult.error(ResultMsg.INVOICE_APPLY_NOT_DETAIL_DATA);
        }
        if(ObjectUtils.isEmpty(req.getCompanyId())){
            return AjaxResult.error(ResultMsg.INVOICE_APPLY_NOT_COMPANY);
        }
        InvoiceApply invoiceApply = CopierCopyUtils.convertTo(req, InvoiceApply.class);
        InvoiceTitle invoiceTitle = invoiceTitleManager.lambdaQuery()
                .eq(InvoiceTitle::getCompanyId, req.getCompanyId())
                .one();
        if(ObjectUtils.isEmpty(invoiceTitle)){
            return AjaxResult.error(ResultMsg.INVOICE_APPLY_NOT_TITLE);
        }
        // 没有ID代表新增 需要添加销售方抬头
        if(ObjectUtils.isEmpty(req.getId())){
            invoiceApply.setInvoiceTitleId(invoiceTitle.getId());
            invoiceApply.setCustomerId(invoiceTitle.getCustomerId());
            invoiceApply.setCreateTime(new Date());
        }
        invoiceApply.setStatus(DictConstants.INVOICE_STATUS_APPLY);
        invoiceApplyManager.saveOrUpdate(invoiceApply);
        List<InvoiceApplyDetail> detailList = req.getDetailInfoList().stream().map(item -> {
            InvoiceApplyDetail invoiceApplyDetail = CopierCopyUtils.convertTo(item, InvoiceApplyDetail.class);
            if (ObjectUtils.isEmpty(invoiceApplyDetail.getInvoiceApplyId())) {
                invoiceApplyDetail.setInvoiceApplyId(invoiceApply.getId());
                invoiceApplyDetail.setCompanyId(invoiceApply.getCompanyId());
                invoiceApplyDetail.setCustomerId(invoiceTitle.getCustomerId());
            }
            return invoiceApplyDetail;
        }).toList();
        List<Long> detailIdList = detailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getId())).map(InvoiceApplyDetail::getId).toList();
        //删除出不包含于本次修改的明细
        if(CollUtil.isNotEmpty(detailIdList)) {
            invoiceApplyDetailManager.lambdaUpdate()
                    .set(InvoiceApplyDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                    .eq(InvoiceApplyDetail::getInvoiceApplyId, invoiceApply.getId())
                    .notIn(InvoiceApplyDetail::getId, detailIdList)
                    .update();
        }
        invoiceApplyDetailManager.saveOrUpdateBatch(detailList);
        return AjaxResult.success(ResultMsg.INVOICE_APPLY_SUCCESS);
    }

    /**
     * 删除发票信息
     * @param id
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteInvoiceApply(Long id) {
        if(ObjectUtils.isEmpty(id)){
            return Boolean.FALSE;
        }
        invoiceApplyDetailManager.lambdaUpdate()
                .set(InvoiceApplyDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                .eq(InvoiceApplyDetail::getInvoiceApplyId, id)
                .update();
        invoiceApplyManager.lambdaUpdate()
                .set(InvoiceApply::getDeleted, GlobalConstant.Number.ONE_INT)
                .eq(InvoiceApply::getId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 获取发票信息
     * @param req
     * @return
     */
    public List<GetInvoiceApplyListResp> getInvoiceApplyList(GetInvoiceApplyListReq req) {
        Map<String, Object> params = req.toBeanMap();
        List<GetInvoiceApplyListResp> invoiceApplyList = invoiceApplyManager.getBaseMapper().getInvoiceApplyList(params);
        if(CollUtil.isEmpty(invoiceApplyList)){
            return Collections.emptyList();
        }
        // 获取字典值
        Map<String, String> dictDataMap = sysDictDataService.getDictDataMapByTypeCode(
                Arrays.asList(DictConstants.INVOICE_STATUS, DictConstants.INVOICE_TYPE, DictConstants.INVOICE_TITLE_TYPE));
        if(MapUtils.isNotEmpty(dictDataMap)) {
            invoiceApplyList.forEach(item -> {
                if(dictDataMap.containsKey(item.getStatus()) && StringUtils.isNotEmpty(dictDataMap.get(item.getStatus()))){
                    item.setStatusName(dictDataMap.get(item.getStatus()));
                }
                if(dictDataMap.containsKey(item.getInvoiceType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getInvoiceType()))){
                    item.setInvoiceTypeName(dictDataMap.get(item.getInvoiceType()));
                }
                if(dictDataMap.containsKey(item.getBuyerType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getBuyerType()))){
                    item.setBuyerTypeName(dictDataMap.get(item.getBuyerType()));
                }
            });
        }
        return invoiceApplyList;

    }

    /**
     * 查询开票服务列表-分页
     * @param req
     * @return
     */
    public List<GetInvoiceApplyPageListResp> getInvoiceApplyPageList(GetInvoiceApplyPageListReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetInvoiceApplyPageListResp> invoiceApplyPageList = invoiceApplyManager.getBaseMapper().getInvoiceApplyPageList(params);
        if(CollUtil.isEmpty(invoiceApplyPageList)){
            return Collections.emptyList();
        }
        // 获取字典值
        Map<String, String> dictDataMap = sysDictDataService.getDictDataMapByTypeCode(
                Arrays.asList(DictConstants.INVOICE_STATUS, DictConstants.INVOICE_TYPE, DictConstants.INVOICE_TITLE_TYPE));
        if(MapUtils.isNotEmpty(dictDataMap)) {
            invoiceApplyPageList.forEach(item -> {
                if(dictDataMap.containsKey(item.getStatus()) && StringUtils.isNotEmpty(dictDataMap.get(item.getStatus()))){
                    item.setStatusName(dictDataMap.get(item.getStatus()));
                }
                if(dictDataMap.containsKey(item.getInvoiceType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getInvoiceType()))){
                    item.setInvoiceTypeName(dictDataMap.get(item.getInvoiceType()));
                }
                if(dictDataMap.containsKey(item.getBuyerType()) && StringUtils.isNotEmpty(dictDataMap.get(item.getBuyerType()))){
                    item.setBuyerTypeName(dictDataMap.get(item.getBuyerType()));
                }
                if(CollUtil.isNotEmpty(item.getDetailInfoList())){
                    item.setAmountSum(item.getDetailInfoList().stream().map(InvoiceApplyDetailInfo::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }
            });
        }
        return invoiceApplyPageList;
    }

    /**
     * 更改发票状态
     * @param req
     * @return
     */
    public Boolean updateInvoiceStatus(UpdateApplyInvoiceStatusReq req) {
        if(ObjectUtils.isEmpty(req.getId())){
            return Boolean.FALSE;
        }
        invoiceApplyManager.lambdaUpdate()
                .set(InvoiceApply::getStatus, DictConstants.INVOICE_STATUS_SUCCESS)
                .set(StringUtils.isNotEmpty(req.getInvoiceUrl()), InvoiceApply::getInvoiceUrl, req.getInvoiceUrl())
                .eq(InvoiceApply::getId, req.getId())
                .update();
        return Boolean.TRUE;
    }
}