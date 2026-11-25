package com.qykj.finmaster.customer.lead.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.lead.entity.LeadPhone;
import com.qykj.finmaster.customer.lead.manager.LeadPhoneManager;
import com.qykj.finmaster.customer.lead.mapper.LeadPhoneMapper;
import org.springframework.stereotype.Service;

@Service
public class LeadPhoneManagerImpl extends ManagerImpl<LeadPhoneMapper, LeadPhone> implements LeadPhoneManager {
}
