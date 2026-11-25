package com.qykj.finmaster.oa.salary.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.oa.salary.domain.OaDeductionRule;
import com.qykj.finmaster.oa.salary.domain.OaSocialInsuranceConfig;
import com.qykj.finmaster.oa.salary.domain.SalaryConfig;
import com.qykj.finmaster.oa.salary.domain.SalaryRecord;
import com.qykj.finmaster.oa.salary.domain.req.SaveSalaryConfigReq;
import com.qykj.finmaster.oa.salary.manager.impl.OaDeductionRuleManagerImpl;
import com.qykj.finmaster.oa.salary.manager.impl.OaSocialInsuranceConfigManagerImpl;
import com.qykj.finmaster.oa.salary.manager.impl.SalaryConfigManagerImpl;
import com.qykj.finmaster.oa.salary.manager.impl.SalaryRecordManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 薪资服务实现类
 */
@Slf4j
@Service
public class SalaryServiceImpl {

    @Autowired
    private SalaryConfigManagerImpl salaryConfigManager;

    @Autowired
    private SalaryRecordManagerImpl salaryRecordManager;

    @Autowired
    private OaSocialInsuranceConfigManagerImpl socialInsuranceConfigManager;

    @Autowired
    private OaDeductionRuleManagerImpl deductionRuleManager;

    @Autowired
    private OrderManagerImpl orderManager;

    @Autowired
    private OrderDetailManagerImpl orderDetailManager;

