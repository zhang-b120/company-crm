package com.qykj.finmaster.third.workWx.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.workWx.service.WorkWxCsOvertimeService;
import com.qykj.finmaster.third.workWx.service.WorkWxResponseMetricServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 企业微信群维度回复/超时监控查询（工单 76 / WXRESP-01）。纯只读，不关联客户。
 */
@RestController
@RequestMapping("/workWx/response")
public class WorkWxResponseController extends BaseController {

    @Autowired
    private WorkWxResponseMetricServiceImpl responseService;

    @Autowired
    private WorkWxCsOvertimeService csOvertimeService;

    private Long tenantId() {
        return getLoginUser() != null ? getLoginUser().getTenantId() : null;
    }

    private Date sinceDays(Integer days) {
        int d = (days == null || days <= 0) ? 7 : days;
        return new Date(System.currentTimeMillis() - d * 24L * 3600 * 1000);
    }

    /** 超时/未回复清单（红表，置顶用）。仅未回复 + 近 days 天（默认7），与客服监控口径一致。
     *  可选 roomId（指定群）、status（逗号分隔状态子集）筛选。 */
    @GetMapping("/unreplied")
    public AjaxResult unreplied(@RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
                                @RequestParam(value = "days", required = false) Integer days,
                                @RequestParam(value = "roomId", required = false) String roomId,
                                @RequestParam(value = "status", required = false) String status) {
        return AjaxResult.success(responseService.unreplied(tenantId(), sinceDays(days), roomId, splitStatus(status), limit));
    }

    /** 按群统计 */
    @GetMapping("/stats-by-group")
    public AjaxResult statsByGroup(@RequestParam(value = "days", required = false) Integer days) {
        return AjaxResult.success(responseService.statsByGroup(tenantId(), sinceDays(days)));
    }

    /** 按员工统计 */
    @GetMapping("/stats-by-member")
    public AjaxResult statsByMember(@RequestParam(value = "days", required = false) Integer days) {
        return AjaxResult.success(responseService.statsByMember(tenantId(), sinceDays(days)));
    }

    /** KPI。overdueUnreplied 跟随 days（默认7），仅未回复；其余三项按今日。 */
    @GetMapping("/kpi")
    public AjaxResult kpi(@RequestParam(value = "days", required = false) Integer days) {
        return AjaxResult.success(responseService.kpi(tenantId(), sinceDays(days)));
    }

    /**
     * 客服超时监控·汇总（WXRESP-12）：客服部每位员工一行——待处理超时数/24h高风险/最久等待/已回复/平均首响。
     * 仅聚合数字、不含聊天内容，登录用户即可看（租户隔离）。
     */
    @GetMapping("/cs-overtime/summary")
    public AjaxResult csOvertimeSummary(@RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        return AjaxResult.success(csOvertimeService.summary(tenantId(), days == null ? 7 : days));
    }

    /**
     * 客服超时监控·明细（WXRESP-12）：某客服名下所有超时未处理回合。含客户消息内容 →
     * 管理/主管可见任何人；客服本人可查自己的明细（WXRESP-14，超时弹窗"查看明细"直达）。
     */
    @GetMapping("/cs-overtime/detail")
    public AjaxResult csOvertimeDetail(@RequestParam("wecomUserid") String wecomUserid,
                                       @RequestParam(value = "days", required = false, defaultValue = "7") Integer days) {
        if (!canViewContent() && !csOvertimeService.isSelfAgent(tenantId(), currentUsername(), wecomUserid)) {
            return AjaxResult.error("无权限：仅管理员/主管或客服本人可查看该明细");
        }
        return AjaxResult.success(csOvertimeService.detail(tenantId(), wecomUserid, days == null ? 7 : days));
    }

    private String currentUsername() {
        LoginUser u = getLoginUser();
        return u != null ? u.getUsername() : null;
    }

