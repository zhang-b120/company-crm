package com.qykj.finmaster.knowledge.comment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.comment.entity.KnowledgeComment;
import com.qykj.finmaster.knowledge.comment.manager.KnowledgeCommentManager;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeLike;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeLikeManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/knowledge/comment")
public class KnowledgeCommentController extends BaseController {

    @Autowired
    private KnowledgeCommentManager commentManager;

    @Autowired
    private KnowledgeArticleManager articleManager;

    @Autowired
    private KnowledgeLikeManager likeManager;

    /**
     * 获取文章评论树
     */
    @GetMapping("/list/{articleId}")
    public Result<?> list(@PathVariable Long articleId) {
        LoginUser u = getLoginUser();

        List<KnowledgeComment> all = commentManager.list(new LambdaQueryWrapper<KnowledgeComment>()
                .eq(KnowledgeComment::getArticleId, articleId)
                .eq(KnowledgeComment::getStatus, "NORMAL")
                .orderByAsc(KnowledgeComment::getCreateTime));

        // 标记是否已点赞 / 已踩
        if (u.getUserId() != null && !all.isEmpty()) {
            List<Long> ids = all.stream().map(KnowledgeComment::getId).collect(Collectors.toList());
            List<KnowledgeLike> myActions = likeManager.list(new LambdaQueryWrapper<KnowledgeLike>()
                    .eq(KnowledgeLike::getTargetType, "COMMENT")
                    .in(KnowledgeLike::getTargetId, ids)
                    .eq(KnowledgeLike::getUserId, u.getUserId()));
            Set<Long> likedIds = myActions.stream()
                    .filter(l -> "LIKE".equals(l.getActionType()))
                    .map(KnowledgeLike::getTargetId).collect(Collectors.toSet());
            Set<Long> dislikedIds = myActions.stream()
                    .filter(l -> "DISLIKE".equals(l.getActionType()))
                    .map(KnowledgeLike::getTargetId).collect(Collectors.toSet());
            all.forEach(c -> {
                c.setLiked(likedIds.contains(c.getId()));
                c.setDisliked(dislikedIds.contains(c.getId()));
            });
        }

        // 构造树
        Map<Long, List<KnowledgeComment>> grouped = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(KnowledgeComment::getParentId));

        List<KnowledgeComment> roots = all.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .peek(c -> c.setChildren(flatChildren(c.getId(), grouped)))
                .collect(Collectors.toList());

        return Result.ok(roots, all.size());
    }

    private List<KnowledgeComment> flatChildren(Long parentId, Map<Long, List<KnowledgeComment>> grouped) {
        List<KnowledgeComment> result = new ArrayList<>();
        Deque<Long> q = new ArrayDeque<>();
        q.offer(parentId);
        while (!q.isEmpty()) {
            Long cur = q.poll();
            List<KnowledgeComment> children = grouped.get(cur);
            if (children != null) {
                for (KnowledgeComment c : children) {
                    result.add(c);
                    q.offer(c.getId());
                }
            }
        }
        return result;
    }

    /**
     * 提交评论（一级或回复，立即可见）
     */
    @PostMapping
    public Result<?> add(@RequestBody KnowledgeComment req) {
        LoginUser u = getLoginUser();
        if (req.getArticleId() == null) return Result.error("文章ID不能为空");
        if (req.getContent() == null || req.getContent().isBlank()) return Result.error("评论内容不能为空");

        KnowledgeArticle article = articleManager.getById(req.getArticleId());
        if (article == null) return Result.error("文章不存在");

        KnowledgeComment c = new KnowledgeComment();
        c.setArticleId(req.getArticleId());
        c.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        c.setReplyToUserId(req.getReplyToUserId());
        c.setReplyToUserName(req.getReplyToUserName());
        c.setContent(req.getContent().trim());
        c.setUserId(u.getUserId());
        c.setUserName(u.getRealName());
        c.setLikeCount(0);
        c.setStatus("NORMAL");
        c.setTenantId(u.getTenantId());
        c.setCreateBy(u.getUsername());
        c.setCreateByName(u.getRealName());
        c.setCreateTime(new Date());
        commentManager.save(c);

        // 评论数 +1
        articleManager.lambdaUpdate()
                .setSql("comment_count = COALESCE(comment_count,0) + 1")
                .eq(KnowledgeArticle::getId, req.getArticleId()).update();

        return Result.ok(c.getId());
    }

    /**
     * 删除评论（作者本人 / 管理员）
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        LoginUser u = getLoginUser();
        KnowledgeComment c = commentManager.getById(id);
        if (c == null) return Result.ok();
        boolean isAuthor = c.getUserId() != null && c.getUserId().equals(u.getUserId());
        boolean canManage = u.getPermissions() != null && u.getPermissions().contains("knowledge:comment:manage");
        if (!isAuthor && !canManage) return Result.error("无权删除该评论");

        commentManager.removeById(id);
        articleManager.lambdaUpdate()
                .setSql("comment_count = GREATEST(COALESCE(comment_count,0) - 1, 0)")
                .eq(KnowledgeArticle::getId, c.getArticleId()).update();
        return Result.ok();
    }

    // ==== 后台管理 ====

    @GetMapping("/admin/list")
    @PreAuthorize("@ss.hasPermi('knowledge:comment:manage')")
    public Result<?> adminList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize,
                               @RequestParam(required = false) Long articleId,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status) {
        LambdaQueryWrapper<KnowledgeComment> w = new LambdaQueryWrapper<>();
        if (articleId != null) w.eq(KnowledgeComment::getArticleId, articleId);
        if (status != null && !status.isBlank()) w.eq(KnowledgeComment::getStatus, status);
        if (keyword != null && !keyword.isBlank()) w.like(KnowledgeComment::getContent, keyword);
        w.orderByDesc(KnowledgeComment::getCreateTime);

        Page<KnowledgeComment> page = commentManager.page(new Page<>(pageNum, pageSize), w);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    @PutMapping("/admin/hide/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:comment:manage')")
    public Result<?> hide(@PathVariable Long id) {
        commentManager.lambdaUpdate()
                .set(KnowledgeComment::getStatus, "HIDDEN")
                .set(KnowledgeComment::getUpdateTime, new Date())
                .eq(KnowledgeComment::getId, id).update();
        return Result.ok();
    }

    @PutMapping("/admin/show/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:comment:manage')")
    public Result<?> show(@PathVariable Long id) {
        commentManager.lambdaUpdate()
                .set(KnowledgeComment::getStatus, "NORMAL")
                .set(KnowledgeComment::getUpdateTime, new Date())
                .eq(KnowledgeComment::getId, id).update();
        return Result.ok();
    }
}
