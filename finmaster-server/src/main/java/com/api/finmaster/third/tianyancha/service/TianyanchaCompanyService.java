package com.qykj.finmaster.third.tianyancha.service;

import com.alibaba.fastjson.JSON;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.company.service.CompanyLegalPersonService;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.tianyancha.domain.TycCompanyInfo;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycBaseInfoResp;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycCompanySearchResp;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycHolderItem;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycSearchItem;
import com.qykj.finmaster.third.tianyancha.manager.TycCompanyInfoManager;
import com.qykj.finmaster.third.tianyancha.util.TianyanchaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 天眼查业务编排：天眼查模糊搜 → 对每条命中调用天眼查基础信息 + 股东 → 落库 + 聚合返回
 */
@Slf4j
@Service
public class TianyanchaCompanyService {

    @Autowired
    private TycCompanyInfoManager tycCompanyInfoManager;

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private CompanyLegalPersonService companyLegalPersonService;

    /**
     * 模糊搜索（廉价：只调 SEARCH_URL，不调 BASEINFO/HOLDER）
     * 返回搜索接口能给的轻量字段，用户选定后再调 {@link #getDetail} 拉详情
     * @param keyword 公司名关键字
     */
    public List<TycCompanySearchResp> search(String keyword) {
        if (StringUtils.isBlank(keyword)) return new ArrayList<>();

        List<TycSearchItem> candidates = TianyanchaUtil.searchByKeyword(keyword.trim(), 1, 20);
        if (candidates.isEmpty()) return new ArrayList<>();

        List<TycCompanySearchResp> result = new ArrayList<>();
        for (TycSearchItem c : candidates) {
            TycCompanySearchResp item = new TycCompanySearchResp();
            item.setName(c.getName());
            item.setCreditCode(c.getCreditCode());
            item.setLegalPersonName(c.getLegalPersonName());
            item.setRegStatus(c.getRegStatus());
            item.setEstiblishDate(normalizeSearchDate(c.getEstiblishTime()));
            item.setRegLocation(c.getRegLocation());
            // baseInfo / industry / stockholders 留空，用户选择后由 detail 接口补
            result.add(item);
        }
        return result;
    }

    /**
     * 按统一社会信用代码拉详情（贵接口：调 BASEINFO + HOLDER），用户在搜索列表选定后才调用
     * @param creditCode 统一社会信用代码（必传）
     * @return 含完整 baseInfo + 股东列表的聚合结果；BASEINFO 失败时返回 null
     */
    public TycCompanySearchResp getDetail(String creditCode) {
        if (StringUtils.isBlank(creditCode)) return null;

        TianyanchaUtil.BaseInfoWithRaw wrap = TianyanchaUtil.getBaseInfoWithRaw(creditCode);
        TycBaseInfoResp baseInfo = wrap != null ? wrap.baseInfo : null;
        if (baseInfo == null) return null;

        // 落库
        if (StringUtils.isNotBlank(baseInfo.getCreditCode())) {
            upsertTycCompanyInfo(baseInfo, wrap.rawJson);
        }

        List<TycHolderItem> holders = TianyanchaUtil.getHolders(creditCode);
        enrichHolderCreditCode(holders);

        // 同时调一般纳税人接口（与股东信息一起，避免在搜索阶段重复消费贵接口）
        TianyanchaUtil.TaxpayerLookup taxpayer = TianyanchaUtil.getTaxpayer(creditCode);

        TycCompanySearchResp item = new TycCompanySearchResp();
        item.setName(baseInfo.getName());
        item.setCreditCode(baseInfo.getCreditCode());
        item.setLegalPersonName(baseInfo.getLegalPersonName());
        item.setRegStatus(baseInfo.getRegStatus());
        item.setEstiblishDate(timestampToDateStr(baseInfo.getEstiblishTime()));
        item.setRegLocation(baseInfo.getRegLocation());
        item.setIndustry(baseInfo.getIndustry());
        item.setBaseInfo(baseInfo);
        item.setStockholders(holders);
        item.setGeneralTaxpayer(taxpayer != null && taxpayer.generalTaxpayer);
        item.setCompanyType(item.getGeneralTaxpayer()
                ? com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_TYPE_GENERAL
                : com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_TYPE_SCALE);
        return item;
    }

    /**
     * 按统一社会信用代码从本地库直接取（用于公司详情页加载，不再打天眼查）
     */
    public TycCompanyInfo findByCreditCode(String creditCode) {
        if (StringUtils.isBlank(creditCode)) return null;
        return tycCompanyInfoManager.lambdaQuery()
                .eq(TycCompanyInfo::getCreditCode, creditCode)
                .one();
    }

    /**
     * 强制刷新天眼查基础信息（用于公司详情页"从天眼查刷新"按钮）
     */
    public TycCompanyInfo refreshByCreditCode(String creditCode) {
        if (StringUtils.isBlank(creditCode)) return null;
        TianyanchaUtil.BaseInfoWithRaw wrap = TianyanchaUtil.getBaseInfoWithRaw(creditCode);
        if (wrap == null || wrap.baseInfo == null) return null;
        upsertTycCompanyInfo(wrap.baseInfo, wrap.rawJson);
        return findByCreditCode(wrap.baseInfo.getCreditCode());
    }

