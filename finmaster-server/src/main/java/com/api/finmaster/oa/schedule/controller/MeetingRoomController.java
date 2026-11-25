package com.qykj.finmaster.oa.schedule.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.schedule.domain.OaMeetingRoom;
import com.qykj.finmaster.oa.schedule.domain.OaSchedule;
import com.qykj.finmaster.oa.schedule.domain.req.AddMeetingRoomReq;
import com.qykj.finmaster.oa.schedule.service.MeetingRoomServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 会议室管理Controller
 */
@RestController
@RequestMapping("/oa/meeting-room")
public class MeetingRoomController {

    @Autowired
    private MeetingRoomServiceImpl meetingRoomService;

    /**
     * 获取当前登录用户
     */
    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 获取租户ID
     */
    private Long getTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser.getTenantId();
    }

    /**
     * 查询会议室列表
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingroom:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        Long tenantId = getTenantId();
        List<OaMeetingRoom> rooms = meetingRoomService.getList(tenantId);
        return AjaxResult.success(rooms);
    }

    /**
     * 新增会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingroom:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddMeetingRoomReq req) {
        Long tenantId = getTenantId();
        Long id = meetingRoomService.addRoom(req, tenantId);
        return AjaxResult.success(id);
    }

    /**
     * 修改会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingroom:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody AddMeetingRoomReq req) {
        meetingRoomService.updateRoom(req);
        return AjaxResult.success();
    }

    /**
     * 删除会议室
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingroom:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        meetingRoomService.deleteRoom(id);
        return AjaxResult.success();
    }

    /**
     * 查询会议室预订情况
     */
    @PreAuthorize("@ss.hasPermi('oa:meetingroom:query')")
    @GetMapping("/{id}/bookings")
    public AjaxResult bookings(@PathVariable Long id,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        List<OaSchedule> bookings = meetingRoomService.getBookings(id, startDate, endDate);
        return AjaxResult.success(bookings);
    }
}
