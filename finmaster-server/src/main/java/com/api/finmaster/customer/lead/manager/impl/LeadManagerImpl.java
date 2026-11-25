package com.qykj.finmaster.customer.lead.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.LeadManager;
import com.qykj.finmaster.customer.lead.mapper.LeadMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class LeadManagerImpl extends ManagerImpl<LeadMapper, Lead> implements LeadManager {
}