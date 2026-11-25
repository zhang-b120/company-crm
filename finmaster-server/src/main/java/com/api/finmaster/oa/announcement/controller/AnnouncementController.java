package com.qykj.finmaster.oa.announcement.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.announcement.domain.OaAnnouncement;
import com.qykj.finmaster.oa.announcement.domain.req.AddAnnouncementReq;
import com.qykj.finmaster.oa.announcement.domain.req.GetAnnouncementListReq;
import com.qykj.finmaster.oa.announcement.service.AnnouncementServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公告通知Controller
 */
@RestController
@RequestMapping("/oa/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementServiceImpl announcementService;

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    private Long getTenantId() {
        return getLoginUser().getTenantId();
    }

    /**
     * 查询公告列表
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetAnnouncementListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaAnnouncement> list = announcementService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 获取公告详情
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Map<String, Object> detail = announcementService.getDetail(id, tenantId, loginUser);
        return AjaxResult.success(detail);
    }

    /**
     * 新增公告
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddAnnouncementReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = announcementService.addAnnouncement(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 修改公告
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody AddAnnouncementReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        announcementService.updateAnnouncement(req, tenantId, loginUser);
        return AjaxResult.success();
    }

    /**
     * 删除公告
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return AjaxResult.success();
    }

    /**
     * 发布公告
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:publish')")
    @PostMapping("/publish/{id}")
    public AjaxResult publish(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        announcementService.publishAnnouncement(id, loginUser);
        return AjaxResult.success();
    }

    /**
     * 归档公告
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:edit')")
    @PostMapping("/archive/{id}")
    public AjaxResult archive(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        announcementService.archiveAnnouncement(id, loginUser);
        return AjaxResult.success();
    }

    /**
     * 获取未读公告数
     */
    @PreAuthorize("@ss.hasPermi('oa:announcement:list')")
    @GetMapping("/unread-count")
    public AjaxResult unreadCount() {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        long count = announcementService.getUnreadCount(tenantId, loginUser);
        return AjaxResult.success(count);
    }
}
