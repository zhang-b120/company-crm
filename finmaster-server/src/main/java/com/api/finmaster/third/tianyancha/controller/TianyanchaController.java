package com.qykj.finmaster.third.tianyancha.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.tianyancha.domain.TycCompanyInfo;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycCompanySearchResp;
import com.qykj.finmaster.third.tianyancha.service.TianyanchaCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 天眼查相关接口
 */
@RestController
@RequestMapping("/tianyancha")
public class TianyanchaController extends BaseController {

    @Autowired
    private TianyanchaCompanyService service;

    /**
     * 模糊搜索：仅返回轻量列表（name / creditCode / legalPersonName / regStatus / regLocation / estiblishDate）
     * 用户从列表选定后再调用 {@link #detail} 加载基础信息 + 股东
     */
    @PostMapping("/search")
    public Result<List<TycCompanySearchResp>> search(@RequestParam("keyword") String keyword) {
        return Result.ok(service.search(keyword));
    }

    /**
     * 用户选定某家公司后调用：按 creditCode 拉基础信息 + 股东（调 BASEINFO + HOLDER）
     */
    @PostMapping("/detail")
    public Result<TycCompanySearchResp> detail(@RequestParam("creditCode") String creditCode) {
        return Result.ok(service.getDetail(creditCode));
    }

    /**
     * 按统一社会信用代码从本地库查天眼查基础信息（公司详情页使用）
     */
    @GetMapping("/company-info")
    public Result<TycCompanyInfo> companyInfo(@RequestParam("creditCode") String creditCode) {
        return Result.ok(service.findByCreditCode(creditCode));
    }

    /**
     * 强制刷新：传 companyId 时同时刷新基础信息 + 股东；否则只按 creditCode 刷基础信息
     */
    @PostMapping("/refresh")
    public Result<TycCompanyInfo> refresh(
            @RequestParam(value = "companyId", required = false) Long companyId,
            @RequestParam(value = "creditCode", required = false) String creditCode) {
        TycCompanyInfo info = companyId != null
                ? service.refreshByCompanyId(companyId, getLoginUser())
                : service.refreshByCreditCode(creditCode);
        return Result.ok(info);
    }

    /**
     * 一般纳税人资格查询：直查天眼查 /m/taxpayer/2.0。
     * 返回 { generalTaxpayer: boolean, companyType: 'general-tax'|'small-scale-tax', items: [...] }
     * 业务规则：天眼查 result.items 非空 → 一般纳税人；空 / 接口异常 → 小规模。
     */
    @GetMapping("/taxpayer-status")
    public Result<java.util.Map<String, Object>> taxpayerStatus(@RequestParam("keyword") String keyword) {
        com.qykj.finmaster.third.tianyancha.util.TianyanchaUtil.TaxpayerLookup r =
                com.qykj.finmaster.third.tianyancha.util.TianyanchaUtil.getTaxpayer(keyword);
        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("generalTaxpayer", r.generalTaxpayer);
        resp.put("companyType", r.generalTaxpayer
                ? com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_TYPE_GENERAL
                : com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_TYPE_SCALE);
        resp.put("items", r.items);
        return Result.ok(resp);
    }
}
