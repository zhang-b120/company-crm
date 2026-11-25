package com.qykj.finmaster.customer.company.service;

import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CompanyLegalPerson;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyLegalPersonReq;
import com.qykj.finmaster.customer.company.domain.req.UpdateCompanyLegalPersonReq;
import com.qykj.finmaster.customer.company.manager.CompanyLegalPersonManager;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycHolderItem;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CompanyLegalPersonService {

    @Autowired
    private CompanyLegalPersonManager manager;

    @Autowired
    private com.qykj.finmaster.customer.company.manager.CustomerCompanyManager customerCompanyManager;

    public List<CompanyLegalPerson> listByCompanyId(Long companyId) {
        return manager.lambdaQuery()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .orderByAsc(CompanyLegalPerson::getId)
                .list();
    }

    public CompanyLegalPerson add(AddCompanyLegalPersonReq req, LoginUser loginUser) {
        CompanyLegalPerson entity = CopierCopyUtils.convertTo(req, CompanyLegalPerson.class);
        entity.setCreateBy(loginUser.getUsername());
        entity.setCreateByName(loginUser.getRealName());
        entity.setCreateTime(new Date());
        entity.setUpdateBy(loginUser.getUsername());
        entity.setUpdateByName(loginUser.getRealName());
        entity.setUpdateTime(new Date());
        manager.save(entity);
        return entity;
    }

    public boolean update(UpdateCompanyLegalPersonReq req, LoginUser loginUser) {
        CompanyLegalPerson entity = CopierCopyUtils.convertTo(req, CompanyLegalPerson.class);
        entity.setUpdateBy(loginUser.getUsername());
        entity.setUpdateByName(loginUser.getRealName());
        entity.setUpdateTime(new Date());
        return manager.updateById(entity);
    }

    public boolean delete(Long id) {
        return manager.removeById(id);
    }

    /**
     * 从天眼查股东数据批量导入（按姓名 upsert：已存在则更新证件号 / 持股比例，不存在则新增）。
     * 证件号 creditCode：type=1 公司为社会统一信用代码（Service 层补查），type=2/3 为用户前端填写的身份证/证件号。
     * 统一写入 CompanyLegalPerson.idCard 字段。
     * @return 实际写入条数（含新增 + 更新）
     */
    public int batchImportFromTyc(Long companyId, List<TycHolderItem> stockholders, LoginUser loginUser) {
        if (companyId == null || stockholders == null || stockholders.isEmpty()) return 0;

        // 按姓名映射现有记录，便于命中后做 update
        java.util.Map<String, CompanyLegalPerson> existingByName = manager.lambdaQuery()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        CompanyLegalPerson::getName,
                        e -> e,
                        (a, b) -> a   // 同名取第一条，避免老数据重复
                ));

        Date now = new Date();
        int count = 0;
        for (TycHolderItem s : stockholders) {
            if (StringUtils.isBlank(s.getName())) continue;

            String idCard = StringUtils.isNotBlank(s.getCreditCode()) ? s.getCreditCode() : null;
            BigDecimal ratio = parseRatio(extractPercent(s));
            CompanyLegalPerson exist = existingByName.get(s.getName());
            if (exist != null) {
                // 已存在 → 更新（仅在新值非空时覆盖，避免空值清空老数据）
                CompanyLegalPerson upd = new CompanyLegalPerson();
                upd.setId(exist.getId());
                if (idCard != null) upd.setIdCard(idCard);
                if (ratio != null)  upd.setShareholdingRatio(ratio);
                upd.setUpdateBy(loginUser.getUsername());
                upd.setUpdateByName(loginUser.getRealName());
                upd.setUpdateTime(now);
                manager.updateById(upd);
            } else {
                // 不存在 → 新增
                CompanyLegalPerson entity = new CompanyLegalPerson();
                entity.setCompanyId(companyId);
                entity.setName(s.getName());
                if (idCard != null) entity.setIdCard(idCard);
                entity.setShareholdingRatio(ratio);
                entity.setCreateBy(loginUser.getUsername());
                entity.setCreateByName(loginUser.getRealName());
                entity.setCreateTime(now);
                entity.setUpdateBy(loginUser.getUsername());
                entity.setUpdateByName(loginUser.getRealName());
                entity.setUpdateTime(now);
                manager.save(entity);
                existingByName.put(s.getName(), entity);
            }
            count++;
        }
        return count;
    }

    /** 取持股占比：优先认缴 capital[0].percent，其次实缴 capitalActl[0].percent */
    private String extractPercent(TycHolderItem s) {
        if (s.getCapital() != null && !s.getCapital().isEmpty()) {
            String p = s.getCapital().get(0).getPercent();
            if (StringUtils.isNotBlank(p)) return p;
        }
        if (s.getCapitalActl() != null && !s.getCapitalActl().isEmpty()) {
            return s.getCapitalActl().get(0).getPercent();
        }
        return null;
    }

    /**
     * 小程序批量导入股东（openId 归属校验）
     */
    public int uaBatchImportFromTyc(String openId, Long companyId, List<TycHolderItem> stockholders) {
        if (StringUtils.isBlank(openId) || companyId == null) {
            throw new com.qykj.finmaster.common.exception.BusinessException("参数不完整");
        }
        com.qykj.finmaster.customer.company.domain.CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getId, companyId)
                .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getOpenid, openId)
                .one();
        if (cc == null) {
            throw new com.qykj.finmaster.common.exception.BusinessException("无权限操作该公司");
        }
        if (stockholders == null || stockholders.isEmpty()) return 0;
        // 按姓名 upsert：已存在则更新，不存在则新增
        java.util.Map<String, CompanyLegalPerson> existingByName = manager.lambdaQuery()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        CompanyLegalPerson::getName,
                        e -> e,
                        (a, b) -> a
                ));
        Date now = new Date();
        int count = 0;
        for (TycHolderItem s : stockholders) {
            if (StringUtils.isBlank(s.getName())) continue;

            String idCard = StringUtils.isNotBlank(s.getCreditCode()) ? s.getCreditCode() : null;
            BigDecimal ratio = parseRatio(extractPercent(s));
            CompanyLegalPerson exist = existingByName.get(s.getName());
            if (exist != null) {
                CompanyLegalPerson upd = new CompanyLegalPerson();
                upd.setId(exist.getId());
                if (idCard != null) upd.setIdCard(idCard);
                if (ratio != null)  upd.setShareholdingRatio(ratio);
                upd.setUpdateBy(openId);
                upd.setUpdateByName("小程序用户");
                upd.setUpdateTime(now);
                manager.updateById(upd);
            } else {
                CompanyLegalPerson entity = new CompanyLegalPerson();
                entity.setCompanyId(companyId);
                entity.setName(s.getName());
                if (idCard != null) entity.setIdCard(idCard);
                entity.setShareholdingRatio(ratio);
                entity.setCreateBy(openId);
                entity.setCreateByName("小程序用户");
                entity.setCreateTime(now);
                entity.setUpdateBy(openId);
                entity.setUpdateByName("小程序用户");
                entity.setUpdateTime(now);
                manager.save(entity);
                existingByName.put(s.getName(), entity);
            }
            count++;
        }
        return count;
    }

    /** 解析持股比例：支持 "30%"、"30.00%"、"0.30"（视为百分比直接使用） */
    private BigDecimal parseRatio(String percent) {
        if (StringUtils.isBlank(percent)) return null;
        try {
            String s = percent.trim().replace("%", "");
            BigDecimal val = new BigDecimal(s);
            if (!percent.contains("%") && val.compareTo(BigDecimal.ONE) <= 0) {
                val = val.multiply(BigDecimal.valueOf(100));
            }
            return val.setScale(2, BigDecimal.ROUND_HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }
}
