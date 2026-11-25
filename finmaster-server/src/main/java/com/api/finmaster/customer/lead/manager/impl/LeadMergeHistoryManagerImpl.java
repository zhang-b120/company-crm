package com.qykj.finmaster.customer.lead.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.lead.entity.LeadMergeHistory;
import com.qykj.finmaster.customer.lead.manager.LeadMergeHistoryManager;
import com.qykj.finmaster.customer.lead.mapper.LeadMergeHistoryMapper;
import org.springframework.stereotype.Service;

@Service
public class LeadMergeHistoryManagerImpl extends ManagerImpl<LeadMergeHistoryMapper, LeadMergeHistory>
        implements LeadMergeHistoryManager {
}
