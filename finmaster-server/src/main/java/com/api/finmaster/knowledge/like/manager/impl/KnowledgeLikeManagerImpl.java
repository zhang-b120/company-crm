package com.qykj.finmaster.knowledge.like.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeLike;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeLikeManager;
import com.qykj.finmaster.knowledge.like.mapper.KnowledgeLikeMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeLikeManagerImpl extends ManagerImpl<KnowledgeLikeMapper, KnowledgeLike>
        implements KnowledgeLikeManager {
}
