package com.qykj.finmaster.widget.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.widget.service.WidgetStatsService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 手机桌面小组件接口
 */
@RestController
@RequestMapping("/widget")
public class WidgetController extends BaseController {

    @Resource
    private WidgetStatsService widgetStatsService;

    /**
     * 小组件统计数据：账套总数、今日代账、今日工商、今日续费、新增线索
     */
    @GetMapping("/stats")
    public AjaxResult stats() {
        return AjaxResult.success(widgetStatsService.getStats(getTenantId()));
    }
}
