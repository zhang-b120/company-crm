package com.qykj.finmaster.analytics.common.context;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.enums.AnalyticsScope;
import com.qykj.finmaster.analytics.common.enums.CompareMode;
import com.qykj.finmaster.analytics.common.enums.TimeRangeType;
import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver.ScopeResult;
import com.qykj.finmaster.analytics.common.service.TimeRangeResolver;
import com.qykj.finmaster.analytics.common.service.TimeRangeResolver.Range;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 由 LoginUser + 请求参数构建 AnalyticsContext
 * 所有统计 Service 第一步都走这里
 */
@Component
public class AnalyticsContextBuilder {

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    @Autowired
    private TimeRangeResolver timeRangeResolver;

    public AnalyticsContext build(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = new AnalyticsContext();
        ctx.setTenantId(user == null ? null : user.getTenantId());
        ctx.setCurrentUsername(user == null ? null : user.getUsername());
        ctx.setCurrentUserId(user == null ? null : user.getUserId());

        // 1. 解析层级（workbenchType 可为 null，表示走数据中心原逻辑）
        String workbenchType = req == null ? null : req.getWorkbenchType();
        AnalyticsScope requested = AnalyticsScope.fromString(req == null ? null : req.getScope());
        AnalyticsScope max = scopeResolver.maxAllowedScope(user, workbenchType);
        AnalyticsScope effective = scopeResolver.cap(max, requested);
        ctx.setScope(effective);

        // 2. 根据层级解析 userNameList / userIdList / deptIdList
        ScopeResult sr;
        switch (effective) {
            case COMPANY:
                sr = new ScopeResult(null, null, null);
                break;
            case TEAM:
                sr = scopeResolver.resolveTeamUsers(user, workbenchType);
                break;
            case PERSONAL:
            default:
                sr = scopeResolver.resolvePersonal(user);
                break;
        }
        ctx.setUserNameList(sr.getUserNameList());
        ctx.setUserIdList(sr.getUserIdList());
        ctx.setDeptIdList(sr.getDeptIdList());

        // 3. 时间范围
        TimeRangeType trt = TimeRangeType.fromString(req == null ? null : req.getTimeRangeType());
        ctx.setTimeRangeType(trt);
        Range current = timeRangeResolver.resolveCurrent(req == null ? new AnalyticsQueryReq() : req);
        ctx.setTimeStart(current.getStart());
        ctx.setTimeEnd(current.getEnd());

        // 4. 对比期
        CompareMode cm = CompareMode.fromString(req == null ? null : req.getCompareWith());
        ctx.setCompareMode(cm);
        Range prev = timeRangeResolver.resolvePrevious(current, cm);
        if (prev != null) {
            ctx.setPrevTimeStart(prev.getStart());
            ctx.setPrevTimeEnd(prev.getEnd());
        }

        return ctx;
    }
}