    /**
     * 保存薪资配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveSalaryConfig(SaveSalaryConfigReq req, Long tenantId, LoginUser loginUser) {
        SalaryConfig config;
        if (req.getId() != null) {
            config = salaryConfigManager.getById(req.getId());
            if (config == null) {
                throw new BusinessException("薪资配置不存在");
            }
            config.setUpdateBy(loginUser.getUsername());
            config.setUpdateByName(loginUser.getRealName());
            config.setUpdateTime(new Date());
        } else {
            // 检查是否已存在该用户的配置
            LambdaQueryWrapper<SalaryConfig> checkWrapper = Wrappers.lambdaQuery();
            checkWrapper.eq(SalaryConfig::getTenantId, tenantId);
            checkWrapper.eq(SalaryConfig::getUserId, req.getUserId());
            SalaryConfig existing = salaryConfigManager.getOne(checkWrapper);
            if (existing != null) {
                throw new BusinessException("该用户的薪资配置已存在，请使用更新操作");
            }

            config = new SalaryConfig();
            config.setTenantId(tenantId);
            config.setCreateBy(loginUser.getUsername());
            config.setCreateByName(loginUser.getRealName());
            config.setCreateTime(new Date());
        }

        config.setUserId(req.getUserId());
        config.setUsername(req.getUsername());
        config.setRealName(req.getRealName());
        config.setBaseSalary(req.getBaseSalary());
        config.setNewRate(req.getNewRate());
        config.setRenewRate(req.getRenewRate());

        if (req.getId() != null) {
            salaryConfigManager.updateById(config);
        } else {
            salaryConfigManager.save(config);
        }

        log.info("保存薪资配置成功，ID: {}，用户: {}", config.getId(), req.getUsername());
        return config.getId();
    }

    /**
     * 获取薪资配置列表
     */
    public List<SalaryConfig> getSalaryConfigList(Long tenantId) {
        LambdaQueryWrapper<SalaryConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SalaryConfig::getTenantId, tenantId);
        wrapper.orderByAsc(SalaryConfig::getUsername);
        return salaryConfigManager.list(wrapper);
    }

    /**
     * 删除薪资配置
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSalaryConfig(Long id) {
        return salaryConfigManager.removeById(id);
    }

    /**
     * 月度薪资核算
     */
    @Transactional(rollbackFor = Exception.class)
    public List<SalaryRecord> calculateMonthly(String salaryMonth, Long tenantId, LoginUser loginUser) {
        // 解析月份
        YearMonth yearMonth = YearMonth.parse(salaryMonth, DateTimeFormatter.ofPattern("yyyy-MM"));
        LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        // 获取所有薪资配置
        LambdaQueryWrapper<SalaryConfig> configWrapper = Wrappers.lambdaQuery();
        configWrapper.eq(SalaryConfig::getTenantId, tenantId);
        List<SalaryConfig> configs = salaryConfigManager.list(configWrapper);

        if (configs.isEmpty()) {
            throw new BusinessException("未找到薪资配置数据");
        }

        List<SalaryRecord> records = new ArrayList<>();

        for (SalaryConfig config : configs) {
            // 查询该员工在本月的已支付订单
            LambdaQueryWrapper<Order> orderWrapper = Wrappers.lambdaQuery();
            orderWrapper.eq(Order::getTenantId, tenantId);
            orderWrapper.eq(Order::getCreateBy, config.getUsername());
            orderWrapper.eq(Order::getStatus, "PAID");
            orderWrapper.ge(Order::getPayTime, monthStart);
            orderWrapper.le(Order::getPayTime, monthEnd);
            List<Order> orders = orderManager.list(orderWrapper);

            BigDecimal newOrderAmount = BigDecimal.ZERO;
            BigDecimal renewOrderAmount = BigDecimal.ZERO;

            if (!orders.isEmpty()) {
                // 获取所有订单ID
                List<Long> orderIds = orders.stream()
                        .map(Order::getId)
                        .collect(Collectors.toList());

                // 查询订单明细
                LambdaQueryWrapper<OrderDetail> detailWrapper = Wrappers.lambdaQuery();
                detailWrapper.in(OrderDetail::getOrderId, orderIds);
                List<OrderDetail> details = orderDetailManager.list(detailWrapper);

                // 按类型汇总金额
                for (OrderDetail detail : details) {
                    if ("NEW".equals(detail.getOrderDetailType())) {
                        newOrderAmount = newOrderAmount.add(
                                detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                    } else if ("RENEW".equals(detail.getOrderDetailType())) {
                        renewOrderAmount = renewOrderAmount.add(
                                detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO);
                    }
                }
            }

            // 计算提成
            BigDecimal newRate = config.getNewRate() != null ? config.getNewRate() : BigDecimal.ZERO;
            BigDecimal renewRate = config.getRenewRate() != null ? config.getRenewRate() : BigDecimal.ZERO;

            BigDecimal newCommission = newOrderAmount
                    .multiply(newRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal renewCommission = renewOrderAmount
                    .multiply(renewRate)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            BigDecimal baseSalary = config.getBaseSalary() != null ? config.getBaseSalary() : BigDecimal.ZERO;

            // 查询扣款规则（迟到/旷工扣款，本期暂无考勤数据来源，默认为0）
            BigDecimal lateDeductionAmount = BigDecimal.ZERO;
            BigDecimal absentDeductionAmount = BigDecimal.ZERO;
            int lateCount = 0;
            int absentCount = 0;

            // 查询社保配置
            LambdaQueryWrapper<OaSocialInsuranceConfig> insWrapper = Wrappers.lambdaQuery();
            insWrapper.eq(OaSocialInsuranceConfig::getTenantId, tenantId);
            insWrapper.eq(OaSocialInsuranceConfig::getUserId, config.getUserId());
            OaSocialInsuranceConfig insConfig = socialInsuranceConfigManager.getOne(insWrapper);

            BigDecimal socialInsurancePersonal = BigDecimal.ZERO;
            BigDecimal housingFundPersonal = BigDecimal.ZERO;

            if (insConfig != null) {
                BigDecimal base = insConfig.getInsuranceBase() != null ? insConfig.getInsuranceBase() : baseSalary;
                BigDecimal endowmentRate = insConfig.getEndowmentPersonalRate() != null ? insConfig.getEndowmentPersonalRate() : new BigDecimal("0.08");
                BigDecimal medicalRate = insConfig.getMedicalPersonalRate() != null ? insConfig.getMedicalPersonalRate() : new BigDecimal("0.02");
                BigDecimal unemploymentRate = insConfig.getUnemploymentPersonalRate() != null ? insConfig.getUnemploymentPersonalRate() : new BigDecimal("0.005");
                BigDecimal housingRate = insConfig.getHousingFundPersonalRate() != null ? insConfig.getHousingFundPersonalRate() : new BigDecimal("0.07");

                socialInsurancePersonal = base.multiply(endowmentRate.add(medicalRate).add(unemploymentRate))
                        .setScale(2, RoundingMode.HALF_UP);
                housingFundPersonal = base.multiply(housingRate).setScale(2, RoundingMode.HALF_UP);
            }

            // 应发工资 = 基本 + 提成 - 迟到扣款 - 旷工扣款
            BigDecimal grossSalary = baseSalary.add(newCommission).add(renewCommission)
                    .subtract(lateDeductionAmount).subtract(absentDeductionAmount);

            // 实发工资 = 应发 - 个人社保 - 个人公积金
            BigDecimal actualSalary = grossSalary.subtract(socialInsurancePersonal).subtract(housingFundPersonal);
            if (actualSalary.compareTo(BigDecimal.ZERO) < 0) {
                actualSalary = BigDecimal.ZERO;
            }

            BigDecimal totalSalary = grossSalary;

            // 查找或创建薪资记录
            LambdaQueryWrapper<SalaryRecord> recordWrapper = Wrappers.lambdaQuery();
            recordWrapper.eq(SalaryRecord::getTenantId, tenantId);
            recordWrapper.eq(SalaryRecord::getUserId, config.getUserId());
            recordWrapper.eq(SalaryRecord::getSalaryMonth, salaryMonth);
            SalaryRecord existingRecord = salaryRecordManager.getOne(recordWrapper);

            SalaryRecord record;
            if (existingRecord != null) {
                record = existingRecord;
                record.setUpdateBy(loginUser.getUsername());
                record.setUpdateByName(loginUser.getRealName());
                record.setUpdateTime(new Date());
            } else {
                record = new SalaryRecord();
                record.setTenantId(tenantId);
                record.setUserId(config.getUserId());
                record.setUsername(config.getUsername());
                record.setRealName(config.getRealName());
                record.setSalaryMonth(salaryMonth);
                record.setCreateBy(loginUser.getUsername());
                record.setCreateByName(loginUser.getRealName());
                record.setCreateTime(new Date());
            }

            record.setBaseSalary(baseSalary);
            record.setNewOrderAmount(newOrderAmount);
            record.setRenewOrderAmount(renewOrderAmount);
            record.setNewCommission(newCommission);
            record.setRenewCommission(renewCommission);
            record.setTotalSalary(totalSalary);
            record.setLateCount(lateCount);
            record.setAbsentCount(absentCount);
            record.setLateDeduction(lateDeductionAmount);
            record.setAbsentDeduction(absentDeductionAmount);
            record.setOtherDeduction(BigDecimal.ZERO);
            record.setSocialInsurancePersonal(socialInsurancePersonal);
            record.setHousingFundPersonal(housingFundPersonal);
            record.setTaxAmount(BigDecimal.ZERO);
            record.setGrossSalary(grossSalary);
            record.setActualSalary(actualSalary);
            record.setPayslipSent(record.getPayslipSent() != null ? record.getPayslipSent() : 0);
            record.setStatus("DRAFT");

            if (existingRecord != null) {
                salaryRecordManager.updateById(record);
            } else {
                salaryRecordManager.save(record);
            }

            records.add(record);
        }

        log.info("月度薪资核算完成，月份: {}，共计 {} 条记录", salaryMonth, records.size());
        return records;
    }

    /**
     * 获取薪资记录列表
     */
    public List<SalaryRecord> getSalaryRecordList(String salaryMonth, Long tenantId) {
        LambdaQueryWrapper<SalaryRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SalaryRecord::getTenantId, tenantId);
        if (salaryMonth != null && !salaryMonth.isEmpty()) {
            wrapper.eq(SalaryRecord::getSalaryMonth, salaryMonth);
        }
        wrapper.orderByAsc(SalaryRecord::getUsername);
        return salaryRecordManager.list(wrapper);
    }

    // ==================== 社保配置 ====================

    public List<OaSocialInsuranceConfig> getSocialInsuranceConfigList(Long tenantId) {
        LambdaQueryWrapper<OaSocialInsuranceConfig> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaSocialInsuranceConfig::getTenantId, tenantId);
        wrapper.orderByAsc(OaSocialInsuranceConfig::getUsername);
        return socialInsuranceConfigManager.list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long saveSocialInsuranceConfig(OaSocialInsuranceConfig req, Long tenantId) {
        req.setTenantId(tenantId);
        req.setUpdateTime(new Date());
        if (req.getId() != null) {
            socialInsuranceConfigManager.updateById(req);
        } else {
            // 按 user_id 唯一：如存在则更新
            LambdaQueryWrapper<OaSocialInsuranceConfig> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(OaSocialInsuranceConfig::getTenantId, tenantId);
            wrapper.eq(OaSocialInsuranceConfig::getUserId, req.getUserId());
            OaSocialInsuranceConfig existing = socialInsuranceConfigManager.getOne(wrapper);
            if (existing != null) {
                req.setId(existing.getId());
                socialInsuranceConfigManager.updateById(req);
            } else {
                socialInsuranceConfigManager.save(req);
            }
        }
        return req.getId();
    }

    public boolean deleteSocialInsuranceConfig(Long id) {
        return socialInsuranceConfigManager.removeById(id);
    }

    // ==================== 扣款规则 ====================

    public OaDeductionRule getDeductionRule(Long tenantId) {
        LambdaQueryWrapper<OaDeductionRule> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaDeductionRule::getTenantId, tenantId);
        OaDeductionRule rule = deductionRuleManager.getOne(wrapper);
        if (rule == null) {
            // 返回默认值
            rule = new OaDeductionRule();
            rule.setTenantId(tenantId);
            rule.setLateDeductionPerTime(new BigDecimal("50"));
            rule.setAbsentDeductionPerDay(new BigDecimal("200"));
        }
        return rule;
    }

    @Transactional(rollbackFor = Exception.class)
    public OaDeductionRule saveDeductionRule(OaDeductionRule req, Long tenantId) {
        req.setTenantId(tenantId);
        req.setUpdateTime(new Date());
        if (req.getId() != null) {
            deductionRuleManager.updateById(req);
        } else {
            LambdaQueryWrapper<OaDeductionRule> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(OaDeductionRule::getTenantId, tenantId);
            OaDeductionRule existing = deductionRuleManager.getOne(wrapper);
            if (existing != null) {
                req.setId(existing.getId());
                deductionRuleManager.updateById(req);
            } else {
                deductionRuleManager.save(req);
            }
        }
        return req;
    }

    // ==================== 工资条发放 ====================

    @Transactional(rollbackFor = Exception.class)
    public void sendPayslip(Long recordId, LoginUser loginUser) {
        SalaryRecord record = salaryRecordManager.getById(recordId);
        if (record == null) {
            throw new BusinessException("薪资记录不存在");
        }
        if (!"CONFIRMED".equals(record.getStatus())) {
            throw new BusinessException("只有已确认的薪资记录才能发送工资条");
        }

        String payslip = buildPayslipText(record);
        log.info("工资条内容 [{}] {}月：\n{}", record.getRealName(), record.getSalaryMonth(), payslip);

        record.setPayslipSent(1);
        record.setUpdateBy(loginUser.getUsername());
        record.setUpdateByName(loginUser.getRealName());
        record.setUpdateTime(new Date());
        salaryRecordManager.updateById(record);

        log.info("工资条已发送，记录ID: {}，员工: {}，月份: {}", recordId, record.getRealName(), record.getSalaryMonth());
    }

    private String buildPayslipText(SalaryRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("====== 工资条 ======\n");
        sb.append("姓名：").append(record.getRealName()).append("\n");
        sb.append("月份：").append(record.getSalaryMonth()).append("\n");
        sb.append("基本工资：").append(formatAmount(record.getBaseSalary())).append("\n");
        sb.append("新签提成：").append(formatAmount(record.getNewCommission())).append("\n");
        sb.append("续费提成：").append(formatAmount(record.getRenewCommission())).append("\n");
        if (record.getLateDeduction() != null && record.getLateDeduction().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("迟到扣款：-").append(formatAmount(record.getLateDeduction())).append("（").append(record.getLateCount()).append("次）\n");
        }
        if (record.getAbsentDeduction() != null && record.getAbsentDeduction().compareTo(BigDecimal.ZERO) > 0) {
            sb.append("旷工扣款：-").append(formatAmount(record.getAbsentDeduction())).append("（").append(record.getAbsentCount()).append("天）\n");
        }
        sb.append("应发工资：").append(formatAmount(record.getGrossSalary())).append("\n");
        sb.append("个人社保：-").append(formatAmount(record.getSocialInsurancePersonal())).append("\n");
        sb.append("个人公积金：-").append(formatAmount(record.getHousingFundPersonal())).append("\n");
        sb.append("实发工资：").append(formatAmount(record.getActualSalary())).append("\n");
        sb.append("===================");
        return sb.toString();
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    /**
     * 确认薪资
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmSalary(Long recordId, LoginUser loginUser) {
        SalaryRecord record = salaryRecordManager.getById(recordId);
        if (record == null) {
            throw new BusinessException("薪资记录不存在");
        }
        if ("CONFIRMED".equals(record.getStatus())) {
            throw new BusinessException("该薪资记录已确认");
        }

        record.setStatus("CONFIRMED");
        record.setUpdateBy(loginUser.getUsername());
        record.setUpdateByName(loginUser.getRealName());
        record.setUpdateTime(new Date());
        salaryRecordManager.updateById(record);

        log.info("薪资确认成功，记录ID: {}，用户: {}，月份: {}", recordId, record.getUsername(), record.getSalaryMonth());
        return true;
    }
}
