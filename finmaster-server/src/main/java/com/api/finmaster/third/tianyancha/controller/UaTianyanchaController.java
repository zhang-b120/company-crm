package com.qykj.finmaster.third.tianyancha.controller;

import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycCompanySearchResp;
import com.qykj.finmaster.third.tianyancha.service.TianyanchaCompanyService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序版本的天眼查查询
 *
 * <pre>
 * POST /ua/tianyancha/search?keyword=xxx
 *   → 轻量候选列表（name / creditCode / legalPersonName / regStatus / regLocation / estiblishDate）
 *
 * POST /ua/tianyancha/detail?creditCode=xxx
 *   → 完整公司信息，返回字段：
 *       name             企业名
 *       creditCode       统一社会信用代码
 *       legalPersonName  法定代表人
 *       regStatus        工商状态
 *       estiblishDate    成立日期（yyyy-MM-dd）
 *       regLocation      注册地址
 *       industry         行业
 *       baseInfo         天眼查完整基础信息（注册资本 / 经营范围 / 省市区 等）
 *       stockholders     股东列表（type=1 的公司型股东已补查 creditCode）
 *       generalTaxpayer  是否一般纳税人（true/false，来自天眼查 /m/taxpayer/2.0）
 *       companyType      纳税人类型字典码：general-tax（一般纳税人）/ small-scale-tax（小规模）
 * </pre>
 */
@RestController
@RequestMapping("/ua/tianyancha")
public class UaTianyanchaController {

    @Autowired
    private TianyanchaCompanyService service;

    /**
     * 模糊搜索：仅返回 name / creditCode / legalPersonName / regStatus / regLocation / estiblishDate
     */
    @PermitAll
    @PostMapping("/search")
    public Result<List<TycCompanySearchResp>> search(@RequestParam("keyword") String keyword) {
        return Result.ok(service.search(keyword));
    }

    /**
     * 用户选定某家公司后调用：按 creditCode 拉基础信息 + 股东 + 一般纳税人判断
     * 同时写入/更新本地 tyc_company_info 缓存表
     */
    @PermitAll
    @PostMapping("/detail")
    public Result<TycCompanySearchResp> detail(@RequestParam("creditCode") String creditCode) {
        return Result.ok(service.getDetail(creditCode));
    }
}
