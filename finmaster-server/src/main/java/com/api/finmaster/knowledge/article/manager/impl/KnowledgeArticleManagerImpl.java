package com.qykj.finmaster.knowledge.article.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.article.mapper.KnowledgeArticleMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeArticleManagerImpl extends ManagerImpl<KnowledgeArticleMapper, KnowledgeArticle>
        implements KnowledgeArticleManager {
}
