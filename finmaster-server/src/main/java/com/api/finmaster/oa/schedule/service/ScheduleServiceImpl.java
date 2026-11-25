package com.qykj.finmaster.oa.schedule.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.schedule.domain.OaMeetingRoom;
import com.qykj.finmaster.oa.schedule.domain.OaSchedule;
import com.qykj.finmaster.oa.schedule.domain.OaScheduleParticipant;
import com.qykj.finmaster.oa.schedule.domain.req.AddScheduleReq;
import com.qykj.finmaster.oa.schedule.domain.req.GetScheduleListReq;
import com.qykj.finmaster.oa.schedule.manager.impl.OaMeetingRoomManagerImpl;
import com.qykj.finmaster.oa.schedule.manager.impl.OaScheduleManagerImpl;
import com.qykj.finmaster.oa.schedule.manager.impl.OaScheduleParticipantManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日程服务实现类
 */
@Slf4j
@Service
public class ScheduleServiceImpl {

    @Autowired
    private OaScheduleManagerImpl scheduleManager;

    @Autowired
    private OaScheduleParticipantManagerImpl participantManager;

    @Autowired
    private OaMeetingRoomManagerImpl meetingRoomManager;

    @Autowired
    private UserMapper userMapper;

    /**
     * 新增日程
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addSchedule(AddScheduleReq req, Long tenantId, LoginUser loginUser) {
        // 会议类型且指定了会议室，检查冲突
        if (req.getMeetingRoomId() != null) {
            checkMeetingRoomConflict(req.getMeetingRoomId(), req.getStartTime(), req.getEndTime(), null);
        }

        OaSchedule schedule = new OaSchedule();
        schedule.setTenantId(tenantId);
        schedule.setUserId(loginUser.getUserId());
        schedule.setUsername(loginUser.getUsername());
        schedule.setRealName(loginUser.getRealName());
        schedule.setTitle(req.getTitle());
        schedule.setDescription(req.getDescription());
        schedule.setLocation(req.getLocation());
        schedule.setStartTime(req.getStartTime());
        schedule.setEndTime(req.getEndTime());
        schedule.setAllDay(req.getAllDay() != null ? req.getAllDay() : 0);
        schedule.setScheduleType(req.getScheduleType());
        schedule.setColor(req.getColor());
        schedule.setReminderMinutes(req.getReminderMinutes());
        schedule.setReminderSent(0);
        schedule.setMeetingRoomId(req.getMeetingRoomId());
        schedule.setCreateBy(loginUser.getUsername());
        schedule.setCreateByName(loginUser.getRealName());
        schedule.setCreateTime(new Date());
        scheduleManager.save(schedule);

        // 保存参与人
        saveParticipants(schedule.getId(), req.getParticipantUserIds());

        log.info("新增日程成功，ID: {}，标题: {}，创建人: {}", schedule.getId(), req.getTitle(), loginUser.getUsername());
        return schedule.getId();
    }

    /**
     * 修改日程
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSchedule(AddScheduleReq req, Long tenantId, LoginUser loginUser) {
        OaSchedule schedule = scheduleManager.getById(req.getId());
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }

        // 会议类型且指定了会议室，检查冲突（排除自身）
        if (req.getMeetingRoomId() != null) {
            checkMeetingRoomConflict(req.getMeetingRoomId(), req.getStartTime(), req.getEndTime(), req.getId());
        }

        schedule.setTitle(req.getTitle());
        schedule.setDescription(req.getDescription());
        schedule.setLocation(req.getLocation());
        schedule.setStartTime(req.getStartTime());
        schedule.setEndTime(req.getEndTime());
        schedule.setAllDay(req.getAllDay() != null ? req.getAllDay() : 0);
        schedule.setScheduleType(req.getScheduleType());
        schedule.setColor(req.getColor());
        schedule.setReminderMinutes(req.getReminderMinutes());
        schedule.setMeetingRoomId(req.getMeetingRoomId());
        schedule.setUpdateBy(loginUser.getUsername());
        schedule.setUpdateByName(loginUser.getRealName());
        schedule.setUpdateTime(new Date());
        scheduleManager.updateById(schedule);

        // 删除旧参与人，重新保存
        LambdaQueryWrapper<OaScheduleParticipant> delWrapper = Wrappers.lambdaQuery();
        delWrapper.eq(OaScheduleParticipant::getScheduleId, req.getId());
        participantManager.remove(delWrapper);

        saveParticipants(req.getId(), req.getParticipantUserIds());

        log.info("修改日程成功，ID: {}，操作人: {}", req.getId(), loginUser.getUsername());
    }

    /**
     * 删除日程
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSchedule(Long id) {
        OaSchedule schedule = scheduleManager.getById(id);
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }
        scheduleManager.removeById(id);

        // 删除参与人
        LambdaQueryWrapper<OaScheduleParticipant> delWrapper = Wrappers.lambdaQuery();
        delWrapper.eq(OaScheduleParticipant::getScheduleId, id);
        participantManager.remove(delWrapper);

        log.info("删除日程成功，ID: {}", id);
    }

    /**
     * 查询日程列表（当前用户）
     */
    public List<OaSchedule> getList(GetScheduleListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaSchedule> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaSchedule::getTenantId, tenantId);
        wrapper.eq(OaSchedule::getUserId, loginUser.getUserId());

