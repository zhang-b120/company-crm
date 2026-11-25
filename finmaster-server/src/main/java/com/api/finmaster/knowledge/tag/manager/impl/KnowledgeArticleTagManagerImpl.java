package com.qykj.finmaster.knowledge.tag.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.tag.entity.KnowledgeArticleTag;
import com.qykj.finmaster.knowledge.tag.manager.KnowledgeArticleTagManager;
import com.qykj.finmaster.knowledge.tag.mapper.KnowledgeArticleTagMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeArticleTagManagerImpl extends ManagerImpl<KnowledgeArticleTagMapper, KnowledgeArticleTag>
        implements KnowledgeArticleTagManager {
}
