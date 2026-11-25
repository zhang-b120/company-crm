package com.qykj.finmaster.knowledge.request.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.request.entity.KnowledgeRequest;
import com.qykj.finmaster.knowledge.request.manager.KnowledgeRequestManager;
import com.qykj.finmaster.knowledge.request.mapper.KnowledgeRequestMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeRequestManagerImpl extends ManagerImpl<KnowledgeRequestMapper, KnowledgeRequest>
        implements KnowledgeRequestManager {
}
