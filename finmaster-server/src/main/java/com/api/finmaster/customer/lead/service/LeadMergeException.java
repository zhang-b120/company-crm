package com.qykj.finmaster.customer.lead.service;

import com.qykj.finmaster.common.exception.BusinessException;

/**
 * 线索合并业务异常
 */
public class LeadMergeException extends BusinessException {
    public LeadMergeException(String message) {
        super(message);
    }
}
