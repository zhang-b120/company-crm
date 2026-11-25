package com.qykj.finmaster.history;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 历史订单批量生成合同（仅内部/运维使用）
 */
@RestController
@RequestMapping("/history/contract")
public class HistoryContractGenerateController {

    @Autowired
    private HistoryContractGenerateService historyContractGenerateService;

    /**
     * 手动触发历史合同生成
     *
     * @param deployTime 可选，上线时间边界，格式与 orders.create_time 一致
     *                   例如：2026-02-06 00:00:00
     *                   传 null/空字符串，则不按时间过滤，处理所有符合条件的历史订单
     */
    @PostMapping("/generate")
    @PermitAll
    public AjaxResult generate(@RequestParam(value = "deployTime", required = false) String deployTime) {
        historyContractGenerateService.generateHistoryContracts(deployTime);
        return AjaxResult.success("历史合同批量生成任务已执行完成（请结合数据验证结果）");
    }
}