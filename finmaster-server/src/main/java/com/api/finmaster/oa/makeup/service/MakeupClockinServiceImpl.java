package com.qykj.finmaster.oa.makeup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.oa.attendance.domain.OaAttendance;
import com.qykj.finmaster.oa.attendance.manager.impl.OaAttendanceManagerImpl;
import com.qykj.finmaster.oa.makeup.domain.OaMakeupClockin;
import com.qykj.finmaster.oa.makeup.domain.req.AddMakeupClockinReq;
import com.qykj.finmaster.oa.makeup.domain.req.GetMakeupClockinListReq;
import com.qykj.finmaster.oa.makeup.manager.impl.OaMakeupClockinManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 补卡服务实现类
 */
@Slf4j
@Service
public class MakeupClockinServiceImpl {

    @Autowired
    private OaMakeupClockinManagerImpl makeupManager;

    @Lazy
    @Autowired
    private OaAttendanceManagerImpl attendanceManager;

    @Autowired
    private ApprovalServiceImpl approvalService;

    /**
     * 新增补卡申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addMakeup(AddMakeupClockinReq req, Long tenantId, LoginUser loginUser) {
        OaMakeupClockin makeup = new OaMakeupClockin();
        makeup.setTenantId(tenantId);
        makeup.setApplicant(loginUser.getUsername());
        makeup.setApplicantName(loginUser.getRealName());
        makeup.setUserId(loginUser.getUserId());
        makeup.setAttendanceDate(parseDate(req.getAttendanceDate(), "yyyy-MM-dd"));
        makeup.setClockinType(req.getClockinType());
        makeup.setClockinTime(parseDate(req.getClockinTime(), "yyyy-MM-dd HH:mm:ss"));
        makeup.setReason(req.getReason());
        makeup.setStatus("DRAFT");
        makeup.setCreateBy(loginUser.getUsername());
        makeup.setCreateByName(loginUser.getRealName());
        makeup.setCreateTime(new Date());
        makeupManager.save(makeup);

        log.info("新增补卡申请成功，ID: {}，申请人: {}，考勤日期: {}，类型: {}",
                makeup.getId(), loginUser.getUsername(), req.getAttendanceDate(), req.getClockinType());
        return makeup.getId();
    }

    /**
     * 提交补卡申请（发起审批）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitMakeup(Long id, Long tenantId, LoginUser loginUser) {
        OaMakeupClockin makeup = makeupManager.getById(id);
        if (makeup == null) {
            throw new BusinessException("补卡申请不存在");
        }
        if (!"DRAFT".equals(makeup.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能提交");
        }

        // 更新状态为PENDING
        makeup.setStatus("PENDING");
        makeup.setUpdateBy(loginUser.getUsername());
        makeup.setUpdateByName(loginUser.getRealName());
        makeup.setUpdateTime(new Date());

        // 提交审批
        String typeDesc = "CHECK_IN".equals(makeup.getClockinType()) ? "签到" : "签退";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String title = loginUser.getRealName() + "的补卡申请（" + sdf.format(makeup.getAttendanceDate()) + " " + typeDesc + "）";
        Long instanceId = approvalService.submit("MAKEUP_CLOCKIN", makeup.getId(), title, tenantId, loginUser);

        makeup.setApprovalInstanceId(instanceId);
        makeupManager.updateById(makeup);

        log.info("补卡申请提交审批成功，补卡ID: {}，审批实例ID: {}", id, instanceId);
        return instanceId;
    }

    /**
     * 更新补卡状态（由审批引擎回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMakeupClockinStatus(Long makeupId, String status) {
        OaMakeupClockin makeup = makeupManager.getById(makeupId);
        if (makeup == null) {
            throw new BusinessException("补卡申请不存在");
        }
        makeup.setStatus(status);
        makeup.setUpdateTime(new Date());

        // 审批通过时，更新考勤记录
        if ("APPROVED".equals(status)) {
            handleApproved(makeup);
        }

        makeupManager.updateById(makeup);
        log.info("补卡申请状态更新，ID: {}，新状态: {}", makeupId, status);
    }

    /**
     * 审批通过处理：查找或创建考勤记录，更新打卡时间
     */
    private void handleApproved(OaMakeupClockin makeup) {
        LocalDate attendanceDate = makeup.getAttendanceDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();

        // 查找当天该用户的考勤记录
        LambdaQueryWrapper<OaAttendance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaAttendance::getUserId, makeup.getUserId());
        wrapper.eq(OaAttendance::getAttendanceDate, attendanceDate);
        wrapper.last("LIMIT 1");
        OaAttendance attendance = attendanceManager.getOne(wrapper);

        if (attendance == null) {
            // 不存在则创建新的考勤记录
            attendance = new OaAttendance();
            attendance.setTenantId(makeup.getTenantId());
            attendance.setUserId(makeup.getUserId());
            attendance.setUsername(makeup.getApplicant());
            attendance.setRealName(makeup.getApplicantName());
            attendance.setAttendanceDate(attendanceDate);
            attendance.setStatus("ABSENT");
            attendance.setCreateBy(makeup.getApplicant());
            attendance.setCreateByName(makeup.getApplicantName());
            attendance.setCreateTime(new Date());
        }

        // 根据打卡类型更新签到或签退时间
        if ("CHECK_IN".equals(makeup.getClockinType())) {
            attendance.setCheckInTime(makeup.getClockinTime());
        } else if ("CHECK_OUT".equals(makeup.getClockinType())) {
            attendance.setCheckOutTime(makeup.getClockinTime());
        }

        // 如果签到和签退时间都存在，标记为正常
        if (attendance.getCheckInTime() != null && attendance.getCheckOutTime() != null) {
            attendance.setStatus("NORMAL");
        }

        attendance.setUpdateTime(new Date());

        if (attendance.getId() == null) {
            attendanceManager.save(attendance);
        } else {
            attendanceManager.updateById(attendance);
        }

        // 回写考勤记录ID到补卡申请
        makeup.setAttendanceId(attendance.getId());

        log.info("补卡审批通过，已更新考勤记录，考勤ID: {}，补卡ID: {}，类型: {}",
                attendance.getId(), makeup.getId(), makeup.getClockinType());
    }

    /**
     * 查询补卡列表
     */
    public List<OaMakeupClockin> getList(GetMakeupClockinListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaMakeupClockin> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaMakeupClockin::getTenantId, tenantId);

        // 非管理员只能看自己的
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaMakeupClockin::getApplicant, loginUser.getUsername());
        }

        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaMakeupClockin::getStatus, req.getStatus());
        }

        if (StringUtils.isNotEmpty(req.getAttendanceDate())) {
            LocalDate date = LocalDate.parse(req.getAttendanceDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            wrapper.eq(OaMakeupClockin::getAttendanceDate, Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }

        wrapper.orderByDesc(OaMakeupClockin::getCreateTime);
        return makeupManager.list(wrapper);
    }

    /**
     * 获取补卡详情
     */
    public OaMakeupClockin getDetail(Long id) {
        OaMakeupClockin makeup = makeupManager.getById(id);
        if (makeup == null) {
            throw new BusinessException("补卡申请不存在");
        }
        return makeup;
    }

    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr, String pattern) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            throw new BusinessException("日期格式错误: " + dateStr);
        }
    }
}
