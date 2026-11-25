package com.qykj.finmaster.scheduledTask;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.utils.third.QccUtil;
import com.qykj.finmaster.common.utils.third.req.QccSearchReq;
import com.qykj.finmaster.common.utils.third.resp.QccSearchResp;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 每月自动刷新合作中客户的工商状态定时任务
 *
 * 背景：企查查字段 reg_status（在业/存续/吊销/注销）一旦录入，后续不会变化。
 * 如果客户企业被吊销或注销，业务侧无法感知，可能仍然把它当合作中客户处理。
 *
 * 规则：
 *   1) 每月 1 号凌晨 3 点执行
 *   2) 查所有 company_status = COOPERATING 的公司
 *   3) 用 tax_number 作为 searchKey 调企查查模糊查询
 *   4) 返回结果若能精确匹配（creditCode == taxNumber），比对 reg_status 变化
 *   5) 有变化则更新，并记录日志（后续 S2-11 基础设施完善后 WS 推送给客户部主管）
 *   6) 失败不阻塞下一条（try-catch 每条独立）
 *
 * 性能：企查查接口有 QPS 限制，每次调用间隔 500ms，避免被限流。
 * 暂无并行处理，按顺序单线程跑，夜间执行可接受。
 */
@Component
@Slf4j
public class CompanyRegStatusRefreshTask {

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Value("${company.reg-status-refresh.enabled:true}")
    private boolean enabled;

    /**
     * 每月 1 号凌晨 3 点执行
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void refreshAllCompanyRegStatus() {
        if (!enabled) {
            log.info("[工商状态刷新] 任务已禁用，跳过");
            return;
        }
        log.info("[工商状态刷新] 任务开始");

        List<CustomerCompany> companies = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getCompanyStatus, "COOPERATING")
                .isNotNull(CustomerCompany::getTaxNumber)
                .ne(CustomerCompany::getTaxNumber, "")
                .list();

        int total = companies.size();
        int updated = 0;
        int failed = 0;
        log.info("[工商状态刷新] 合作中公司共 {} 条", total);

        for (CustomerCompany company : companies) {
            try {
                String newStatus = queryRegStatusFromQcc(company.getTaxNumber(), company.getCompanyName());
                if (StringUtils.isNotBlank(newStatus) && !Objects.equals(newStatus, company.getRegStatus())) {
                    log.warn("[工商状态刷新] 公司状态变化 id={} name={} {} -> {}",
                            company.getId(), company.getCompanyName(), company.getRegStatus(), newStatus);

                    customerCompanyManager.lambdaUpdate()
                            .set(CustomerCompany::getRegStatus, newStatus)
                            .set(CustomerCompany::getUpdateTime, new Date())
                            .eq(CustomerCompany::getId, company.getId())
                            .update();
                    updated++;

                    // TODO: 接入 L1-6 通知基础设施后，这里推送 WS 给客户部主管
                    //  内容建议："[工商状态变化] XX 公司由【在业】变为【吊销】，请核实"
                }
                // 限流避让
                Thread.sleep(500);
            } catch (Exception e) {
                failed++;
                log.warn("[工商状态刷新] 刷新失败 id={} name={}: {}",
                        company.getId(), company.getCompanyName(), e.getMessage());
            }
        }

        log.info("[工商状态刷新] 任务结束 total={} updated={} failed={}", total, updated, failed);
    }

    /**
     * 从企查查查询工商状态
     * @param taxNumber 税号（优先）
     * @param companyName 公司名（兜底）
     * @return 工商状态字符串（如"存续"/"在业"/"吊销"/"注销"），查不到返回 null
     */
    private String queryRegStatusFromQcc(String taxNumber, String companyName) {
        QccSearchReq req = new QccSearchReq();
        req.setSearchKey(StringUtils.isNotBlank(taxNumber) ? taxNumber : companyName);
        List<QccSearchResp> results = QccUtil.getCompanyInfo(req);
        if (CollUtil.isEmpty(results)) {
            return null;
        }
        // 精确匹配 creditCode
        if (StringUtils.isNotBlank(taxNumber)) {
            for (QccSearchResp r : results) {
                if (taxNumber.equals(r.getCreditCode())) {
                    return r.getStatus();
                }
            }
        }
        // 公司名精确匹配
        if (StringUtils.isNotBlank(companyName)) {
            for (QccSearchResp r : results) {
                if (companyName.equals(r.getName())) {
                    return r.getStatus();
                }
            }
        }
        return null;
    }
}