    /**
     * 按 customer_company.id 刷新：更新天眼查基础信息 + 重新导入股东（同名跳过，不会重复）
     */
    public TycCompanyInfo refreshByCompanyId(Long companyId, LoginUser loginUser) {
        if (companyId == null) return null;
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (cc == null || StringUtils.isBlank(cc.getTaxNumber())) return null;
        String creditCode = cc.getTaxNumber();

        // 1) 刷新 tyc_company_info
        TianyanchaUtil.BaseInfoWithRaw wrap = TianyanchaUtil.getBaseInfoWithRaw(creditCode);
        if (wrap != null && wrap.baseInfo != null) {
            upsertTycCompanyInfo(wrap.baseInfo, wrap.rawJson);
            // 1.1) 把基础信息同步到 customer_company（成立日期 / 工商状态 / 注册地址 / 法人 等）
            syncCustomerCompanyFromTyc(cc, wrap.baseInfo);
        }

        // 2) 重新拉股东 + 补查 creditCode + 落到 company_legal_person（同名跳过）
        List<TycHolderItem> holders = TianyanchaUtil.getHolders(creditCode);
        if (holders != null && !holders.isEmpty()) {
            enrichHolderCreditCode(holders);
            companyLegalPersonService.batchImportFromTyc(companyId, holders, loginUser);
        }

        return findByCreditCode(creditCode);
    }

    /**
     * 把天眼查 baseInfo 同步到 customer_company：成立日期 / 工商状态 / 注册地址 / 法人姓名 / 公司名（仅在为空时）。
     * 仅当字段值发生变化时才更新；空值不会覆盖已有内容。
     */
    private void syncCustomerCompanyFromTyc(CustomerCompany cc, TycBaseInfoResp base) {
        if (cc == null || base == null) return;
        boolean dirty = false;

        // 成立日期：天眼查返回时间戳 → LocalDate
        java.time.LocalDate established = timestampToLocalDate(base.getEstiblishTime());
        if (established != null && !java.util.Objects.equals(established, cc.getEstablishedDate())) {
            cc.setEstablishedDate(established);
            dirty = true;
        }
        // 工商状态
        if (StringUtils.isNotBlank(base.getRegStatus())
                && !java.util.Objects.equals(base.getRegStatus(), cc.getRegStatus())) {
            cc.setRegStatus(base.getRegStatus());
            dirty = true;
        }
        // 注册地址（优先 regLocation，备选 regLocationHalfWidth）
        String addr = StringUtils.isNotBlank(base.getRegLocation())
                ? base.getRegLocation()
                : base.getRegLocationHalfWidth();
        if (StringUtils.isNotBlank(addr)
                && !java.util.Objects.equals(addr, cc.getRegisteredAddress())) {
            cc.setRegisteredAddress(addr);
            dirty = true;
        }
        // 法人姓名（仅当现有为空或与天眼查不一致时才覆盖；天眼查返回为空则跳过）
        if (StringUtils.isNotBlank(base.getLegalPersonName())
                && !java.util.Objects.equals(base.getLegalPersonName(), cc.getLegalName())) {
            cc.setLegalName(base.getLegalPersonName());
            dirty = true;
        }
        // 公司名（仅当现有为空时填入，避免覆盖人工修订）
        if (StringUtils.isBlank(cc.getCompanyName()) && StringUtils.isNotBlank(base.getName())) {
            cc.setCompanyName(base.getName());
            dirty = true;
        }
        if (dirty) {
            cc.setUpdateTime(new java.util.Date());
            customerCompanyManager.updateById(cc);
            log.info("[天眼查→公司表] 同步 customer_company.id={} 完成 (成立日期/工商状态/注册地址/法人 字段已更新)", cc.getId());
        }
    }

