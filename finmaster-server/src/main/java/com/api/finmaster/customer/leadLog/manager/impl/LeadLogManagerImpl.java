package com.qykj.finmaster.customer.leadLog.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.manager.LeadLogManager;
import com.qykj.finmaster.customer.leadLog.mapper.LeadLogMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class LeadLogManagerImpl extends ManagerImpl<LeadLogMapper, LeadLog> implements LeadLogManager {
}