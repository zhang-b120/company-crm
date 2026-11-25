package com.qykj.finmaster.oa.announcement.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.announcement.domain.OaAnnouncementRead;
import com.qykj.finmaster.oa.announcement.manager.OaAnnouncementReadManager;
import com.qykj.finmaster.oa.announcement.mapper.OaAnnouncementReadMapper;
import org.springframework.stereotype.Service;

/**
 * 公告阅读记录Manager实现
 */
@Service
public class OaAnnouncementReadManagerImpl extends ManagerImpl<OaAnnouncementReadMapper, OaAnnouncementRead> implements OaAnnouncementReadManager {
}
