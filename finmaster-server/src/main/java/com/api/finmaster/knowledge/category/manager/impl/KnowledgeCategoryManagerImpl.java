package com.qykj.finmaster.knowledge.category.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.category.entity.KnowledgeCategory;
import com.qykj.finmaster.knowledge.category.manager.KnowledgeCategoryManager;
import com.qykj.finmaster.knowledge.category.mapper.KnowledgeCategoryMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeCategoryManagerImpl extends ManagerImpl<KnowledgeCategoryMapper, KnowledgeCategory>
        implements KnowledgeCategoryManager {
}
