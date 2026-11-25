package com.qykj.finmaster.knowledge.article.controller;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.domain.req.KnowledgeArticleQueryReq;
import com.qykj.finmaster.knowledge.article.domain.req.KnowledgeArticleSaveReq;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticleReference;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeViewLog;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleReferenceManager;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeViewLogManager;
import com.qykj.finmaster.knowledge.category.entity.KnowledgeCategory;
import com.qykj.finmaster.knowledge.category.manager.KnowledgeCategoryManager;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeFavorite;
import com.qykj.finmaster.knowledge.like.entity.KnowledgeLike;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeFavoriteManager;
import com.qykj.finmaster.knowledge.like.manager.KnowledgeLikeManager;
import com.qykj.finmaster.knowledge.tag.entity.KnowledgeArticleTag;
import com.qykj.finmaster.knowledge.tag.entity.KnowledgeTag;
import com.qykj.finmaster.knowledge.tag.manager.KnowledgeArticleTagManager;
import com.qykj.finmaster.knowledge.tag.manager.KnowledgeTagManager;
import com.qykj.finmaster.security.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/knowledge/article")
public class KnowledgeArticleController extends BaseController {

    @Autowired
    private KnowledgeArticleManager articleManager;
    @Autowired
    private KnowledgeCategoryManager categoryManager;
    @Autowired
    private KnowledgeTagManager tagManager;
    @Autowired
    private KnowledgeArticleTagManager articleTagManager;
    @Autowired
    private KnowledgeViewLogManager viewLogManager;
    @Autowired
    private KnowledgeLikeManager likeManager;
    @Autowired
    private KnowledgeFavoriteManager favoriteManager;
    @Autowired
    private KnowledgeArticleReferenceManager articleReferenceManager;

    /**
     * 文章列表（前台 / 管理后台共用）
     */
    @GetMapping("/list")
    public Result<?> list(KnowledgeArticleQueryReq req) {
        int pageNum = req.getPageNum() == null ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null ? 10 : req.getPageSize();

        LambdaQueryWrapper<KnowledgeArticle> w = new LambdaQueryWrapper<>();
        if (req.getCategoryId() != null) {
            // 包含子分类
            List<Long> ids = collectCategoryAndChildren(req.getCategoryId());
            w.in(KnowledgeArticle::getCategoryId, ids);
        }
        if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
            String kw = req.getKeyword().trim();
            w.and(qw -> qw.like(KnowledgeArticle::getTitle, kw)
                    .or().like(KnowledgeArticle::getSummary, kw)
                    .or().like(KnowledgeArticle::getContentText, kw));
        }
        if (req.getAuthorId() != null) {
            w.eq(KnowledgeArticle::getAuthorId, req.getAuthorId());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            w.eq(KnowledgeArticle::getStatus, req.getStatus());
        }

