package com.qykj.finmaster.knowledge.request.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.category.entity.KnowledgeCategory;
import com.qykj.finmaster.knowledge.category.manager.KnowledgeCategoryManager;
import com.qykj.finmaster.knowledge.request.domain.req.KnowledgeRequestSaveReq;
import com.qykj.finmaster.knowledge.request.entity.KnowledgeRequest;
import com.qykj.finmaster.knowledge.request.manager.KnowledgeRequestManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/knowledge/request")
public class KnowledgeRequestController extends BaseController {

    @Autowired
    private KnowledgeRequestManager requestManager;

    @Autowired
    private KnowledgeCategoryManager categoryManager;

    @Autowired
    private KnowledgeArticleManager articleManager;

    /**
     * 列表（管理端用，分页 + 筛选）
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Long requestUserId) {
        LambdaQueryWrapper<KnowledgeRequest> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) w.eq(KnowledgeRequest::getStatus, status);
        if (requestUserId != null) w.eq(KnowledgeRequest::getRequestUserId, requestUserId);
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            w.and(qw -> qw.like(KnowledgeRequest::getTitle, kw)
                    .or().like(KnowledgeRequest::getProblemDescription, kw));
        }
        // 待处理 → 进行中 → 完成 → 驳回；同状态按时间倒序
        w.orderByAsc(KnowledgeRequest::getStatus).orderByDesc(KnowledgeRequest::getCreateTime);

        Page<KnowledgeRequest> page = requestManager.page(new Page<>(pageNum, pageSize), w);
        fillExtras(page.getRecords());
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 我的需求列表
     */
    @GetMapping("/my")
    public Result<?> my(@RequestParam(defaultValue = "1") Integer pageNum,
                        @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginUser u = getLoginUser();
        Page<KnowledgeRequest> page = requestManager.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<KnowledgeRequest>()
                        .eq(KnowledgeRequest::getRequestUserId, u.getUserId())
                        .orderByDesc(KnowledgeRequest::getCreateTime));
        fillExtras(page.getRecords());
        return Result.ok(page.getRecords(), page.getTotal());
    }

    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        KnowledgeRequest r = requestManager.getById(id);
        if (r == null) return Result.error("需求不存在");
        fillExtras(Collections.singletonList(r));
        return Result.ok(r);
    }

    /**
     * 提交需求
     */
    @PostMapping
    public Result<?> add(@RequestBody KnowledgeRequestSaveReq req) {
        LoginUser u = getLoginUser();
        if (req.getTitle() == null || req.getTitle().isBlank()) return Result.error("请填写需求标题");

        KnowledgeRequest r = new KnowledgeRequest();
        r.setTitle(req.getTitle().trim());
        r.setProblemDescription(req.getProblemDescription());
        r.setExpectCategoryId(req.getExpectCategoryId());
        r.setExpectTags(joinTags(req.getExpectTags()));
        r.setStatus("PENDING");
        r.setRequestUserId(u.getUserId());
        r.setRequestUserName(u.getRealName());
        r.setTenantId(u.getTenantId());
        r.setCreateBy(u.getUsername());
        r.setCreateByName(u.getRealName());
        r.setCreateTime(new Date());
        requestManager.save(r);
        return Result.ok(r.getId());
    }

    /**
     * 修改需求（仅申请人本人在 PENDING 状态可改）
     */
    @PutMapping
    public Result<?> update(@RequestBody KnowledgeRequestSaveReq req) {
        LoginUser u = getLoginUser();
        KnowledgeRequest exists = requestManager.getById(req.getId());
        if (exists == null) return Result.error("需求不存在");
        if (!u.getUserId().equals(exists.getRequestUserId())) return Result.error("无权修改");
        if (!"PENDING".equals(exists.getStatus())) return Result.error("已被处理，不可修改");

        KnowledgeRequest update = new KnowledgeRequest();
        update.setId(req.getId());
        update.setTitle(req.getTitle());
        update.setProblemDescription(req.getProblemDescription());
        update.setExpectCategoryId(req.getExpectCategoryId());
        update.setExpectTags(joinTags(req.getExpectTags()));
        update.setUpdateBy(u.getUsername());
        update.setUpdateByName(u.getRealName());
        update.setUpdateTime(new Date());
        requestManager.updateById(update);
        return Result.ok();
    }

    /**
     * 更新状态（管理员处理）
     * body: { status, handleRemark }
     * status: IN_PROGRESS / DONE / REJECTED
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LoginUser u = getLoginUser();
        KnowledgeRequest exists = requestManager.getById(id);
        if (exists == null) return Result.error("需求不存在");
        String status = (String) body.get("status");
        if (status == null || status.isBlank()) return Result.error("请传入状态");

        requestManager.lambdaUpdate()
                .set(KnowledgeRequest::getStatus, status)
                .set(KnowledgeRequest::getHandlerId, u.getUserId())
                .set(KnowledgeRequest::getHandlerName, u.getRealName())
                .set(KnowledgeRequest::getHandleTime, new Date())
                .set(body.get("handleRemark") != null,
                     KnowledgeRequest::getHandleRemark, body.get("handleRemark"))
                .set(KnowledgeRequest::getUpdateTime, new Date())
                .eq(KnowledgeRequest::getId, id)
                .update();
        return Result.ok();
    }

    /**
     * 关联文章（撰写完文章后调用）：把需求标记为 DONE 并写入 linked_article_id
     */
    @PutMapping("/{id}/link")
    public Result<?> link(@PathVariable Long id, @RequestParam Long articleId) {
        LoginUser u = getLoginUser();
        KnowledgeRequest exists = requestManager.getById(id);
        if (exists == null) return Result.error("需求不存在");

        requestManager.lambdaUpdate()
                .set(KnowledgeRequest::getStatus, "DONE")
                .set(KnowledgeRequest::getLinkedArticleId, articleId)
                .set(KnowledgeRequest::getHandlerId, u.getUserId())
                .set(KnowledgeRequest::getHandlerName, u.getRealName())
                .set(KnowledgeRequest::getHandleTime, new Date())
                .set(KnowledgeRequest::getUpdateTime, new Date())
                .eq(KnowledgeRequest::getId, id)
                .update();
        return Result.ok();
    }

    /**
     * 删除（申请人本人 / 拥有 article:delete 权限的人）
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        LoginUser u = getLoginUser();
        KnowledgeRequest exists = requestManager.getById(id);
        if (exists == null) return Result.ok();
        boolean isAuthor = exists.getRequestUserId() != null
                && exists.getRequestUserId().equals(u.getUserId());
        boolean canManage = u.getPermissions() != null
                && (u.getPermissions().contains("knowledge:article:delete")
                || u.getPermissions().contains("knowledge:article:edit"));
        if (!isAuthor && !canManage) return Result.error("无权删除");
        requestManager.removeById(id);
        return Result.ok();
    }

    // ==== 辅助 ====

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return tags.stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isBlank()).collect(Collectors.joining(","));
    }

    private void fillExtras(List<KnowledgeRequest> list) {
        if (list == null || list.isEmpty()) return;
        // 分类名
        Set<Long> catIds = list.stream().map(KnowledgeRequest::getExpectCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catMap = Collections.emptyMap();
        if (!catIds.isEmpty()) {
            catMap = categoryManager.list(new LambdaQueryWrapper<KnowledgeCategory>()
                            .in(KnowledgeCategory::getId, catIds))
                    .stream().collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
        }
        // 关联文章名
        Set<Long> articleIds = list.stream().map(KnowledgeRequest::getLinkedArticleId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> articleMap = Collections.emptyMap();
        if (!articleIds.isEmpty()) {
            articleMap = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                            .select(KnowledgeArticle::getId, KnowledgeArticle::getTitle)
                            .in(KnowledgeArticle::getId, articleIds))
                    .stream().collect(Collectors.toMap(KnowledgeArticle::getId, KnowledgeArticle::getTitle));
        }
        for (KnowledgeRequest r : list) {
            if (r.getExpectCategoryId() != null) r.setExpectCategoryName(catMap.get(r.getExpectCategoryId()));
            if (r.getLinkedArticleId() != null) r.setLinkedArticleTitle(articleMap.get(r.getLinkedArticleId()));
            if (r.getExpectTags() != null && !r.getExpectTags().isBlank()) {
                r.setExpectTagList(Arrays.stream(r.getExpectTags().split(","))
                        .map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList()));
            } else {
                r.setExpectTagList(Collections.emptyList());
            }
        }
    }
}
