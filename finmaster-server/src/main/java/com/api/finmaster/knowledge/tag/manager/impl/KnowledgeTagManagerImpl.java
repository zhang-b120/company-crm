package com.qykj.finmaster.knowledge.tag.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.tag.entity.KnowledgeTag;
import com.qykj.finmaster.knowledge.tag.manager.KnowledgeTagManager;
import com.qykj.finmaster.knowledge.tag.mapper.KnowledgeTagMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeTagManagerImpl extends ManagerImpl<KnowledgeTagMapper, KnowledgeTag>
        implements KnowledgeTagManager {
}
