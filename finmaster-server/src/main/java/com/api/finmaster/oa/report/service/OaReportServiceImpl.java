package com.qykj.finmaster.oa.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.oa.expense.domain.OaExpense;
import com.qykj.finmaster.oa.expense.domain.OaExpenseItem;
import com.qykj.finmaster.oa.expense.manager.impl.OaExpenseItemManagerImpl;
import com.qykj.finmaster.oa.expense.manager.impl.OaExpenseManagerImpl;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import com.qykj.finmaster.oa.payment.manager.impl.OaPaymentManagerImpl;
import com.qykj.finmaster.oa.report.domain.resp.ExpenseReportResp;
import com.qykj.finmaster.oa.report.domain.resp.PayrollReportResp;
import com.qykj.finmaster.oa.salary.domain.SalaryRecord;
import com.qykj.finmaster.oa.salary.domain.OaSocialInsuranceConfig;
import com.qykj.finmaster.oa.salary.manager.impl.SalaryRecordManagerImpl;
import com.qykj.finmaster.oa.salary.manager.impl.OaSocialInsuranceConfigManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OA报表服务实现类
 */
@Slf4j
@Service
public class OaReportServiceImpl {

    @Autowired
    private OaExpenseManagerImpl expenseManager;

    @Autowired
    private OaExpenseItemManagerImpl expenseItemManager;

    @Autowired
    private OaPaymentManagerImpl paymentManager;

    @Autowired
    private SalaryRecordManagerImpl salaryRecordManager;

    @Autowired
    private OaSocialInsuranceConfigManagerImpl socialInsuranceConfigManager;

    /**
     * 费用分析报表
     */
    public ExpenseReportResp getExpenseReport(String startDate, String endDate, Long tenantId) {
        ExpenseReportResp resp = new ExpenseReportResp();

        // 查询已审批/已付款的报销单
        LambdaQueryWrapper<OaExpense> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaExpense::getTenantId, tenantId);
        wrapper.in(OaExpense::getStatus, "APPROVED", "PAID");
        if (startDate != null) wrapper.ge(OaExpense::getCreateTime, startDate + " 00:00:00");
        if (endDate != null) wrapper.le(OaExpense::getCreateTime, endDate + " 23:59:59");
        List<OaExpense> expenses = expenseManager.list(wrapper);

        // 汇总
        BigDecimal totalAmount = expenses.stream()
                .map(OaExpense::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalAmount(totalAmount);
        resp.setTotalCount((long) expenses.size());
        resp.setAvgAmount(expenses.isEmpty() ? BigDecimal.ZERO :
                totalAmount.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP));

        // 按费用类型分组（从expense_item）
        List<Long> expenseIds = expenses.stream().map(OaExpense::getId).collect(Collectors.toList());
        if (!expenseIds.isEmpty()) {
            List<OaExpenseItem> items = expenseItemManager.list(
                    Wrappers.lambdaQuery(OaExpenseItem.class).in(OaExpenseItem::getExpenseId, expenseIds));
            Map<String, BigDecimal> byTypeMap = items.stream()
                    .collect(Collectors.groupingBy(
                            i -> i.getExpenseType() != null ? i.getExpenseType() : "其他",
                            Collectors.reducing(BigDecimal.ZERO, OaExpenseItem::getAmount, BigDecimal::add)));
            List<Map<String, Object>> byType = new ArrayList<>();
            byTypeMap.forEach((type, amount) -> {
                Map<String, Object> m = new HashMap<>();
                m.put("type", type);
                m.put("amount", amount);
                byType.add(m);
            });
            byType.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
            resp.setByType(byType);
        } else {
            resp.setByType(Collections.emptyList());
        }

