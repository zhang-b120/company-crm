package com.qykj.finmaster.knowledge.like.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.comment.entity.KnowledgeComment;
import com.qykj.finmaster.knowledge.comment.manager.KnowledgeCommentManager;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeFavorite;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeLike;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeFavoriteManager;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeLikeManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeInteractionController extends BaseController {

    @Autowired
    private KnowledgeLikeManager likeManager;

    @Autowired
    private KnowledgeFavoriteManager favoriteManager;

    @Autowired
    private KnowledgeArticleManager articleManager;

    @Autowired
    private KnowledgeCommentManager commentManager;

    /**
     * 切换点赞 / 踩
     * body: { targetType: "ARTICLE"|"COMMENT", targetId: 1, actionType: "LIKE"|"DISLIKE" }
     * 互斥：点赞会自动取消已存在的踩，反之亦然
     */
    @PostMapping("/like/toggle")
    public Result<?> toggleLike(@RequestBody Map<String, Object> req) {
        LoginUser u = getLoginUser();
        String targetType = (String) req.get("targetType");
        Object idObj = req.get("targetId");
        if (targetType == null || idObj == null) return Result.error("参数错误");
        Long targetId = Long.valueOf(idObj.toString());
        String actionType = (String) req.get("actionType");
        if (actionType == null || actionType.isBlank()) actionType = "LIKE";
        if (!"LIKE".equals(actionType) && !"DISLIKE".equals(actionType)) {
            return Result.error("actionType 仅支持 LIKE / DISLIKE");
        }
        String oppositeAction = "LIKE".equals(actionType) ? "DISLIKE" : "LIKE";

        // 当前 action 是否已存在
        KnowledgeLike sameAction = likeManager.getOne(new LambdaQueryWrapper<KnowledgeLike>()
                .eq(KnowledgeLike::getTargetType, targetType)
                .eq(KnowledgeLike::getTargetId, targetId)
                .eq(KnowledgeLike::getUserId, u.getUserId())
                .eq(KnowledgeLike::getActionType, actionType)
                .last("LIMIT 1"));

        boolean active;
        if (sameAction != null) {
            // 取消当前 action
            likeManager.removeById(sameAction.getId());
            updateActionCount(targetType, targetId, actionType, -1);
            active = false;
        } else {
            // 先取消相反 action（互斥）
            KnowledgeLike opposite = likeManager.getOne(new LambdaQueryWrapper<KnowledgeLike>()
                    .eq(KnowledgeLike::getTargetType, targetType)
                    .eq(KnowledgeLike::getTargetId, targetId)
                    .eq(KnowledgeLike::getUserId, u.getUserId())
                    .eq(KnowledgeLike::getActionType, oppositeAction)
                    .last("LIMIT 1"));
            if (opposite != null) {
                likeManager.removeById(opposite.getId());
                updateActionCount(targetType, targetId, oppositeAction, -1);
            }

            KnowledgeLike like = new KnowledgeLike();
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            like.setUserId(u.getUserId());
            like.setActionType(actionType);
            like.setTenantId(u.getTenantId());
            like.setCreateTime(new Date());
            try {
                likeManager.save(like);
            } catch (Exception ignore) {
                // 唯一索引冲突
            }
            updateActionCount(targetType, targetId, actionType, 1);
            active = true;
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("actionType", actionType);
        resp.put("active", active); // 当前 action 是否激活
        // 返回最新计数
        Integer likeCount = getActionCount(targetType, targetId, "LIKE");
        Integer dislikeCount = getActionCount(targetType, targetId, "DISLIKE");
        resp.put("liked", "LIKE".equals(actionType) ? active : false);
        resp.put("disliked", "DISLIKE".equals(actionType) ? active : false);
        resp.put("likeCount", likeCount);
        resp.put("dislikeCount", dislikeCount);
        return Result.ok(resp);
    }

    /**
     * 切换收藏
     */
    @PostMapping("/favorite/toggle")
    public Result<?> toggleFavorite(@RequestBody Map<String, Object> req) {
        LoginUser u = getLoginUser();
        Object idObj = req.get("articleId");
        if (idObj == null) return Result.error("参数错误");
        Long articleId = Long.valueOf(idObj.toString());

        KnowledgeFavorite exists = favoriteManager.getOne(new LambdaQueryWrapper<KnowledgeFavorite>()
                .eq(KnowledgeFavorite::getArticleId, articleId)
                .eq(KnowledgeFavorite::getUserId, u.getUserId())
                .last("LIMIT 1"));

        boolean favorited;
        if (exists != null) {
            favoriteManager.removeById(exists.getId());
            articleManager.lambdaUpdate()
                    .setSql("favorite_count = GREATEST(COALESCE(favorite_count,0) - 1, 0)")
                    .eq(KnowledgeArticle::getId, articleId).update();
            favorited = false;
        } else {
            KnowledgeFavorite f = new KnowledgeFavorite();
            f.setArticleId(articleId);
            f.setUserId(u.getUserId());
            f.setTenantId(u.getTenantId());
            f.setCreateTime(new Date());
            try {
                favoriteManager.save(f);
            } catch (Exception ignore) {}
            articleManager.lambdaUpdate()
                    .setSql("favorite_count = COALESCE(favorite_count,0) + 1")
                    .eq(KnowledgeArticle::getId, articleId).update();
            favorited = true;
        }

        KnowledgeArticle a = articleManager.getById(articleId);
        Map<String, Object> resp = new HashMap<>();
        resp.put("favorited", favorited);
        resp.put("favoriteCount", a == null ? 0 : a.getFavoriteCount());
        return Result.ok(resp);
    }

    /**
     * 我的收藏列表
     */
    @GetMapping("/favorite/my")
    public Result<?> myFavorites(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginUser u = getLoginUser();
        Page<KnowledgeFavorite> page = favoriteManager.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<KnowledgeFavorite>()
                        .eq(KnowledgeFavorite::getUserId, u.getUserId())
                        .orderByDesc(KnowledgeFavorite::getCreateTime));

        if (page.getRecords().isEmpty()) return Result.ok(Collections.emptyList(), 0);

        List<Long> ids = page.getRecords().stream().map(KnowledgeFavorite::getArticleId).collect(Collectors.toList());
        List<KnowledgeArticle> articles = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                .in(KnowledgeArticle::getId, ids));
        Map<Long, KnowledgeArticle> map = articles.stream().collect(Collectors.toMap(KnowledgeArticle::getId, x -> x));
        List<KnowledgeArticle> ordered = new ArrayList<>();
        for (KnowledgeFavorite f : page.getRecords()) {
            KnowledgeArticle a = map.get(f.getArticleId());
            if (a != null) {
                a.setContent(null);
                a.setContentText(null);
                ordered.add(a);
            }
        }
        return Result.ok(ordered, page.getTotal());
    }

    /**
     * 我的评论列表
     */
    @GetMapping("/comment/my")
    public Result<?> myComments(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        LoginUser u = getLoginUser();
        Page<KnowledgeComment> page = commentManager.page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<KnowledgeComment>()
                        .eq(KnowledgeComment::getUserId, u.getUserId())
                        .orderByDesc(KnowledgeComment::getCreateTime));
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * action_type 计数列加减：column 自动按 LIKE/DISLIKE 选择
     */
    private void updateActionCount(String targetType, Long targetId, String actionType, int delta) {
        String col = "DISLIKE".equalsIgnoreCase(actionType) ? "dislike_count" : "like_count";
        String sql = delta > 0
                ? col + " = COALESCE(" + col + ",0) + 1"
                : col + " = GREATEST(COALESCE(" + col + ",0) - 1, 0)";
        if ("ARTICLE".equalsIgnoreCase(targetType)) {
            articleManager.lambdaUpdate().setSql(sql).eq(KnowledgeArticle::getId, targetId).update();
        } else if ("COMMENT".equalsIgnoreCase(targetType)) {
            commentManager.lambdaUpdate().setSql(sql).eq(KnowledgeComment::getId, targetId).update();
        }
    }

    private Integer getActionCount(String targetType, Long targetId, String actionType) {
        if ("ARTICLE".equalsIgnoreCase(targetType)) {
            KnowledgeArticle a = articleManager.getById(targetId);
            if (a == null) return 0;
            return "DISLIKE".equalsIgnoreCase(actionType)
                    ? (a.getDislikeCount() == null ? 0 : a.getDislikeCount())
                    : (a.getLikeCount() == null ? 0 : a.getLikeCount());
        } else {
            KnowledgeComment c = commentManager.getById(targetId);
            if (c == null) return 0;
            return "DISLIKE".equalsIgnoreCase(actionType)
                    ? (c.getDislikeCount() == null ? 0 : c.getDislikeCount())
                    : (c.getLikeCount() == null ? 0 : c.getLikeCount());
        }
    }
}
