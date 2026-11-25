package com.qykj.finmaster.knowledge.attachment.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.knowledge.attachment.entity.KnowledgeAttachment;
import com.qykj.finmaster.knowledge.attachment.manager.KnowledgeAttachmentManager;
import com.qykj.finmaster.knowledge.attachment.mapper.KnowledgeAttachmentMapper;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeAttachmentManagerImpl extends ManagerImpl<KnowledgeAttachmentMapper, KnowledgeAttachment>
        implements KnowledgeAttachmentManager {
}