        // 月度趋势
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        Map<String, BigDecimal> monthlyMap = new TreeMap<>();
        for (OaExpense e : expenses) {
            if (e.getCreateTime() != null) {
                String month = monthFmt.format(e.getCreateTime());
                monthlyMap.merge(month, e.getTotalAmount() != null ? e.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add);
            }
        }
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        monthlyMap.forEach((month, amount) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("month", month);
            m.put("amount", amount);
            monthlyTrend.add(m);
        });
        resp.setMonthlyTrend(monthlyTrend);

        // Top申请人
        Map<String, BigDecimal> applicantMap = expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getApplicantName() != null ? e.getApplicantName() : e.getApplicant(),
                        Collectors.reducing(BigDecimal.ZERO, e -> e.getTotalAmount() != null ? e.getTotalAmount() : BigDecimal.ZERO, BigDecimal::add)));
        List<Map<String, Object>> topApplicants = new ArrayList<>();
        applicantMap.forEach((name, amount) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("applicantName", name);
            m.put("amount", amount);
            topApplicants.add(m);
        });
        topApplicants.sort((a, b) -> ((BigDecimal) b.get("amount")).compareTo((BigDecimal) a.get("amount")));
        if (topApplicants.size() > 10) {
            resp.setTopApplicants(topApplicants.subList(0, 10));
        } else {
            resp.setTopApplicants(topApplicants);
        }

        // 按付款分类
        LambdaQueryWrapper<OaPayment> paymentWrapper = Wrappers.lambdaQuery();
        paymentWrapper.eq(OaPayment::getTenantId, tenantId);
        paymentWrapper.in(OaPayment::getStatus, "APPROVED", "PAID");
        if (startDate != null) paymentWrapper.ge(OaPayment::getCreateTime, startDate + " 00:00:00");
        if (endDate != null) paymentWrapper.le(OaPayment::getCreateTime, endDate + " 23:59:59");
        List<OaPayment> payments = paymentManager.list(paymentWrapper);

        Map<String, BigDecimal> categoryMap = payments.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentCategory() != null ? p.getPaymentCategory() : "OTHER",
                        Collectors.reducing(BigDecimal.ZERO, p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO, BigDecimal::add)));
        List<Map<String, Object>> byCategory = new ArrayList<>();
        categoryMap.forEach((category, amount) -> {
            Map<String, Object> m = new HashMap<>();
            m.put("category", category);
            m.put("amount", amount);
            byCategory.add(m);
        });
        resp.setByCategory(byCategory);

        return resp;
    }

    /**
     * 人力成本报表
     */
    public PayrollReportResp getPayrollReport(String startMonth, String endMonth, Long tenantId) {
        PayrollReportResp resp = new PayrollReportResp();

        // 查询工资记录
        LambdaQueryWrapper<SalaryRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SalaryRecord::getTenantId, tenantId);
        if (startMonth != null) wrapper.ge(SalaryRecord::getSalaryMonth, startMonth);
        if (endMonth != null) wrapper.le(SalaryRecord::getSalaryMonth, endMonth);
        wrapper.orderByAsc(SalaryRecord::getSalaryMonth);
        List<SalaryRecord> records = salaryRecordManager.list(wrapper);

        // 汇总
        BigDecimal totalPayroll = records.stream()
                .map(SalaryRecord::getActualSalary)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        resp.setTotalPayroll(totalPayroll);

        // 按月分组计算人数
        Map<String, List<SalaryRecord>> byMonth = records.stream()
                .collect(Collectors.groupingBy(SalaryRecord::getSalaryMonth, TreeMap::new, Collectors.toList()));

        int latestHeadcount = 0;
        BigDecimal latestTotal = BigDecimal.ZERO;

        // 月度趋势
        List<Map<String, Object>> payrollTrend = new ArrayList<>();
        for (Map.Entry<String, List<SalaryRecord>> entry : byMonth.entrySet()) {
            List<SalaryRecord> monthRecords = entry.getValue();
            BigDecimal monthTotal = monthRecords.stream()
                    .map(SalaryRecord::getActualSalary)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> m = new HashMap<>();
            m.put("month", entry.getKey());
            m.put("totalPayroll", monthTotal);
            m.put("headcount", monthRecords.size());
            payrollTrend.add(m);
            latestHeadcount = monthRecords.size();
            latestTotal = monthTotal;
        }
        resp.setPayrollTrend(payrollTrend);
        resp.setHeadcount(latestHeadcount);
        resp.setAvgSalary(latestHeadcount > 0 ?
                latestTotal.divide(BigDecimal.valueOf(latestHeadcount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        // 提成构成（底薪 vs 新签提成 vs 续费提成）
        List<Map<String, Object>> commBreakdown = new ArrayList<>();
        for (Map.Entry<String, List<SalaryRecord>> entry : byMonth.entrySet()) {
            List<SalaryRecord> monthRecords = entry.getValue();
            BigDecimal baseSalarySum = monthRecords.stream()
                    .map(SalaryRecord::getBaseSalary).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal newCommSum = monthRecords.stream()
                    .map(SalaryRecord::getNewCommission).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal renewCommSum = monthRecords.stream()
                    .map(SalaryRecord::getRenewCommission).filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> m = new HashMap<>();
            m.put("month", entry.getKey());
            m.put("baseSalary", baseSalarySum);
            m.put("newCommission", newCommSum);
            m.put("renewCommission", renewCommSum);
            commBreakdown.add(m);
        }
        resp.setCommissionBreakdown(commBreakdown);

        // 扣款分析
        BigDecimal totalLateDeduction = records.stream()
                .map(SalaryRecord::getLateDeduction).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAbsentDeduction = records.stream()
                .map(SalaryRecord::getAbsentDeduction).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOtherDeduction = records.stream()
                .map(SalaryRecord::getOtherDeduction).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Map<String, Object>> deductionAnalysis = new ArrayList<>();
        deductionAnalysis.add(Map.of("type", "迟到扣款", "amount", totalLateDeduction));
        deductionAnalysis.add(Map.of("type", "旷工扣款", "amount", totalAbsentDeduction));
        deductionAnalysis.add(Map.of("type", "其他扣款", "amount", totalOtherDeduction));
        resp.setDeductionAnalysis(deductionAnalysis);

        // 社保公积金成本
        BigDecimal totalSocialPersonal = records.stream()
                .map(SalaryRecord::getSocialInsurancePersonal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalHousingPersonal = records.stream()
                .map(SalaryRecord::getHousingFundPersonal).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 估算公司部分（从社保配置）
        List<OaSocialInsuranceConfig> insuranceConfigs = socialInsuranceConfigManager.list(
                Wrappers.lambdaQuery(OaSocialInsuranceConfig.class).eq(OaSocialInsuranceConfig::getTenantId, tenantId));
        BigDecimal totalCompanyInsurance = BigDecimal.ZERO;
        BigDecimal totalCompanyHousing = BigDecimal.ZERO;
        for (OaSocialInsuranceConfig cfg : insuranceConfigs) {
            if (cfg.getInsuranceBase() != null) {
                BigDecimal base = cfg.getInsuranceBase();
                BigDecimal companyRate = BigDecimal.ZERO;
                if (cfg.getEndowmentCompanyRate() != null) companyRate = companyRate.add(cfg.getEndowmentCompanyRate());
                if (cfg.getMedicalCompanyRate() != null) companyRate = companyRate.add(cfg.getMedicalCompanyRate());
                if (cfg.getUnemploymentCompanyRate() != null) companyRate = companyRate.add(cfg.getUnemploymentCompanyRate());
                totalCompanyInsurance = totalCompanyInsurance.add(base.multiply(companyRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                if (cfg.getHousingFundCompanyRate() != null) {
                    totalCompanyHousing = totalCompanyHousing.add(base.multiply(cfg.getHousingFundCompanyRate()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }
            }
        }

        List<Map<String, Object>> socialCosts = new ArrayList<>();
        socialCosts.add(Map.of("item", "社保(个人)", "amount", totalSocialPersonal));
        socialCosts.add(Map.of("item", "公积金(个人)", "amount", totalHousingPersonal));
        socialCosts.add(Map.of("item", "社保(公司)", "amount", totalCompanyInsurance));
        socialCosts.add(Map.of("item", "公积金(公司)", "amount", totalCompanyHousing));
        resp.setSocialInsuranceCosts(socialCosts);

        return resp;
    }
}
