package com.qykj.finmaster.invoice.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.invoice.domain.InvoiceApply;
import com.qykj.finmaster.invoice.manager.InvoiceApplyManager;
import com.qykj.finmaster.invoice.mapper.InvoiceApplyMapper;
import org.springframework.stereotype.Service;

/**
 * 发票申请信息Manager
 */
@Service
public class InvoiceApplyManagerImpl extends ManagerImpl<InvoiceApplyMapper, InvoiceApply> implements InvoiceApplyManager {
} 