package com.qykj.finmaster.third.workWx.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.workWx.entity.WorkWxKeywordRule;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageKeywordHit;
import com.qykj.finmaster.third.workWx.manager.WorkWxKeywordRuleManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageKeywordHitManager;
import com.qykj.finmaster.third.workWx.service.WorkWxKeywordHitQueryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 企业微信关键词规则配置 + 命中清单（工单 77 / WXKW-01）。
 * 敏感类（sensitive=1）命中仅管理员/超管可见（复用 RBAC）。纯只读消息、不生成工单/商机。
 */
@RestController
@RequestMapping("/workWx/keyword")
public class WorkWxKeywordController extends BaseController {

    private static final List<String> RULE_TYPES = Arrays.asList(
            "RENEWAL", "BUSINESS", "TAX_RISK", "COMPLAINT", "PRIVATE_DEAL", "INVOICE");

    @Autowired
    private WorkWxKeywordRuleManager ruleManager;
    @Autowired
    private WorkWxMessageKeywordHitManager hitManager;
    @Autowired
    private WorkWxKeywordHitQueryServiceImpl hitQueryService;

    private Long tenantId() {
        return getLoginUser() != null ? getLoginUser().getTenantId() : null;
    }

    /** 当前用户是否可见敏感命中（管理员/超管） */
    private boolean canSeeSensitive() {
        LoginUser u = getLoginUser();
        return u != null && (u.isAdmin() || u.isSuperAdmin());
    }

    /** 规则写权限：仅管理员/超管可改/停规则（与 canSeeSensitive 一致；后续可纳入"主管"角色/权限码 wecom:keyword:edit） */
    private boolean canEditRule() {
        return canSeeSensitive();
    }

    /** 规则列表 */
    @GetMapping("/rules")
    public AjaxResult rules() {
        return AjaxResult.success(ruleManager.lambdaQuery()
                .orderByAsc(WorkWxKeywordRule::getSort)
                .list());
    }

    /** 新增/修改规则（仅管理员/超管，防任何登录用户改/停飞单检测规则） */
    @PostMapping("/rules")
    public AjaxResult saveRule(@RequestBody WorkWxKeywordRule rule) {
        if (!canEditRule()) return AjaxResult.error("无权限：仅管理员可配置关键词规则");
        if (rule.getMatchMode() == null) rule.setMatchMode("CONTAINS");
        if (rule.getEnabled() == null) rule.setEnabled(1);
        if (rule.getSensitiveFlag() == null) rule.setSensitiveFlag(0);
        if (rule.getTenantId() == null) rule.setTenantId(tenantId());
        ruleManager.saveOrUpdate(rule);
        return AjaxResult.success(rule);
    }

    /** 启停规则（仅管理员/超管） */
    @PostMapping("/rules/{id}/status")
    public AjaxResult setStatus(@PathVariable("id") Long id, @RequestParam("enabled") Integer enabled) {
        if (!canEditRule()) return AjaxResult.error("无权限：仅管理员可启停关键词规则");
        WorkWxKeywordRule rule = ruleManager.getById(id);
        if (rule == null) return AjaxResult.error("规则不存在");
        rule.setEnabled(enabled != null && enabled == 1 ? 1 : 0);
        ruleManager.updateById(rule);
        return AjaxResult.success();
    }

    /**
     * 命中清单（按 类型/群名/时间 分页；敏感命中对非管理员过滤）。
     * 控制器只做鉴权 + 取参 + 调 service；enrich/筛选/分页在 {@link WorkWxKeywordHitQueryServiceImpl}。
     */
    @GetMapping("/hits")
    public AjaxResult hits(@RequestParam(value = "ruleType", required = false) String ruleType,
                           @RequestParam(value = "roomId", required = false) String roomId,
                           @RequestParam(value = "chatName", required = false) String chatName,
                           @RequestParam(value = "start", required = false) String start,
                           @RequestParam(value = "end", required = false) String end,
                           @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                           @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return AjaxResult.success(hitQueryService.hits(
                tenantId(), canSeeSensitive(), ruleType, chatName, roomId, start, end, pageNum, pageSize));
    }

    /** 命中概览：按规则类型计数。与 /hits 口径一致——非管理员一律 eq(sensitive,0) 过滤（而非按类型硬排除）。 */
    @GetMapping("/hits/summary")
    public AjaxResult summary() {
        List<Map<String, Object>> list = new ArrayList<>();
        boolean canSensitive = canSeeSensitive();
        for (String t : RULE_TYPES) {
            com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper<WorkWxMessageKeywordHit> w =
                    hitManager.lambdaQuery()
                            .eq(WorkWxMessageKeywordHit::getTenantId, tenantId())
                            .eq(WorkWxMessageKeywordHit::getRuleType, t);
            if (!canSensitive) w.eq(WorkWxMessageKeywordHit::getSensitiveFlag, 0);
            long c = w.count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ruleType", t);
            m.put("count", c);
            list.add(m);
        }
        return AjaxResult.success(list);
    }
}
