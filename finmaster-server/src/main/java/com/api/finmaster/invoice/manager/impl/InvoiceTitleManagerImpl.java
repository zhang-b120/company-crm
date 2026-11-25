package com.qykj.finmaster.invoice.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.invoice.domain.InvoiceTitle;
import com.qykj.finmaster.invoice.manager.InvoiceTitleManager;
import com.qykj.finmaster.invoice.mapper.InvoiceTitleMapper;
import org.springframework.stereotype.Service;

/**
 * 发票抬头信息Manager
 */
@Service
public class InvoiceTitleManagerImpl extends ManagerImpl<InvoiceTitleMapper, InvoiceTitle> implements InvoiceTitleManager {
} 