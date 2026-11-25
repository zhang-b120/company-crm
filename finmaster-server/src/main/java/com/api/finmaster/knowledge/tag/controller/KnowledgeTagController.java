package com.qykj.finmaster.knowledge.tag.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.knowledge.tag.entity.KnowledgeTag;
import com.qykj.finmaster.knowledge.tag.manager.KnowledgeTagManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/knowledge/tag")
public class KnowledgeTagController extends BaseController {

    @Autowired
    private KnowledgeTagManager tagManager;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String name) {
        LambdaQueryWrapper<KnowledgeTag> w = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            w.like(KnowledgeTag::getName, name);
        }
        w.orderByDesc(KnowledgeTag::getUseCount).orderByDesc(KnowledgeTag::getCreateTime);
        return Result.ok(tagManager.list(w));
    }

    @GetMapping("/popular")
    public Result<?> popular(@RequestParam(defaultValue = "20") Integer limit) {
        List<KnowledgeTag> list = tagManager.list(new LambdaQueryWrapper<KnowledgeTag>()
                .gt(KnowledgeTag::getUseCount, 0)
                .orderByDesc(KnowledgeTag::getUseCount)
                .last("LIMIT " + Math.max(1, Math.min(limit, 100))));
        return Result.ok(list);
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('knowledge:tag:manage')")
    public Result<?> add(@RequestBody KnowledgeTag req) {
        LoginUser u = getLoginUser();
        if (req.getName() == null || req.getName().isBlank()) {
            return Result.error("标签名不能为空");
        }
        long exists = tagManager.count(new LambdaQueryWrapper<KnowledgeTag>().eq(KnowledgeTag::getName, req.getName().trim()));
        if (exists > 0) return Result.error("标签已存在");
        req.setId(null);
        req.setName(req.getName().trim());
        if (req.getUseCount() == null) req.setUseCount(0);
        req.setTenantId(u.getTenantId());
        req.setCreateBy(u.getUsername());
        req.setCreateByName(u.getRealName());
        req.setCreateTime(new Date());
        tagManager.save(req);
        return Result.ok(req.getId());
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('knowledge:tag:manage')")
    public Result<?> update(@RequestBody KnowledgeTag req) {
        LoginUser u = getLoginUser();
        req.setUpdateBy(u.getUsername());
        req.setUpdateByName(u.getRealName());
        req.setUpdateTime(new Date());
        tagManager.updateById(req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('knowledge:tag:manage')")
    public Result<?> delete(@PathVariable Long id) {
        tagManager.removeById(id);
        return Result.ok();
    }
}
