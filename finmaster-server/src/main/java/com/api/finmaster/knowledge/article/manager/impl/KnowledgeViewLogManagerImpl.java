package com.qykj.finmaster.knowledge.article.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeViewLog;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeViewLogManager;
import com.qykj.finmaster.knowledge.article.mapper.KnowledgeViewLogMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeViewLogManagerImpl extends ManagerImpl<KnowledgeViewLogMapper, KnowledgeViewLog>
        implements KnowledgeViewLogManager {
}