        // 标签过滤
        if (req.getTagId() != null || (req.getTagName() != null && !req.getTagName().isBlank())) {
            LambdaQueryWrapper<KnowledgeArticleTag> tw = new LambdaQueryWrapper<>();
            if (req.getTagId() != null) tw.eq(KnowledgeArticleTag::getTagId, req.getTagId());
            if (req.getTagName() != null && !req.getTagName().isBlank())
                tw.eq(KnowledgeArticleTag::getTagName, req.getTagName().trim());
            List<Long> articleIds = articleTagManager.list(tw).stream()
                    .map(KnowledgeArticleTag::getArticleId).distinct().collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                return Result.ok(Collections.emptyList(), 0);
            }
            w.in(KnowledgeArticle::getId, articleIds);
        }

        String sort = req.getSortBy() == null ? "latest" : req.getSortBy();
        if ("hot".equalsIgnoreCase(sort)) {
            w.orderByDesc(KnowledgeArticle::getViewCount).orderByDesc(KnowledgeArticle::getCreateTime);
        } else if ("top".equalsIgnoreCase(sort)) {
            w.orderByDesc(KnowledgeArticle::getIsTop)
                    .orderByDesc(KnowledgeArticle::getPublishTime)
                    .orderByDesc(KnowledgeArticle::getCreateTime);
        } else {
            w.orderByDesc(KnowledgeArticle::getIsTop)
                    .orderByDesc(KnowledgeArticle::getPublishTime)
                    .orderByDesc(KnowledgeArticle::getCreateTime);
        }

        Page<KnowledgeArticle> page = articleManager.page(new Page<>(pageNum, pageSize), w);

        List<KnowledgeArticle> records = page.getRecords();
        records.forEach(a -> a.setContent(null)); // 列表不返回正文
        fillCategoryAndTags(records);

        return Result.ok(records, page.getTotal());
    }

    /**
     * 文章详情（自动 +1 浏览，记录 view_log）
     */
    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        KnowledgeArticle a = articleManager.getById(id);
        if (a == null || (a.getDeleted() != null && a.getDeleted() == 1)) {
            return Result.error("文章不存在");
        }
        LoginUser u = getLoginUser();

        // 浏览 +1
        a.setViewCount((a.getViewCount() == null ? 0 : a.getViewCount()) + 1);
        articleManager.lambdaUpdate()
                .setSql("view_count = COALESCE(view_count,0) + 1")
                .eq(KnowledgeArticle::getId, id).update();

        // 写浏览日志
        try {
            KnowledgeViewLog log = new KnowledgeViewLog();
            log.setArticleId(id);
            log.setUserId(u.getUserId());
            log.setUserName(u.getRealName());
            log.setTenantId(u.getTenantId());
            log.setViewTime(new Date());
            viewLogManager.save(log);
        } catch (Exception e) {
            // 日志失败不影响主流程
        }

        // 填充分类、标签
        fillCategoryAndTags(Collections.singletonList(a));

        // 是否已点赞 / 踩 / 收藏
        if (u.getUserId() != null) {
            long liked = likeManager.count(new LambdaQueryWrapper<KnowledgeLike>()
                    .eq(KnowledgeLike::getTargetType, "ARTICLE")
                    .eq(KnowledgeLike::getTargetId, id)
                    .eq(KnowledgeLike::getUserId, u.getUserId())
                    .eq(KnowledgeLike::getActionType, "LIKE"));
            a.setLiked(liked > 0);
            long disliked = likeManager.count(new LambdaQueryWrapper<KnowledgeLike>()
                    .eq(KnowledgeLike::getTargetType, "ARTICLE")
                    .eq(KnowledgeLike::getTargetId, id)
                    .eq(KnowledgeLike::getUserId, u.getUserId())
                    .eq(KnowledgeLike::getActionType, "DISLIKE"));
            a.setDisliked(disliked > 0);
            long fav = favoriteManager.count(new LambdaQueryWrapper<KnowledgeFavorite>()
                    .eq(KnowledgeFavorite::getArticleId, id)
                    .eq(KnowledgeFavorite::getUserId, u.getUserId()));
            a.setFavorited(fav > 0);
        }

        // 引用文章（作者手动指定，按 sort 升序）
        a.setReferences(loadReferences(id));

        return Result.ok(a);
    }

    /**
     * 新增文章（DRAFT 不需 publish 权限；PUBLISHED 需 add 权限）
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('knowledge:article:add')")
    public Result<?> add(@RequestBody KnowledgeArticleSaveReq req) {
        LoginUser u = getLoginUser();
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            return Result.error("标题不能为空");
        }

        KnowledgeArticle a = new KnowledgeArticle();
        a.setTitle(req.getTitle().trim());
        a.setSummary(buildSummary(req));
        a.setContent(req.getContent());
        a.setContentText(stripHtml(req.getContent()));
        a.setCategoryId(req.getCategoryId());
        a.setCoverImage(req.getCoverImage());
        a.setStatus(req.getStatus() == null ? "DRAFT" : req.getStatus());
        a.setAuthorId(u.getUserId());
        a.setAuthorName(u.getRealName());
        a.setIsTop(false);
        a.setIsRecommended(false);
        a.setViewCount(0);
        a.setLikeCount(0);
        a.setCommentCount(0);
        a.setFavoriteCount(0);
        if ("PUBLISHED".equals(a.getStatus())) {
            a.setPublishTime(new Date());
        }
        a.setTenantId(u.getTenantId());
        a.setCreateBy(u.getUsername());
        a.setCreateByName(u.getRealName());
        a.setCreateTime(new Date());

        articleManager.save(a);

        saveTags(a.getId(), req.getTags());
        saveReferences(a.getId(), req.getReferenceIds());

        return Result.ok(a.getId());
    }

    /**
     * 修改文章
     */
    @PutMapping
    public Result<?> update(@RequestBody KnowledgeArticleSaveReq req) {
        LoginUser u = getLoginUser();
        if (req.getId() == null) return Result.error("ID不能为空");

        KnowledgeArticle exists = articleManager.getById(req.getId());
        if (exists == null) return Result.error("文章不存在");

        // 权限校验：作者本人 OR 有 edit 权限
        boolean isAuthor = exists.getAuthorId() != null && exists.getAuthorId().equals(u.getUserId());
        boolean hasPermi = u.getPermissions() != null && u.getPermissions().contains("knowledge:article:edit");
        if (!isAuthor && !hasPermi) {
            return Result.error("无权修改该文章");
        }

        KnowledgeArticle update = new KnowledgeArticle();
        update.setId(req.getId());
        update.setTitle(req.getTitle());
        update.setSummary(buildSummary(req));
        update.setContent(req.getContent());
        update.setContentText(stripHtml(req.getContent()));
        update.setCategoryId(req.getCategoryId());
        update.setCoverImage(req.getCoverImage());
        if (req.getStatus() != null) {
            update.setStatus(req.getStatus());
            if ("PUBLISHED".equals(req.getStatus()) && exists.getPublishTime() == null) {
                update.setPublishTime(new Date());
            }
        }
        update.setUpdateBy(u.getUsername());
        update.setUpdateByName(u.getRealName());
        update.setUpdateTime(new Date());
        articleManager.updateById(update);

        // 重新维护标签
        if (req.getTags() != null) {
            articleTagManager.remove(new LambdaQueryWrapper<KnowledgeArticleTag>()
                    .eq(KnowledgeArticleTag::getArticleId, req.getId()));
            saveTags(req.getId(), req.getTags());
        }

        // 重新维护引用关系
        if (req.getReferenceIds() != null) {
            articleReferenceManager.remove(new LambdaQueryWrapper<KnowledgeArticleReference>()
                    .eq(KnowledgeArticleReference::getArticleId, req.getId()));
            saveReferences(req.getId(), req.getReferenceIds());
        }

        return Result.ok();
    }

    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        LoginUser u = getLoginUser();
        KnowledgeArticle exists = articleManager.getById(id);
        if (exists == null) return Result.error("文章不存在");

        boolean isAuthor = exists.getAuthorId() != null && exists.getAuthorId().equals(u.getUserId());
        boolean hasPermi = u.getPermissions() != null && u.getPermissions().contains("knowledge:article:delete");
        if (!isAuthor && !hasPermi) {
            return Result.error("无权删除该文章");
        }
        articleManager.removeById(id);
        return Result.ok();
    }

    /**
     * 发布
     */
    @PutMapping("/publish/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:article:publish')")
    public Result<?> publish(@PathVariable Long id) {
        articleManager.lambdaUpdate()
                .set(KnowledgeArticle::getStatus, "PUBLISHED")
                .set(KnowledgeArticle::getPublishTime, new Date())
                .set(KnowledgeArticle::getUpdateTime, new Date())
                .eq(KnowledgeArticle::getId, id).update();
        return Result.ok();
    }

    /**
     * 归档
     */
    @PutMapping("/archive/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:article:publish')")
    public Result<?> archive(@PathVariable Long id) {
        articleManager.lambdaUpdate()
                .set(KnowledgeArticle::getStatus, "ARCHIVED")
                .set(KnowledgeArticle::getUpdateTime, new Date())
                .eq(KnowledgeArticle::getId, id).update();
        return Result.ok();
    }

    /**
     * 置顶 / 取消置顶
     */
    @PutMapping("/top/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:article:publish')")
    public Result<?> toggleTop(@PathVariable Long id) {
        KnowledgeArticle a = articleManager.getById(id);
        if (a == null) return Result.error("文章不存在");
        Boolean newVal = !(Boolean.TRUE.equals(a.getIsTop()));
        articleManager.lambdaUpdate()
                .set(KnowledgeArticle::getIsTop, newVal)
                .eq(KnowledgeArticle::getId, id).update();
        return Result.ok(newVal);
    }

    /**
     * 推荐 / 取消推荐
     */
    @PutMapping("/recommend/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:article:publish')")
    public Result<?> toggleRecommend(@PathVariable Long id) {
        KnowledgeArticle a = articleManager.getById(id);
        if (a == null) return Result.error("文章不存在");
        Boolean newVal = !(Boolean.TRUE.equals(a.getIsRecommended()));
        articleManager.lambdaUpdate()
                .set(KnowledgeArticle::getIsRecommended, newVal)
                .eq(KnowledgeArticle::getId, id).update();
        return Result.ok(newVal);
    }

    /**
     * 富文本图片上传（WangEditor 用）
     */
    @PostMapping("/upload-image")
    @PreAuthorize("@ss.hasPermi('knowledge:article:add')")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.error("文件不能为空");
        try {
            String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
            if (suffix == null || suffix.isBlank()) suffix = "png";
            String fileName = "KB_" + System.currentTimeMillis() + "_" + new Random().nextInt(10000) + "." + suffix;
            File dir = new File(FileConstants.FILE_URL_KNOWLEDGE_IMAGE);
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new File(FileConstants.FILE_URL_KNOWLEDGE_IMAGE + File.separator + fileName));
            // 走后端流式返回，避免依赖 Nginx 静态映射
            String url = "/finmaster-api/knowledge/article/image/" + fileName;
            Map<String, Object> wangResp = new HashMap<>();
            wangResp.put("errno", 0);
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("alt", file.getOriginalFilename());
            wangResp.put("data", data);
            return Result.ok(wangResp);
        } catch (Exception e) {
            log.error("知识库图片上传失败", e);
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 富文本图片读取（公开访问，浏览器 img src 直接加载）
     * 路径需要在 SecurityConfig 白名单
     */
    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        // 防越权：阻止 ../ 路径穿越
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }
        File file = new File(FileConstants.FILE_URL_KNOWLEDGE_IMAGE, filename);
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaType.IMAGE_PNG;
        try {
            String mime = Files.probeContentType(file.toPath());
            if (mime != null) mediaType = MediaType.parseMediaType(mime);
        } catch (IOException ignored) {
        }
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .body(new FileSystemResource(file));
    }

    /**
     * 推荐文章
     */
    @GetMapping("/recommended")
    public Result<?> recommended(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> list = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .eq(KnowledgeArticle::getIsRecommended, true)
                .orderByDesc(KnowledgeArticle::getPublishTime)
                .last("LIMIT " + Math.min(limit, 50)));
        list.forEach(a -> { a.setContent(null); a.setContentText(null); });
        fillCategoryAndTags(list);
        return Result.ok(list);
    }

    /**
     * 热门文章
     */
    @GetMapping("/hot")
    public Result<?> hot(@RequestParam(defaultValue = "10") Integer limit) {
        List<KnowledgeArticle> list = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getStatus, "PUBLISHED")
                .orderByDesc(KnowledgeArticle::getViewCount)
                .last("LIMIT " + Math.min(limit, 50)));
        list.forEach(a -> { a.setContent(null); a.setContentText(null); });
        fillCategoryAndTags(list);
        return Result.ok(list);
    }

    /**
     * 我的文章（含草稿）
     */
    @GetMapping("/my")
    public Result<?> my(KnowledgeArticleQueryReq req) {
        LoginUser u = getLoginUser();
        req.setAuthorId(u.getUserId());
        return list(req);
    }

    /**
     * 最近浏览
     */
    @GetMapping("/recent-viewed")
    public Result<?> recentViewed(@RequestParam(defaultValue = "10") Integer limit) {
        LoginUser u = getLoginUser();
        if (u.getUserId() == null) return Result.ok(Collections.emptyList());
        List<KnowledgeViewLog> logs = viewLogManager.list(new LambdaQueryWrapper<KnowledgeViewLog>()
                .eq(KnowledgeViewLog::getUserId, u.getUserId())
                .orderByDesc(KnowledgeViewLog::getViewTime)
                .last("LIMIT " + Math.min(limit, 50)));
        // 去重保留最新
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        logs.forEach(l -> ids.add(l.getArticleId()));
        if (ids.isEmpty()) return Result.ok(Collections.emptyList());

        List<KnowledgeArticle> articles = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                .in(KnowledgeArticle::getId, ids));
        Map<Long, KnowledgeArticle> map = articles.stream().collect(Collectors.toMap(KnowledgeArticle::getId, x -> x));
        List<KnowledgeArticle> ordered = new ArrayList<>();
        for (Long id : ids) {
            KnowledgeArticle a = map.get(id);
            if (a != null) {
                a.setContent(null);
                a.setContentText(null);
                ordered.add(a);
            }
        }
        fillCategoryAndTags(ordered);
        return Result.ok(ordered);
    }

    /**
     * 阅读记录审计（管理后台用）—— 谁、什么时候、读过哪篇文章
     */
    @GetMapping("/view-log/list")
    @PreAuthorize("@ss.hasPermi('knowledge:article:list')")
    public Result<?> viewLogList(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "20") Integer pageSize,
                                 @RequestParam(required = false) Long articleId,
                                 @RequestParam(required = false) Long userId,
                                 @RequestParam(required = false) String userName,
                                 @RequestParam(required = false) String start,
                                 @RequestParam(required = false) String end) {
        LambdaQueryWrapper<KnowledgeViewLog> w = new LambdaQueryWrapper<>();
        if (articleId != null) w.eq(KnowledgeViewLog::getArticleId, articleId);
        if (userId != null) w.eq(KnowledgeViewLog::getUserId, userId);
        if (userName != null && !userName.isBlank()) w.like(KnowledgeViewLog::getUserName, userName.trim());
        if (start != null && !start.isBlank()) w.ge(KnowledgeViewLog::getViewTime, start);
        if (end != null && !end.isBlank()) w.le(KnowledgeViewLog::getViewTime, end);
        w.orderByDesc(KnowledgeViewLog::getViewTime);

        Page<KnowledgeViewLog> page = viewLogManager.page(new Page<>(pageNum, pageSize), w);

        // 反查文章标题
        if (!page.getRecords().isEmpty()) {
            Set<Long> ids = page.getRecords().stream()
                    .map(KnowledgeViewLog::getArticleId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!ids.isEmpty()) {
                Map<Long, String> titleMap = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                                .select(KnowledgeArticle::getId, KnowledgeArticle::getTitle)
                                .in(KnowledgeArticle::getId, ids))
                        .stream().collect(Collectors.toMap(KnowledgeArticle::getId, KnowledgeArticle::getTitle));
                page.getRecords().forEach(r -> r.setArticleTitle(titleMap.get(r.getArticleId())));
            }
        }

        return Result.ok(page.getRecords(), page.getTotal());
    }

    // ========== 辅助方法 ==========

    private List<Long> collectCategoryAndChildren(Long rootId) {
        List<KnowledgeCategory> all = categoryManager.list();
        Set<Long> ids = new HashSet<>();
        ids.add(rootId);
        Map<Long, List<Long>> childMap = new HashMap<>();
        for (KnowledgeCategory c : all) {
            childMap.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(c.getId());
        }
        Deque<Long> q = new ArrayDeque<>();
        q.offer(rootId);
        while (!q.isEmpty()) {
            Long cur = q.poll();
            List<Long> ch = childMap.get(cur);
            if (ch != null) {
                for (Long c : ch) {
                    if (ids.add(c)) q.offer(c);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private void fillCategoryAndTags(List<KnowledgeArticle> list) {
        if (list == null || list.isEmpty()) return;

        Set<Long> categoryIds = list.stream().map(KnowledgeArticle::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            catMap = categoryManager.list(new LambdaQueryWrapper<KnowledgeCategory>()
                            .in(KnowledgeCategory::getId, categoryIds))
                    .stream().collect(Collectors.toMap(KnowledgeCategory::getId, KnowledgeCategory::getName));
        }
        for (KnowledgeArticle a : list) {
            if (a.getCategoryId() != null) a.setCategoryName(catMap.get(a.getCategoryId()));
        }

        Set<Long> articleIds = list.stream().map(KnowledgeArticle::getId).collect(Collectors.toSet());
        Map<Long, List<String>> tagMap = articleTagManager.list(new LambdaQueryWrapper<KnowledgeArticleTag>()
                        .in(KnowledgeArticleTag::getArticleId, articleIds))
                .stream().collect(Collectors.groupingBy(
                        KnowledgeArticleTag::getArticleId,
                        Collectors.mapping(KnowledgeArticleTag::getTagName, Collectors.toList())));
        for (KnowledgeArticle a : list) {
            a.setTags(tagMap.getOrDefault(a.getId(), Collections.emptyList()));
        }
    }

    private void saveTags(Long articleId, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        LoginUser u = getLoginUser();
        for (String name : tags) {
            if (name == null || name.isBlank()) continue;
            String tagName = name.trim();
            KnowledgeTag tag = tagManager.getOne(new LambdaQueryWrapper<KnowledgeTag>()
                    .eq(KnowledgeTag::getName, tagName).last("LIMIT 1"));
            if (tag == null) {
                tag = new KnowledgeTag();
                tag.setName(tagName);
                tag.setUseCount(1);
                tag.setTenantId(u.getTenantId());
                tag.setCreateBy(u.getUsername());
                tag.setCreateByName(u.getRealName());
                tag.setCreateTime(new Date());
                tagManager.save(tag);
            } else {
                tagManager.lambdaUpdate()
                        .setSql("use_count = COALESCE(use_count,0) + 1")
                        .eq(KnowledgeTag::getId, tag.getId()).update();
            }
            KnowledgeArticleTag link = new KnowledgeArticleTag();
            link.setArticleId(articleId);
            link.setTagId(tag.getId());
            link.setTagName(tagName);
            link.setCreateTime(new Date());
            try {
                articleTagManager.save(link);
            } catch (Exception ignore) {
                // 唯一索引冲突忽略
            }
        }
    }

    /**
     * 保存文章引用关系（按传入顺序作为 sort）
     */
    private void saveReferences(Long articleId, List<Long> refIds) {
        if (refIds == null || refIds.isEmpty()) return;
        Set<Long> seen = new LinkedHashSet<>();
        int sortIdx = 0;
        for (Long refId : refIds) {
            if (refId == null || refId.equals(articleId)) continue;
            if (!seen.add(refId)) continue; // 去重
            KnowledgeArticleReference link = new KnowledgeArticleReference();
            link.setArticleId(articleId);
            link.setRefArticleId(refId);
            link.setSort(sortIdx++);
            link.setCreateTime(new Date());
            try {
                articleReferenceManager.save(link);
            } catch (Exception ignore) {
                // 唯一索引冲突忽略
            }
        }
    }

    /**
     * 加载某文章的引用文章简要信息（按 sort 升序）
     */
    private List<KnowledgeArticle> loadReferences(Long articleId) {
        List<KnowledgeArticleReference> links = articleReferenceManager.list(
                new LambdaQueryWrapper<KnowledgeArticleReference>()
                        .eq(KnowledgeArticleReference::getArticleId, articleId)
                        .orderByAsc(KnowledgeArticleReference::getSort));
        if (links.isEmpty()) return Collections.emptyList();
        List<Long> refIds = links.stream().map(KnowledgeArticleReference::getRefArticleId).collect(Collectors.toList());
        List<KnowledgeArticle> articles = articleManager.list(new LambdaQueryWrapper<KnowledgeArticle>()
                .select(KnowledgeArticle::getId, KnowledgeArticle::getTitle, KnowledgeArticle::getSummary,
                        KnowledgeArticle::getCategoryId, KnowledgeArticle::getAuthorName,
                        KnowledgeArticle::getViewCount, KnowledgeArticle::getStatus, KnowledgeArticle::getPublishTime)
                .in(KnowledgeArticle::getId, refIds)
                .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        Map<Long, KnowledgeArticle> map = articles.stream().collect(Collectors.toMap(KnowledgeArticle::getId, x -> x));
        List<KnowledgeArticle> result = new ArrayList<>();
        for (Long id : refIds) {
            KnowledgeArticle a = map.get(id);
            if (a != null) result.add(a);
        }
        return result;
    }

    private String buildSummary(KnowledgeArticleSaveReq req) {
        if (req.getSummary() != null && !req.getSummary().isBlank()) {
            return req.getSummary().length() > 500 ? req.getSummary().substring(0, 500) : req.getSummary();
        }
        String text = stripHtml(req.getContent());
        if (text == null) return null;
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ").trim();
    }
}
