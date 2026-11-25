package com.qykj.finmaster.customer.company.UaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.service.CustomerCompanyServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 大屏无鉴权接口（/ua/** 命中 SecurityConfig 白名单，免鉴权）。
 *
 * @Description 数据大屏：全国合作公司省份分布等只读统计
 */
@RestController
@RequestMapping("/ua/screen")
public class UaScreenController {

    @Autowired
    private CustomerCompanyServiceImpl customerCompanyService;

    /**
     * 按省份统计合作中公司数（供全屏大屏地图使用）。
     * 无鉴权、全量跨租户、宽松口径（排除注销/合作取消、放行 NULL）。
     */
    @PermitAll
    @GetMapping("/companyProvinceStat")
    public AjaxResult companyProvinceStat() {
        return AjaxResult.success(customerCompanyService.companyProvinceStat());
    }
}
