package com.qykj.finmaster.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.mapper.CustomerInfoMapper;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.mapper.LeadMapper;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workbench.dto.WorkbenchTodoResp;
import com.qykj.finmaster.workorder.domain.WorkOrder;
import com.qykj.finmaster.workorder.mapper.WorkOrderMapper;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工作台"我的待办"聚合（右下角面板）
 *
 * 不依赖 common_task（按团队规约：common_task / cs/task-list 不动）；
 * 待办来源 = work_order + sale_lead.next_follow_time + customer_info.next_follow_time
 */
@Service
public class WorkbenchTodoService {

    private static final int LIST_LIMIT = 20;

    @Autowired
    private WorkOrderMapper workOrderMapper;

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private SysBizConfigService bizConfigService;

    private static final int RECYCLE_WARN_DAYS = 3;

    public WorkbenchTodoResp getMyTodo(LoginUser user) {
        WorkbenchTodoResp resp = new WorkbenchTodoResp();
        if (user == null || user.getTenantId() == null || user.getUsername() == null) {
            return resp;
        }
        Long tenantId = user.getTenantId();
        String username = user.getUsername();

        LocalDate today = LocalDate.now();
        Date dayStart = toDate(today.atStartOfDay());
        Date dayEnd = toDate(today.plusDays(1).atStartOfDay());

        // ─── 任务单 ────────────────────────────────────────────────────────
        List<String> activeStatus = Arrays.asList("PENDING", "RUNNING", "WAITING_CHILD", "SUSPENDED");
        List<WorkOrder> wos = workOrderMapper.selectList(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getTenantId, tenantId)
                .and(w -> w.eq(WorkOrder::getCurrentOwnerUser, username)
                        .or().eq(WorkOrder::getOwnerUser, username))
                .in(WorkOrder::getStatus, activeStatus)
                .orderByAsc(WorkOrder::getDueTime)
                .last("LIMIT " + LIST_LIMIT));
        Date now = new Date();
        if (wos != null) {
            for (WorkOrder wo : wos) {
                WorkbenchTodoResp.WorkOrderItem item = new WorkbenchTodoResp.WorkOrderItem();
                item.setId(wo.getId());
                item.setTaskNo(wo.getTaskNo());
                item.setTitle(wo.getTitle());
                item.setTaskType(wo.getTaskType());
                item.setStatus(wo.getStatus());
                item.setDueTime(wo.getDueTime());
                item.setOverdue(wo.getDueTime() != null && wo.getDueTime().before(now));
                resp.getWorkOrders().add(item);
            }
        }
        Long woCount = workOrderMapper.selectCount(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getTenantId, tenantId)
                .and(w -> w.eq(WorkOrder::getCurrentOwnerUser, username)
                        .or().eq(WorkOrder::getOwnerUser, username))
                .in(WorkOrder::getStatus, activeStatus));
        resp.getCounts().setWorkOrder(woCount == null ? 0L : woCount);

        // ─── 线索：今日约定 + 即将回收 (≤3 天)，按 id 去重合并 ────────────
        int leadNoFollowDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        Map<Long, WorkbenchTodoResp.FollowUpItem> leadMap = new LinkedHashMap<>();

        // 1) 今日约定
        List<Lead> apptLeads = leadMapper.selectList(new LambdaQueryWrapper<Lead>()
                .eq(Lead::getTenantId, tenantId)
                .eq(Lead::getOwnerUser, username)
                .in(Lead::getStatus, Arrays.asList(1, 2))
                .isNotNull(Lead::getNextFollowTime)
                .ge(Lead::getNextFollowTime, today.atStartOfDay())
                .lt(Lead::getNextFollowTime, today.plusDays(1).atStartOfDay())
                .orderByAsc(Lead::getNextFollowTime)
                .last("LIMIT " + LIST_LIMIT));
        if (apptLeads != null) {
            for (Lead l : apptLeads) {
                leadMap.put(l.getId(), buildLeadItem(l, "APPOINTMENT", null));
            }
        }
        // 2) 即将回收（距 noFollowDays 阈值剩余 ≤ RECYCLE_WARN_DAYS）
        List<Lead> recycleLeads = leadMapper.selectList(new LambdaQueryWrapper<Lead>()
                .eq(Lead::getTenantId, tenantId)
                .eq(Lead::getOwnerUser, username)
                .in(Lead::getStatus, Arrays.asList(1, 2))
                .last("AND DATEDIFF(NOW(), COALESCE(last_follow_time, create_time)) BETWEEN " +
                        Math.max(leadNoFollowDays - RECYCLE_WARN_DAYS, 0) + " AND " + leadNoFollowDays +
                        " ORDER BY COALESCE(last_follow_time, create_time) ASC LIMIT " + LIST_LIMIT));
        if (recycleLeads != null) {
            for (Lead l : recycleLeads) {
                Integer remain = computeRecycleDays(l.getLastFollowTime(), l.getCreateTime(), leadNoFollowDays);
                if (leadMap.containsKey(l.getId())) {
                    // 既约定又即将回收，优先标记 RECYCLE 但保留约定字段
                    WorkbenchTodoResp.FollowUpItem existing = leadMap.get(l.getId());
                    existing.setTagType("RECYCLE");
                    existing.setRecycleDays(remain);
                } else {
                    leadMap.put(l.getId(), buildLeadItem(l, "RECYCLE", remain));
                }
            }
        }
        resp.getLeadFollowUps().addAll(leadMap.values());
        resp.getCounts().setLeadFollowUp((long) leadMap.size());

        // ─── 客户：今日约定 + 即将回收 ──────────────────────────────────
        int custNoFollowDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        Map<Long, WorkbenchTodoResp.FollowUpItem> custMap = new LinkedHashMap<>();

        List<CustomerInfo> apptCusts = customerInfoMapper.selectList(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getTenantId, tenantId)
                .eq(CustomerInfo::getOwnerUser, username)
                .eq(CustomerInfo::getCustomerType, 1)
                .isNotNull(CustomerInfo::getNextFollowTime)
                .ge(CustomerInfo::getNextFollowTime, dayStart)
                .lt(CustomerInfo::getNextFollowTime, dayEnd)
                .orderByAsc(CustomerInfo::getNextFollowTime)
                .last("LIMIT " + LIST_LIMIT));
        if (apptCusts != null) {
            for (CustomerInfo c : apptCusts) {
                custMap.put(c.getId(), buildCustomerItem(c, "APPOINTMENT", null));
            }
        }
        List<CustomerInfo> recycleCusts = customerInfoMapper.selectList(new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getTenantId, tenantId)
                .eq(CustomerInfo::getOwnerUser, username)
                .eq(CustomerInfo::getCustomerType, 1)
                .last("AND DATEDIFF(NOW(), COALESCE(last_follow_time, create_time)) BETWEEN " +
                        Math.max(custNoFollowDays - RECYCLE_WARN_DAYS, 0) + " AND " + custNoFollowDays +
                        " ORDER BY COALESCE(last_follow_time, create_time) ASC LIMIT " + LIST_LIMIT));
        if (recycleCusts != null) {
            for (CustomerInfo c : recycleCusts) {
                Integer remain = computeRecycleDays(c.getLastFollowTime(), c.getCreateTime(), custNoFollowDays);
                if (custMap.containsKey(c.getId())) {
                    WorkbenchTodoResp.FollowUpItem existing = custMap.get(c.getId());
                    existing.setTagType("RECYCLE");
                    existing.setRecycleDays(remain);
                } else {
                    custMap.put(c.getId(), buildCustomerItem(c, "RECYCLE", remain));
                }
            }
        }
        resp.getCustomerFollowUps().addAll(custMap.values());
        resp.getCounts().setCustomerFollowUp((long) custMap.size());

        return resp;
    }

    private WorkbenchTodoResp.FollowUpItem buildLeadItem(Lead l, String tag, Integer recycleDays) {
        WorkbenchTodoResp.FollowUpItem it = new WorkbenchTodoResp.FollowUpItem();
        it.setId(l.getId());
        it.setName(l.getLeadName());
        it.setContactName(l.getContactName());
        it.setPhone(l.getPhone());
        if (l.getNextFollowTime() != null) it.setNextFollowTime(toDate(l.getNextFollowTime()));
        it.setLastFollowContent(l.getLastFollowContent());
        it.setTagType(tag);
        it.setRecycleDays(recycleDays);
        return it;
    }

    private WorkbenchTodoResp.FollowUpItem buildCustomerItem(CustomerInfo c, String tag, Integer recycleDays) {
        WorkbenchTodoResp.FollowUpItem it = new WorkbenchTodoResp.FollowUpItem();
        it.setId(c.getId());
        it.setName(c.getCustomerName());
        it.setContactName(c.getContactName());
        it.setPhone(c.getPhone());
        it.setNextFollowTime(c.getNextFollowTime());
        it.setLastFollowContent(c.getLastFollowContent());
        it.setTagType(tag);
        it.setRecycleDays(recycleDays);
        return it;
    }

    /** 距自动回收剩余天数：noFollowDays - 距上次活动时间已过天数（≥0） */
    private static Integer computeRecycleDays(Date lastFollow, Date createTime, int noFollowDays) {
        Date base = lastFollow != null ? lastFollow : createTime;
        if (base == null) return null;
        long passed = (System.currentTimeMillis() - base.getTime()) / (24L * 3600L * 1000L);
        long remain = Math.max(noFollowDays - passed, 0);
        return (int) remain;
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
