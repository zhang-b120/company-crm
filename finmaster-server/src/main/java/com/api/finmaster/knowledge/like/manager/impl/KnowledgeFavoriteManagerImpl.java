package com.qykj.finmaster.knowledge.like.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeFavorite;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeFavoriteManager;
import com.qykj.finmaster.knowledge.like.mapper.KnowledgeFavoriteMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeFavoriteManagerImpl extends ManagerImpl<KnowledgeFavoriteMapper, KnowledgeFavorite>
        implements KnowledgeFavoriteManager {
}
