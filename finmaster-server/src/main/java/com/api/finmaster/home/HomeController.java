package com.qykj.finmaster.home;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.impl.ContractManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.manager.impl.CommonTaskManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 首页角色工作台聚合数据（Sprint 4 M4）
 *
 * 用一个接口根据当前用户角色返回对应工作台的卡片数据
 * GET /home/summary  返回：{ role, ...字段 }
 *
 * 各角色的字段不同，前端 Dashboard 按角色取自己关心的
 */
@RestController
@RequestMapping("/home")
public class HomeController extends BaseController {

    @Autowired private LeadManagerImpl leadManager;
    @Autowired private OrderManagerImpl orderManager;
    @Autowired private ContractManagerImpl contractManager;
    @Autowired private CommonTaskManagerImpl commonTaskManager;
    @Autowired private CompanyServiceManagerImpl companyServiceManager;

    @GetMapping("/summary")
    public AjaxResult getSummary() {
        LoginUser loginUser = getLoginUser();
        Map<String, Object> ret = new LinkedHashMap<>();

        if (loginUser == null) {
            return AjaxResult.success(ret);
        }
        String userName = loginUser.getUsername();

        LocalDate today = LocalDate.now();
        Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date weekStart = Date.from(today.with(java.time.DayOfWeek.MONDAY).atStartOfDay(ZoneId.systemDefault()).toInstant());
        LocalDateTime nowDt = LocalDateTime.now();

        // ===== 销售视角 =====
        long todayFollow = leadManager.lambdaQuery()
                .eq(Lead::getOwnerUser, userName)
                .ge(Lead::getNextFollowTime, today.atStartOfDay())
                .le(Lead::getNextFollowTime, today.atTime(23, 59, 59))
                .count();

        long overdue = leadManager.lambdaQuery()
                .eq(Lead::getOwnerUser, userName)
                .in(Lead::getStatus, GlobalConstant.Number.ONE_INT, GlobalConstant.Number.TWO_INT)
                .isNotNull(Lead::getLastFollowTime)
                .lt(Lead::getLastFollowTime, Date.from(today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .count();

        long newAssignedThisWeek = leadManager.lambdaQuery()
                .eq(Lead::getOwnerUser, userName)
                .ge(Lead::getCreateTime, weekStart)
                .count();

        long weekDeals = orderManager.lambdaQuery()
                .eq(Order::getStatus, "PAID")
                .ge(Order::getPayTime, weekStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime())
                .count();

        Map<String, Object> sale = new LinkedHashMap<>();
        sale.put("todayFollow", todayFollow);
        sale.put("overdue", overdue);
        sale.put("newAssignedThisWeek", newAssignedThisWeek);
        sale.put("weekDeals", weekDeals);
        ret.put("sale", sale);

        // ===== 客户部视角 =====
        long myProcessing = commonTaskManager.lambdaQuery()
                .eq(CommonTask::getCurrentOwnerUser, userName)
                .in(CommonTask::getStatus, "PENDING", "PROCESSING")
                .count();

        long myStuck = commonTaskManager.lambdaQuery()
                .eq(CommonTask::getCurrentOwnerUser, userName)
                .in(CommonTask::getStatus, "PENDING", "PROCESSING")
                .lt(CommonTask::getUpdateTime, Date.from(today.minusDays(2).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                .count();

        long myRenewalPending = companyServiceManager.lambdaQuery()
                .eq(CompanyService::getPendingRenewalAssignee, userName)
                .count();

        Map<String, Object> cs = new LinkedHashMap<>();
        cs.put("processing", myProcessing);
        cs.put("stuck2d", myStuck);
        cs.put("renewalPending", myRenewalPending);
        ret.put("cs", cs);

        // ===== 市场部视角 =====
        long todayNewLeads = leadManager.lambdaQuery()
                .ge(Lead::getCreateTime, todayStart)
                .count();
        long todayWxLeads = leadManager.lambdaQuery()
                .ge(Lead::getCreateTime, todayStart)
                .eq(Lead::getSource, "small_produce")
                .count();
        long unassigned = leadManager.lambdaQuery()
                .and(qw -> qw.isNull(Lead::getOwnerUser).or().eq(Lead::getOwnerUser, ""))
                .in(Lead::getStatus, GlobalConstant.Number.ONE_INT, GlobalConstant.Number.TWO_INT)
                .count();
        long unassignedOver1h = leadManager.lambdaQuery()
                .and(qw -> qw.isNull(Lead::getOwnerUser).or().eq(Lead::getOwnerUser, ""))
                .lt(Lead::getCreateTime, Date.from(nowDt.minusHours(1).atZone(ZoneId.systemDefault()).toInstant()))
                .in(Lead::getStatus, GlobalConstant.Number.ONE_INT, GlobalConstant.Number.TWO_INT)
                .count();

        Map<String, Object> market = new LinkedHashMap<>();
        market.put("todayNew", todayNewLeads);
        market.put("todayWx", todayWxLeads);
        market.put("todayManual", todayNewLeads - todayWxLeads);
        market.put("unassigned", unassigned);
        market.put("unassignedOver1h", unassignedOver1h);
        ret.put("market", market);

        // ===== 财务视角（轻量） =====
        long pendingPaymentOrders = orderManager.lambdaQuery()
                .eq(Order::getStatus, "PENDING")
                .count();
        long pendingRefundOrders = orderManager.lambdaQuery()
                .eq(Order::getStatus, "REFUNDING")
                .count();
        long draftContracts = contractManager.lambdaQuery()
                .eq(Contract::getStatus, "DRAFT")
                .count();
        Map<String, Object> finance = new LinkedHashMap<>();
        finance.put("pendingPayment", pendingPaymentOrders);
        finance.put("pendingRefund", pendingRefundOrders);
        finance.put("draftContracts", draftContracts);
        ret.put("finance", finance);

        return AjaxResult.success(ret);
    }

}