    /** 含聊天内容的接口仅管理/主管可见（复用工单77 RBAC 口径）。 */
    private boolean canViewContent() {
        LoginUser u = getLoginUser();
        return u != null && (u.isAdmin() || u.isSuperAdmin());
    }

    /** 超时/未回复快捷状态集（onlyOverdue 用） */
    private static final List<String> OVERDUE_STATUSES =
            Arrays.asList("timeout", "serious_timeout", "unreplied_high_risk");

    /**
     * 会话回合明细（工单 82 / WXRESP-03，分页）。含聊天内容 → 限管理/主管。
     * status 逗号分隔多选；onlyOverdue=true 且未指定 status 时取超时/未回复集。
     */
    @GetMapping("/turns")
    public AjaxResult turns(@RequestParam(value = "days", required = false) Integer days,
                            @RequestParam(value = "startDate", required = false) String startDate,
                            @RequestParam(value = "endDate", required = false) String endDate,
                            @RequestParam(value = "roomId", required = false) String roomId,
                            @RequestParam(value = "memberUser", required = false) String memberUser,
                            @RequestParam(value = "customerUser", required = false) String customerUser,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "keyword", required = false) String keyword,
                            @RequestParam(value = "onlyOverdue", required = false, defaultValue = "false") Boolean onlyOverdue,
                            @RequestParam(value = "orderBy", required = false) String orderBy,
                            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
                            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        if (!canViewContent()) {
            return AjaxResult.error("无权限：会话回合明细含聊天内容，仅管理员/主管可见");
        }
        Date startTime = startDate != null ? parseDay(startDate, false) : sinceDays(days);
        Date endTime = endDate != null ? parseDay(endDate, true) : null;
        List<String> statuses = splitStatus(status);
        if ((statuses == null || statuses.isEmpty()) && Boolean.TRUE.equals(onlyOverdue)) {
            statuses = OVERDUE_STATUSES;
        }
        return AjaxResult.success(responseService.turns(tenantId(), startTime, endTime, roomId, memberUser,
                customerUser, statuses, keyword, orderBy, pageNum, pageSize));
    }

    /**
     * 会话上下文（工单 82 / WXRESP-04）：按 roomId + 时间窗口升序返回该群消息。含内容 → 限管理/主管。
     * centerTimeMs：该回合首条客户消息时间（epoch ms）。
     */
    @GetMapping("/conversation")
    public AjaxResult conversation(@RequestParam("roomId") String roomId,
                                   @RequestParam("centerTimeMs") Long centerTimeMs,
                                   @RequestParam(value = "beforeMinutes", required = false, defaultValue = "5") Integer beforeMinutes,
                                   @RequestParam(value = "afterMinutes", required = false, defaultValue = "120") Integer afterMinutes) {
        // 管理/主管可看任意群；客服本人可看自己所在群的会话（WXRESP-14，超时明细下钻）
        if (!canViewContent() && !csOvertimeService.isRoomMember(tenantId(), currentUsername(), roomId)) {
            return AjaxResult.error("无权限：会话内容仅管理员/主管或该群成员客服可见");
        }
        Date centerTime = centerTimeMs != null ? new Date(centerTimeMs) : null;
        return AjaxResult.success(responseService.conversation(tenantId(), roomId, centerTime, beforeMinutes, afterMinutes));
    }

    private List<String> splitStatus(String status) {
        if (StringUtils.isBlank(status)) return null;
        List<String> list = new ArrayList<>();
        for (String s : status.split(",")) {
            if (StringUtils.isNotBlank(s)) list.add(s.trim());
        }
        return list;
    }

    /** 解析 yyyy-MM-dd；endExclusive=true 时返回次日 0 点（用于 &lt; 上界）。解析失败返回 null。 */
    private Date parseDay(String day, boolean endExclusive) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd").parse(day.trim());
            return endExclusive ? new Date(d.getTime() + 24L * 3600 * 1000) : d;
        } catch (Exception e) {
            return null;
        }
    }
}
