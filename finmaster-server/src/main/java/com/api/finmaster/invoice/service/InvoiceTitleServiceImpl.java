package com.qykj.finmaster.invoice.service;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.invoice.domain.InvoiceTitle;
import com.qykj.finmaster.invoice.domain.req.SaveInvoiceTitleReq;
import com.qykj.finmaster.invoice.domain.resp.GetInvoiceTitleResp;
import com.qykj.finmaster.invoice.manager.impl.InvoiceTitleManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 发票抬头信息Service实现类
 */
@Service
public class InvoiceTitleServiceImpl {

    @Autowired
    private InvoiceTitleManagerImpl invoiceTitleManager;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    /**
     * 保存公司抬头
     * @param req
     * @return
     */
    public AjaxResult saveInvoiceTitle(SaveInvoiceTitleReq req) {
        if(ObjectUtils.isEmpty(req) || ObjectUtils.isEmpty(req.getCompanyId())){
            return AjaxResult.error(ResultMsg.INVOICE_TITLE_NOT_DATA);
        }
        CustomerCompany customerCompany = customerCompanyManager.getById(req.getCompanyId());
        if(ObjectUtils.isEmpty(customerCompany)){
            return AjaxResult.error(ResultMsg.INVOICE_TITLE_NOT_COMPANY);
        }
        InvoiceTitle invoiceTitle = CopierCopyUtils.convertTo(req, InvoiceTitle.class);
        if(ObjectUtils.isEmpty(invoiceTitle.getCustomerId()) && ObjectUtils.isNotEmpty(customerCompany.getCustomerId())){
            invoiceTitle.setCustomerId(customerCompany.getCustomerId());
        }
        // 没有id则为新增
        if(ObjectUtils.isEmpty(req.getId())) {
            invoiceTitle.setCreateTime(new Date());
        }
        invoiceTitle.setUpdateTime(new Date());
        invoiceTitleManager.saveOrUpdate(invoiceTitle);
        return AjaxResult.success(ResultMsg.INVOICE_TITLE_SUCCESS);
    }

    /**
     * 获取公司抬头
     * @param companyId
     * @return
     */
    public GetInvoiceTitleResp getInvoiceTitle(Long companyId) {
        GetInvoiceTitleResp resp = null;
        if(ObjectUtils.isNotEmpty(companyId)){
            InvoiceTitle invoiceTitle = invoiceTitleManager.lambdaQuery()
                    .eq(InvoiceTitle::getCompanyId, companyId)
                    .one();
            if(ObjectUtils.isNotEmpty(invoiceTitle)){
                resp = CopierCopyUtils.convertTo(invoiceTitle, GetInvoiceTitleResp.class);
            }
        }
        return resp;
    }
}