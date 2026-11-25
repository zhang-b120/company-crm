package com.qykj.finmaster.knowledge.category.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.article.entity.KnowledgeArticle;
import com.qykj.finmaster.knowledge.article.manager.KnowledgeArticleManager;
import com.qykj.finmaster.knowledge.category.entity.KnowledgeCategory;
import com.qykj.finmaster.knowledge.category.manager.KnowledgeCategoryManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/knowledge/category")
public class KnowledgeCategoryController extends BaseController {

    @Autowired
    private KnowledgeCategoryManager categoryManager;

    @Autowired
    private KnowledgeArticleManager articleManager;

    /**
     * 树形列表（带每个分类的已发布文章数）
     */
    @GetMapping("/tree")
    public Result<?> tree() {
        List<KnowledgeCategory> all = categoryManager.list(
                new LambdaQueryWrapper<KnowledgeCategory>()
                        .orderByAsc(KnowledgeCategory::getSort)
                        .orderByAsc(KnowledgeCategory::getId));

        // 文章数（仅统计已发布）
        List<KnowledgeArticle> articles = articleManager.list(
                new LambdaQueryWrapper<KnowledgeArticle>()
                        .select(KnowledgeArticle::getCategoryId)
                        .eq(KnowledgeArticle::getStatus, "PUBLISHED"));
        Map<Long, Long> countMap = articles.stream()
                .filter(a -> a.getCategoryId() != null)
                .collect(Collectors.groupingBy(KnowledgeArticle::getCategoryId, Collectors.counting()));

        all.forEach(c -> c.setArticleCount(countMap.getOrDefault(c.getId(), 0L).intValue()));

        return Result.ok(buildTree(all, 0L));
    }

    /**
     * 平铺列表
     */
    @GetMapping("/list")
    public Result<?> list() {
        List<KnowledgeCategory> all = categoryManager.list(
                new LambdaQueryWrapper<KnowledgeCategory>()
                        .orderByAsc(KnowledgeCategory::getSort)
                        .orderByAsc(KnowledgeCategory::getId));
        return Result.ok(all);
    }

    @GetMapping("/{id}")
    public Result<?> get(@PathVariable Long id) {
        return Result.ok(categoryManager.getById(id));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('knowledge:category:manage')")
    public Result<?> add(@RequestBody KnowledgeCategory req) {
        LoginUser loginUser = getLoginUser();
        req.setId(null);
        if (req.getParentId() == null) req.setParentId(0L);
        if (req.getEnabled() == null) req.setEnabled(true);
        if (req.getSort() == null) req.setSort(0);
        req.setTenantId(loginUser.getTenantId());
        req.setCreateBy(loginUser.getUsername());
        req.setCreateByName(loginUser.getRealName());
        req.setCreateTime(new Date());
        categoryManager.save(req);
        return Result.ok(req.getId());
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('knowledge:category:manage')")
    public Result<?> update(@RequestBody KnowledgeCategory req) {
        LoginUser loginUser = getLoginUser();
        req.setUpdateBy(loginUser.getUsername());
        req.setUpdateByName(loginUser.getRealName());
        req.setUpdateTime(new Date());
        categoryManager.updateById(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:category:manage')")
    public Result<?> delete(@PathVariable Long id) {
        // 1. 是否有子分类
        long childCount = categoryManager.count(new LambdaQueryWrapper<KnowledgeCategory>()
                .eq(KnowledgeCategory::getParentId, id));
        if (childCount > 0) {
            return Result.error("该分类下存在子分类，请先删除子分类");
        }
        // 2. 是否还有文章
        long articleCount = articleManager.count(new LambdaQueryWrapper<KnowledgeArticle>()
                .eq(KnowledgeArticle::getCategoryId, id));
        if (articleCount > 0) {
            return Result.error("该分类下还有文章，请先迁移或删除文章");
        }
        categoryManager.removeById(id);
        return Result.ok();
    }

    private List<KnowledgeCategory> buildTree(List<KnowledgeCategory> all, Long rootParentId) {
        Map<Long, List<KnowledgeCategory>> grouped = all.stream()
                .filter(c -> c.getParentId() != null)
                .collect(Collectors.groupingBy(KnowledgeCategory::getParentId));
        return assemble(grouped, rootParentId);
    }

    private List<KnowledgeCategory> assemble(Map<Long, List<KnowledgeCategory>> grouped, Long parentId) {
        List<KnowledgeCategory> nodes = grouped.getOrDefault(parentId, Collections.emptyList());
        for (KnowledgeCategory n : nodes) {
            List<KnowledgeCategory> children = assemble(grouped, n.getId());
            n.setChildren(children);
            // 累计子分类的文章数
            int totalArticles = (n.getArticleCount() == null ? 0 : n.getArticleCount());
            for (KnowledgeCategory c : children) {
                totalArticles += (c.getArticleCount() == null ? 0 : c.getArticleCount());
            }
            n.setArticleCount(totalArticles);
        }
        return nodes;
    }
}
