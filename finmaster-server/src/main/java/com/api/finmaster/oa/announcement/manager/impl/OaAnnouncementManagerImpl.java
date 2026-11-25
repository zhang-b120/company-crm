package com.qykj.finmaster.oa.announcement.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.announcement.domain.OaAnnouncement;
import com.qykj.finmaster.oa.announcement.manager.OaAnnouncementManager;
import com.qykj.finmaster.oa.announcement.mapper.OaAnnouncementMapper;
import org.springframework.stereotype.Service;

/**
 * 公告通知Manager实现
 */
@Service
public class OaAnnouncementManagerImpl extends ManagerImpl<OaAnnouncementMapper, OaAnnouncement> implements OaAnnouncementManager {
}
