package com.qykj.finmaster.oa.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.schedule.domain.OaMeetingRoom;
import com.qykj.finmaster.oa.schedule.domain.OaSchedule;
import com.qykj.finmaster.oa.schedule.domain.req.AddMeetingRoomReq;
import com.qykj.finmaster.oa.schedule.manager.impl.OaMeetingRoomManagerImpl;
import com.qykj.finmaster.oa.schedule.manager.impl.OaScheduleManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 会议室服务实现类
 */
@Slf4j
@Service
public class MeetingRoomServiceImpl {

    @Autowired
    private OaMeetingRoomManagerImpl meetingRoomManager;

    @Autowired
    private OaScheduleManagerImpl scheduleManager;

    /**
     * 新增会议室
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addRoom(AddMeetingRoomReq req, Long tenantId) {
        OaMeetingRoom room = new OaMeetingRoom();
        room.setTenantId(tenantId);
        room.setName(req.getName());
        room.setLocation(req.getLocation());
        room.setCapacity(req.getCapacity());
        room.setFacilities(req.getFacilities());
        room.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
        room.setCreateTime(new Date());
        room.setUpdateTime(new Date());
        meetingRoomManager.save(room);

        log.info("新增会议室成功，ID: {}，名称: {}", room.getId(), req.getName());
        return room.getId();
    }

    /**
     * 修改会议室
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateRoom(AddMeetingRoomReq req) {
        OaMeetingRoom room = meetingRoomManager.getById(req.getId());
        if (room == null) {
            throw new BusinessException("会议室不存在");
        }

        room.setName(req.getName());
        room.setLocation(req.getLocation());
        room.setCapacity(req.getCapacity());
        room.setFacilities(req.getFacilities());
        if (req.getEnabled() != null) {
            room.setEnabled(req.getEnabled());
        }
        room.setUpdateTime(new Date());
        meetingRoomManager.updateById(room);

        log.info("修改会议室成功，ID: {}", req.getId());
    }

    /**
     * 删除会议室
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoom(Long id) {
        OaMeetingRoom room = meetingRoomManager.getById(id);
        if (room == null) {
            throw new BusinessException("会议室不存在");
        }
        meetingRoomManager.removeById(id);

        log.info("删除会议室成功，ID: {}", id);
    }

    /**
     * 查询会议室列表
     */
    public List<OaMeetingRoom> getList(Long tenantId) {
        LambdaQueryWrapper<OaMeetingRoom> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaMeetingRoom::getTenantId, tenantId);
        wrapper.orderByAsc(OaMeetingRoom::getId);
        return meetingRoomManager.list(wrapper);
    }

    /**
     * 查询会议室预订情况
     */
    public List<OaSchedule> getBookings(Long roomId, String startDate, String endDate) {
        LambdaQueryWrapper<OaSchedule> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaSchedule::getMeetingRoomId, roomId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        if (StringUtils.isNotEmpty(startDate)) {
            try {
                Date start = sdf.parse(startDate);
                wrapper.ge(OaSchedule::getStartTime, start);
            } catch (ParseException e) {
                log.warn("日期格式错误: {}", startDate);
            }
        }
        if (StringUtils.isNotEmpty(endDate)) {
            try {
                Date end = sdf.parse(endDate);
                end = new Date(end.getTime() + 24 * 60 * 60 * 1000);
                wrapper.le(OaSchedule::getEndTime, end);
            } catch (ParseException e) {
                log.warn("日期格式错误: {}", endDate);
            }
        }

        wrapper.orderByAsc(OaSchedule::getStartTime);
        return scheduleManager.list(wrapper);
    }
}
