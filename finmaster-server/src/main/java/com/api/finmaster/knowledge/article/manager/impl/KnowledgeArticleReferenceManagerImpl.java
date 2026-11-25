package com.qykj.finmaster.knowledge.article.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticleReference;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleReferenceManager;
import com.qykj.finmaster.knowledge.article.mapper.KnowledgeArticleReferenceMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeArticleReferenceManagerImpl extends ManagerImpl<KnowledgeArticleReferenceMapper, KnowledgeArticleReference>
        implements KnowledgeArticleReferenceManager {
}
