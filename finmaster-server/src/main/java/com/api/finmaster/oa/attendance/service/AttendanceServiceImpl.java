package com.qykj.finmaster.oa.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.attendance.domain.OaAttendance;
import com.qykj.finmaster.oa.attendance.domain.req.AddAttendanceReq;
import com.qykj.finmaster.oa.attendance.domain.req.GetAttendanceListReq;
import com.qykj.finmaster.oa.attendance.manager.impl.OaAttendanceManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考勤服务实现类
 */
@Slf4j
@Service
public class AttendanceServiceImpl {

    @Autowired
    private OaAttendanceManagerImpl attendanceManager;

    /**
     * 新增考勤记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addAttendance(AddAttendanceReq req, Long tenantId, LoginUser loginUser) {
        OaAttendance attendance = new OaAttendance();
        attendance.setTenantId(tenantId);
        attendance.setUserId(req.getUserId());
        attendance.setUsername(req.getUsername());
        attendance.setRealName(req.getRealName());
        if (req.getAttendanceDate() != null) {
            attendance.setAttendanceDate(req.getAttendanceDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }
        attendance.setCheckInTime(req.getCheckInTime());
        attendance.setCheckOutTime(req.getCheckOutTime());
        attendance.setStatus(req.getStatus());
        attendance.setCreateBy(loginUser.getUsername());
        attendance.setCreateByName(loginUser.getRealName());
        attendance.setCreateTime(new Date());
        attendanceManager.save(attendance);

        log.info("新增考勤记录成功，ID: {}，用户: {}，日期: {}", attendance.getId(), req.getUsername(), req.getAttendanceDate());
        return attendance.getId();
    }

    /**
     * 更新考勤记录
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAttendance(Long id, AddAttendanceReq req, LoginUser loginUser) {
        OaAttendance attendance = attendanceManager.getById(id);
        if (attendance == null) {
            throw new BusinessException("考勤记录不存在");
        }

        attendance.setUserId(req.getUserId());
        attendance.setUsername(req.getUsername());
        attendance.setRealName(req.getRealName());
        if (req.getAttendanceDate() != null) {
            attendance.setAttendanceDate(req.getAttendanceDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate());
        }
        attendance.setCheckInTime(req.getCheckInTime());
        attendance.setCheckOutTime(req.getCheckOutTime());
        attendance.setStatus(req.getStatus());
        attendance.setUpdateBy(loginUser.getUsername());
        attendance.setUpdateByName(loginUser.getRealName());
        attendance.setUpdateTime(new Date());
        attendanceManager.updateById(attendance);

        log.info("更新考勤记录成功，ID: {}", id);
        return true;
    }

    /**
     * 查询考勤列表
     */
    public List<OaAttendance> getList(GetAttendanceListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaAttendance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaAttendance::getTenantId, tenantId);

        // 非管理员只能看自己的
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaAttendance::getUsername, loginUser.getUsername());
        } else if (StringUtils.isNotEmpty(req.getUsername())) {
            wrapper.eq(OaAttendance::getUsername, req.getUsername());
        }

        if (req.getAttendanceDateStart() != null) {
            LocalDate startDate = req.getAttendanceDateStart().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            wrapper.ge(OaAttendance::getAttendanceDate, startDate);
        }
        if (req.getAttendanceDateEnd() != null) {
            LocalDate endDate = req.getAttendanceDateEnd().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();
            wrapper.le(OaAttendance::getAttendanceDate, endDate);
        }
        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaAttendance::getStatus, req.getStatus());
        }

        wrapper.orderByDesc(OaAttendance::getAttendanceDate);
        return attendanceManager.list(wrapper);
    }

    /**
     * 获取月度考勤汇总
     *
     * @param year     年份
     * @param month    月份
     * @param tenantId 租户ID
     * @return 按用户汇总的考勤统计列表
     */
    public List<Map<String, Object>> getMonthlySummary(int year, int month, Long tenantId) {
        // 计算月份的开始和结束日期
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        // 查询该月所有考勤记录
        LambdaQueryWrapper<OaAttendance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaAttendance::getTenantId, tenantId);
        wrapper.ge(OaAttendance::getAttendanceDate, monthStart);
        wrapper.le(OaAttendance::getAttendanceDate, monthEnd);
        List<OaAttendance> allRecords = attendanceManager.list(wrapper);

        // 按用户分组汇总
        Map<String, List<OaAttendance>> groupedByUser = allRecords.stream()
                .collect(Collectors.groupingBy(OaAttendance::getUsername));

        return groupedByUser.entrySet().stream().map(entry -> {
            String username = entry.getKey();
            List<OaAttendance> userRecords = entry.getValue();

            Map<String, Object> summary = new HashMap<>();
            summary.put("username", username);
            summary.put("realName", userRecords.get(0).getRealName());
            summary.put("totalDays", userRecords.size());

            long normalCount = userRecords.stream()
                    .filter(a -> "NORMAL".equals(a.getStatus())).count();
            long lateCount = userRecords.stream()
                    .filter(a -> "LATE".equals(a.getStatus())).count();
            long earlyCount = userRecords.stream()
                    .filter(a -> "EARLY".equals(a.getStatus())).count();
            long absentCount = userRecords.stream()
                    .filter(a -> "ABSENT".equals(a.getStatus())).count();

            summary.put("normalCount", normalCount);
            summary.put("lateCount", lateCount);
            summary.put("earlyCount", earlyCount);
            summary.put("absentCount", absentCount);

            return summary;
        }).collect(Collectors.toList());
    }
}