    /** 时间戳（毫秒）转 LocalDate；null/无效返回 null */
    private static java.time.LocalDate timestampToLocalDate(Long ts) {
        if (ts == null || ts <= 0) return null;
        try {
            return java.time.Instant.ofEpochMilli(ts)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    /* -------------------- 内部工具 -------------------- */

    /**
     * 对 type=1 公司型股东，通过天眼查模糊搜索补查社会统一信用代码
     */
    private void enrichHolderCreditCode(List<TycHolderItem> holders) {
        if (holders == null || holders.isEmpty()) return;
        for (TycHolderItem h : holders) {
            if (!Objects.equals(h.getType(), 1)) continue;
            if (StringUtils.isBlank(h.getName())) continue;
            try {
                List<TycSearchItem> list = TianyanchaUtil.searchByKeyword(h.getName(), 1, 5);
                if (list.isEmpty()) continue;
                TycSearchItem match = list.stream()
                        .filter(r -> h.getName().equals(r.getName()))
                        .findFirst()
                        .orElse(list.get(0));
                h.setCreditCode(match.getCreditCode());
            } catch (Exception e) {
                log.warn("[股东税号补查] 失败，shareholder={}", h.getName(), e);
            }
        }
    }

    /**
     * 按 credit_code 唯一键 upsert
     */
    private void upsertTycCompanyInfo(TycBaseInfoResp base, String rawJson) {
        TycCompanyInfo existing = tycCompanyInfoManager.lambdaQuery()
                .eq(TycCompanyInfo::getCreditCode, base.getCreditCode())
                .one();

        TycCompanyInfo entity = existing != null ? existing : new TycCompanyInfo();
        entity.setTycId(base.getId());
        entity.setName(base.getName());
        entity.setAlias(base.getAlias());
        entity.setCreditCode(base.getCreditCode());
        entity.setTaxNumber(base.getTaxNumber());
        entity.setRegNumber(base.getRegNumber());
        entity.setOrgNumber(base.getOrgNumber());
        entity.setLegalPersonName(base.getLegalPersonName());
        entity.setLegalPersonType(base.getType());
        entity.setRegStatus(base.getRegStatus());
        entity.setRegCapital(base.getRegCapital());
        entity.setRegCapitalCurrency(base.getRegCapitalCurrency());
        entity.setActualCapital(base.getActualCapital());
        entity.setActualCapitalCurrency(base.getActualCapitalCurrency());
        entity.setCompanyOrgType(base.getCompanyOrgType());
        entity.setCompForm(base.getCompForm());
        entity.setIndustry(base.getIndustry());
        if (base.getIndustryAll() != null) {
            entity.setCategory(base.getIndustryAll().getCategory());
            entity.setCategoryBig(base.getIndustryAll().getCategoryBig());
            entity.setCategoryMiddle(base.getIndustryAll().getCategoryMiddle());
            entity.setCategorySmall(base.getIndustryAll().getCategorySmall());
            entity.setCategoryCodeFirst(base.getIndustryAll().getCategoryCodeFirst());
            entity.setCategoryCodeSecond(base.getIndustryAll().getCategoryCodeSecond());
            entity.setCategoryCodeThird(base.getIndustryAll().getCategoryCodeThird());
            entity.setCategoryCodeFourth(base.getIndustryAll().getCategoryCodeFourth());
        }
        entity.setRegLocation(base.getRegLocation());
        entity.setRegLocationHalfWidth(base.getRegLocationHalfWidth());
        entity.setBase(base.getBase());
        entity.setCity(base.getCity());
        entity.setDistrict(base.getDistrict());
        entity.setDistrictCode(base.getDistrictCode());
        entity.setEconomicFunctionZone1(base.getEconomicFunctionZone1());
        entity.setEconomicFunctionZone2(base.getEconomicFunctionZone2());
        entity.setRegInstitute(base.getRegInstitute());
        entity.setEstiblishTime(timestampToDate(base.getEstiblishTime()));
        entity.setApprovedTime(timestampToDate(base.getApprovedTime()));
        entity.setFromTime(timestampToDate(base.getFromTime()));
        entity.setToTime(timestampToDate(base.getToTime()));
        entity.setRevokeDate(timestampToDate(base.getRevokeDate()));
        entity.setRevokeReason(base.getRevokeReason());
        entity.setCancelDate(timestampToDate(base.getCancelDate()));
        entity.setCancelReason(base.getCancelReason());
        entity.setStaffNumRange(base.getStaffNumRange());
        entity.setSocialStaffNum(base.getSocialStaffNum());
        entity.setIsMicroEnt(base.getIsMicroEnt());
        entity.setAboveScale(base.getAboveScale());
        entity.setPercentileScore(base.getPercentileScore());
        entity.setBondName(base.getBondName());
        entity.setUsedBondName(base.getUsedBondName());
        entity.setBondNum(base.getBondNum());
        entity.setBondType(base.getBondType());
        entity.setBusinessScope(base.getBusinessScope());
        entity.setTags(base.getTags());
        entity.setEnglishName(base.getProperty3());
        entity.setHistoryNames(base.getHistoryNames());
        entity.setHistoryNameList(base.getHistoryNameList() == null ? null : JSON.toJSONString(base.getHistoryNameList()));
        entity.setTycUpdateTime(timestampToDate(base.getUpdateTimes()));
        entity.setRawJson(rawJson);

        Date now = new Date();
        if (existing == null) {
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            tycCompanyInfoManager.save(entity);
        } else {
            entity.setUpdateTime(now);
            tycCompanyInfoManager.updateById(entity);
        }
    }

    private static Date timestampToDate(Long ts) {
        if (ts == null || ts <= 0) return null;
        return new Date(ts);
    }

    private static String timestampToDateStr(Long ts) {
        if (ts == null || ts <= 0) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault())
                .toLocalDate()
                .toString();
    }

    /**
     * 归一化模糊搜索接口的成立时间字符串："2021-04-22 00:00:00.0" → "2021-04-22"；
     * "-" / 空 / 无日期 → null。取前 10 位日期部分，非法则原样返回 trim 结果。
     */
    private static String normalizeSearchDate(String raw) {
        if (StringUtils.isBlank(raw)) return null;
        String s = raw.trim();
        if ("-".equals(s)) return null;
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }
}
