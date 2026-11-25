package com.qykj.finmaster.oa.announcement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.announcement.domain.OaAnnouncement;
import com.qykj.finmaster.oa.announcement.domain.OaAnnouncementRead;
import com.qykj.finmaster.oa.announcement.domain.req.AddAnnouncementReq;
import com.qykj.finmaster.oa.announcement.domain.req.GetAnnouncementListReq;
import com.qykj.finmaster.oa.announcement.manager.impl.OaAnnouncementManagerImpl;
import com.qykj.finmaster.oa.announcement.manager.impl.OaAnnouncementReadManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 公告通知服务实现类
 */
@Slf4j
@Service
public class AnnouncementServiceImpl {

    @Autowired
    private OaAnnouncementManagerImpl announcementManager;

    @Autowired
    private OaAnnouncementReadManagerImpl announcementReadManager;

    /**
     * 新增公告
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addAnnouncement(AddAnnouncementReq req, Long tenantId, LoginUser loginUser) {
        OaAnnouncement announcement = new OaAnnouncement();
        announcement.setTenantId(tenantId);
        announcement.setTitle(req.getTitle());
        announcement.setContent(req.getContent());
        announcement.setCategory(req.getCategory());
        announcement.setPriority(req.getPriority() != null ? req.getPriority() : "NORMAL");
        announcement.setPinned(req.getPinned() != null ? req.getPinned() : 0);
        announcement.setAttachments(req.getAttachments());
        announcement.setStatus("DRAFT");
        announcement.setCreateBy(loginUser.getUsername());
        announcement.setCreateByName(loginUser.getRealName());
        announcement.setCreateTime(new Date());
        announcementManager.save(announcement);

        log.info("新增公告成功，ID: {}，标题: {}", announcement.getId(), req.getTitle());
        return announcement.getId();
    }

    /**
     * 修改公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(AddAnnouncementReq req, Long tenantId, LoginUser loginUser) {
        OaAnnouncement announcement = announcementManager.getById(req.getId());
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (!"DRAFT".equals(announcement.getStatus())) {
            throw new BusinessException("只有草稿状态的公告才能编辑");
        }
        announcement.setTitle(req.getTitle());
        announcement.setContent(req.getContent());
        announcement.setCategory(req.getCategory());
        announcement.setPriority(req.getPriority());
        announcement.setPinned(req.getPinned());
        announcement.setAttachments(req.getAttachments());
        announcement.setUpdateBy(loginUser.getUsername());
        announcement.setUpdateByName(loginUser.getRealName());
        announcement.setUpdateTime(new Date());
        announcementManager.updateById(announcement);
    }

    /**
     * 删除公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        announcementManager.removeById(id);
    }

    /**
     * 发布公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(Long id, LoginUser loginUser) {
        OaAnnouncement announcement = announcementManager.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (!"DRAFT".equals(announcement.getStatus())) {
            throw new BusinessException("只有草稿状态的公告才能发布");
        }
        announcement.setStatus("PUBLISHED");
        announcement.setPublishTime(new Date());
        announcement.setPublisher(loginUser.getUsername());
        announcement.setPublisherName(loginUser.getRealName());
        announcement.setUpdateBy(loginUser.getUsername());
        announcement.setUpdateByName(loginUser.getRealName());
        announcement.setUpdateTime(new Date());
        announcementManager.updateById(announcement);

        log.info("公告发布成功，ID: {}，发布人: {}", id, loginUser.getUsername());
    }

    /**
     * 归档公告
     */
    @Transactional(rollbackFor = Exception.class)
    public void archiveAnnouncement(Long id, LoginUser loginUser) {
        OaAnnouncement announcement = announcementManager.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        if (!"PUBLISHED".equals(announcement.getStatus())) {
            throw new BusinessException("只有已发布的公告才能归档");
        }
        announcement.setStatus("ARCHIVED");
        announcement.setUpdateBy(loginUser.getUsername());
        announcement.setUpdateByName(loginUser.getRealName());
        announcement.setUpdateTime(new Date());
        announcementManager.updateById(announcement);
    }

    /**
     * 查询公告列表
     */
    public List<OaAnnouncement> getList(GetAnnouncementListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaAnnouncement> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaAnnouncement::getTenantId, tenantId);

        // 非管理员只能看到已发布的公告
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaAnnouncement::getStatus, "PUBLISHED");
        } else if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaAnnouncement::getStatus, req.getStatus());
        }

        if (StringUtils.isNotEmpty(req.getTitle())) {
            wrapper.like(OaAnnouncement::getTitle, req.getTitle());
        }
        if (StringUtils.isNotEmpty(req.getCategory())) {
            wrapper.eq(OaAnnouncement::getCategory, req.getCategory());
        }
        if (StringUtils.isNotEmpty(req.getPriority())) {
            wrapper.eq(OaAnnouncement::getPriority, req.getPriority());
        }

        // 置顶优先，然后按发布时间倒序
        wrapper.orderByDesc(OaAnnouncement::getPinned);
        wrapper.orderByDesc(OaAnnouncement::getPublishTime);
        wrapper.orderByDesc(OaAnnouncement::getCreateTime);
        return announcementManager.list(wrapper);
    }

    /**
     * 获取公告详情（自动标记已读）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> getDetail(Long id, Long tenantId, LoginUser loginUser) {
        OaAnnouncement announcement = announcementManager.getById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }

        // 自动标记已读
        markAsRead(id, tenantId, loginUser);

        // 查询已读数
        long readCount = announcementReadManager.count(
                Wrappers.lambdaQuery(OaAnnouncementRead.class)
                        .eq(OaAnnouncementRead::getAnnouncementId, id));

        Map<String, Object> result = new HashMap<>();
        result.put("announcement", announcement);
        result.put("readCount", readCount);
        return result;
    }

    /**
     * 标记已读
     */
    private void markAsRead(Long announcementId, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaAnnouncementRead> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaAnnouncementRead::getAnnouncementId, announcementId);
        wrapper.eq(OaAnnouncementRead::getUserId, loginUser.getUserId());
        OaAnnouncementRead existing = announcementReadManager.getOne(wrapper);

        if (existing == null) {
            OaAnnouncementRead read = new OaAnnouncementRead();
            read.setTenantId(tenantId);
            read.setAnnouncementId(announcementId);
            read.setUserId(loginUser.getUserId());
            read.setUsername(loginUser.getUsername());
            read.setRealName(loginUser.getRealName());
            read.setReadTime(new Date());
            announcementReadManager.save(read);
        }
    }

    /**
     * 获取当前用户未读公告数
     */
    public long getUnreadCount(Long tenantId, LoginUser loginUser) {
        // 已发布的公告总数
        long publishedCount = announcementManager.count(
                Wrappers.lambdaQuery(OaAnnouncement.class)
                        .eq(OaAnnouncement::getTenantId, tenantId)
                        .eq(OaAnnouncement::getStatus, "PUBLISHED"));

        // 当前用户已读数
        long readCount = announcementReadManager.count(
                Wrappers.lambdaQuery(OaAnnouncementRead.class)
                        .eq(OaAnnouncementRead::getTenantId, tenantId)
                        .eq(OaAnnouncementRead::getUserId, loginUser.getUserId()));

        return Math.max(0, publishedCount - readCount);
    }
}
