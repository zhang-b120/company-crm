package com.qykj.finmaster.oa.schedule.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.schedule.domain.OaMeetingRoom;
import com.qykj.finmaster.oa.schedule.manager.OaMeetingRoomManager;
import com.qykj.finmaster.oa.schedule.mapper.OaMeetingRoomMapper;
import org.springframework.stereotype.Service;

/**
 * 会议室Manager实现
 */
@Service
public class OaMeetingRoomManagerImpl extends ManagerImpl<OaMeetingRoomMapper, OaMeetingRoom> implements OaMeetingRoomManager {
}
