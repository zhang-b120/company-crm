package com.qykj.finmaster.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.workbench.dto.SalesWorkbenchResp;
import com.qykj.finmaster.workbench.entity.CommissionTierConfig;
import com.qykj.finmaster.workbench.mapper.CommissionTierMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 提成档位查询 + 提成计算
 *
 * 优先按 tenant_id 查租户专属配置，没配置时回退到全局（tenant_id=0）
 */
@Service
public class CommissionTierService {

    @Autowired
    private CommissionTierMapper mapper;

    /** 查询业绩档（普通档）列表，已排序 */
    public List<CommissionTierConfig> listPerformanceTiers(Long tenantId) {
        return loadTiers(tenantId, 0);
    }

    /** 查询工商业务档（一般只有一行） */
    public List<CommissionTierConfig> listBusinessRegTiers(Long tenantId) {
        return loadTiers(tenantId, 1);
    }

    /** 全部档位（前端配置页用） */
    public List<CommissionTierConfig> listAll(Long tenantId) {
        List<CommissionTierConfig> own = mapper.selectList(new LambdaQueryWrapper<CommissionTierConfig>()
                .eq(CommissionTierConfig::getTenantId, tenantId == null ? 0L : tenantId)
                .orderByAsc(CommissionTierConfig::getSort));
        if (own != null && !own.isEmpty()) return own;
        return mapper.selectList(new LambdaQueryWrapper<CommissionTierConfig>()
                .eq(CommissionTierConfig::getTenantId, 0L)
                .orderByAsc(CommissionTierConfig::getSort));
    }

    private List<CommissionTierConfig> loadTiers(Long tenantId, int isBusinessReg) {
        List<CommissionTierConfig> own = mapper.selectList(new LambdaQueryWrapper<CommissionTierConfig>()
                .eq(CommissionTierConfig::getTenantId, tenantId == null ? 0L : tenantId)
                .eq(CommissionTierConfig::getIsBusinessReg, isBusinessReg)
                .eq(CommissionTierConfig::getEnabled, 1)
                .orderByAsc(CommissionTierConfig::getMinAmount));
        if (own != null && !own.isEmpty()) return own;
        return mapper.selectList(new LambdaQueryWrapper<CommissionTierConfig>()
                .eq(CommissionTierConfig::getTenantId, 0L)
                .eq(CommissionTierConfig::getIsBusinessReg, isBusinessReg)
                .eq(CommissionTierConfig::getEnabled, 1)
                .orderByAsc(CommissionTierConfig::getMinAmount));
    }

    /**
     * 按当前业绩计算提成
     *
     * @param tenantId          租户
     * @param totalMonthAmount  本月业绩总金额（含工商业务，仅做展示）
     * @param performanceAmount 计入业绩档的金额（=总金额-工商业务收入，避免重复提成）
     * @param businessRegProfit 工商业务利润（revenue - cost）
     */
    public SalesWorkbenchResp.CommissionTierBlock computeCommission(
            Long tenantId, BigDecimal totalMonthAmount, BigDecimal performanceAmount, BigDecimal businessRegProfit) {
        SalesWorkbenchResp.CommissionTierBlock block = new SalesWorkbenchResp.CommissionTierBlock();
        if (totalMonthAmount == null) totalMonthAmount = BigDecimal.ZERO;
        if (performanceAmount == null) performanceAmount = BigDecimal.ZERO;
        if (businessRegProfit == null) businessRegProfit = BigDecimal.ZERO;
        block.setCurrentMonthAmount(totalMonthAmount);
        block.setBusinessRegProfit(businessRegProfit);

        // 业绩档查找
        List<CommissionTierConfig> tiers = listPerformanceTiers(tenantId);
        tiers = new ArrayList<>(tiers);
        tiers.sort(Comparator.comparing(CommissionTierConfig::getMinAmount,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        CommissionTierConfig current = null;
        CommissionTierConfig next = null;
        for (int i = 0; i < tiers.size(); i++) {
            CommissionTierConfig t = tiers.get(i);
            BigDecimal min = t.getMinAmount() == null ? BigDecimal.ZERO : t.getMinAmount();
            BigDecimal max = t.getMaxAmount();
            boolean inLow = performanceAmount.compareTo(min) >= 0;
            boolean inHigh = max == null || performanceAmount.compareTo(max) < 0;
            if (inLow && inHigh) {
                current = t;
                if (i + 1 < tiers.size()) {
                    next = tiers.get(i + 1);
                }
                break;
            }
        }

        BigDecimal performanceCommission = BigDecimal.ZERO;
        if (current != null) {
            block.setCurrentTierName(current.getTierName());
            block.setCurrentRate(current.getRatePercent());
            performanceCommission = performanceAmount
                    .multiply(current.getRatePercent())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (next != null) {
                BigDecimal nextMin = next.getMinAmount() == null ? BigDecimal.ZERO : next.getMinAmount();
                BigDecimal diff = nextMin.subtract(performanceAmount);
                block.setNextTierThreshold(nextMin);
                block.setAmountToNextTier(diff.signum() < 0 ? BigDecimal.ZERO : diff);
                block.setNextTierRate(next.getRatePercent());
            }
        }
        block.setPerformanceCommission(performanceCommission);

        // 工商业务档
        BigDecimal businessRegCommission = BigDecimal.ZERO;
        List<CommissionTierConfig> bizTiers = listBusinessRegTiers(tenantId);
        if (bizTiers != null && !bizTiers.isEmpty() && businessRegProfit.signum() > 0) {
            BigDecimal rate = bizTiers.get(0).getRatePercent();
            if (rate != null) {
                businessRegCommission = businessRegProfit
                        .multiply(rate)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
        block.setBusinessRegCommission(businessRegCommission);

        block.setTotalCommission(performanceCommission.add(businessRegCommission));
        return block;
    }
}