        // 日期范围过滤
        applyDateRange(wrapper, req.getStartDate(), req.getEndDate());

        if (StringUtils.isNotEmpty(req.getScheduleType())) {
            wrapper.eq(OaSchedule::getScheduleType, req.getScheduleType());
        }

        wrapper.orderByAsc(OaSchedule::getStartTime);
        return scheduleManager.list(wrapper);
    }

    /**
     * 查询团队日程（同部门用户的日程）
     */
    public List<OaSchedule> getTeamSchedules(GetScheduleListReq req, Long tenantId, LoginUser loginUser) {
        // 获取当前用户所在部门
        List<Long> deptIds = userMapper.selectDepartmentIds(loginUser.getUserId());
        if (deptIds == null || deptIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取同部门的所有用户
        List<User> deptUsers = userMapper.getDepartmentUser(deptIds.get(0));
        if (deptUsers == null || deptUsers.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> userIds = deptUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // 如果指定了userId，只查该用户
        if (req.getUserId() != null) {
            if (!userIds.contains(req.getUserId())) {
                return new ArrayList<>();
            }
            userIds = List.of(req.getUserId());
        }

        LambdaQueryWrapper<OaSchedule> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaSchedule::getTenantId, tenantId);
        wrapper.in(OaSchedule::getUserId, userIds);

        // 日期范围过滤
        applyDateRange(wrapper, req.getStartDate(), req.getEndDate());

        if (StringUtils.isNotEmpty(req.getScheduleType())) {
            wrapper.eq(OaSchedule::getScheduleType, req.getScheduleType());
        }

        wrapper.orderByAsc(OaSchedule::getStartTime);
        return scheduleManager.list(wrapper);
    }

    /**
     * 获取日程详情（含参与人）
     */
    public Map<String, Object> getDetail(Long id) {
        OaSchedule schedule = scheduleManager.getById(id);
        if (schedule == null) {
            throw new BusinessException("日程不存在");
        }

        LambdaQueryWrapper<OaScheduleParticipant> pWrapper = Wrappers.lambdaQuery();
        pWrapper.eq(OaScheduleParticipant::getScheduleId, id);
        pWrapper.orderByAsc(OaScheduleParticipant::getId);
        List<OaScheduleParticipant> participants = participantManager.list(pWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("schedule", schedule);
        result.put("participants", participants);
        return result;
    }

    /**
     * 检查会议室时间冲突
     */
    private void checkMeetingRoomConflict(Long meetingRoomId, Date startTime, Date endTime, Long excludeScheduleId) {
        LambdaQueryWrapper<OaSchedule> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaSchedule::getMeetingRoomId, meetingRoomId);
        wrapper.lt(OaSchedule::getStartTime, endTime);
        wrapper.gt(OaSchedule::getEndTime, startTime);
        if (excludeScheduleId != null) {
            wrapper.ne(OaSchedule::getId, excludeScheduleId);
        }

        long count = scheduleManager.count(wrapper);
        if (count > 0) {
            // 获取会议室名称
            OaMeetingRoom room = meetingRoomManager.getById(meetingRoomId);
            String roomName = room != null ? room.getName() : "ID:" + meetingRoomId;
            throw new BusinessException("会议室「" + roomName + "」在该时间段已被预订，请选择其他时间或会议室");
        }
    }

    /**
     * 保存参与人
     */
    private void saveParticipants(Long scheduleId, List<Long> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return;
        }

        List<OaScheduleParticipant> participants = new ArrayList<>();
        for (Long userId : participantUserIds) {
            User user = userMapper.selectUserById(userId);
            if (user == null) {
                continue;
            }
            OaScheduleParticipant participant = new OaScheduleParticipant();
            participant.setScheduleId(scheduleId);
            participant.setUserId(userId);
            participant.setUsername(user.getUsername());
            participant.setRealName(user.getRealName());
            participant.setStatus("PENDING");
            participants.add(participant);
        }
        if (!participants.isEmpty()) {
            participantManager.saveBatch(participants);
        }
    }

    /**
     * 应用日期范围过滤
     */
    private void applyDateRange(LambdaQueryWrapper<OaSchedule> wrapper, String startDate, String endDate) {
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
                // 结束日期包含当天，加一天
                Date end = sdf.parse(endDate);
                end = new Date(end.getTime() + 24 * 60 * 60 * 1000);
                wrapper.le(OaSchedule::getEndTime, end);
            } catch (ParseException e) {
                log.warn("日期格式错误: {}", endDate);
            }
        }
    }
}
