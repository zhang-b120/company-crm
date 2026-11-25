package com.qykj.finmaster.knowledge.comment.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.comment.entity.KnowledgeComment;
import com.qykj.finmaster.knowledge.comment.manager.KnowledgeCommentManager;
import com.qykj.finmaster.knowledge.comment.mapper.KnowledgeCommentMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeCommentManagerImpl extends ManagerImpl<KnowledgeCommentMapper, KnowledgeComment>
        implements KnowledgeCommentManager {
}
