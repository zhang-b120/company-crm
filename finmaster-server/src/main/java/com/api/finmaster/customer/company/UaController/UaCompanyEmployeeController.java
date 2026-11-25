package com.qykj.finmaster.customer.company.UaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.req.UaCompanyEmployeeReq;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序公司员工名册接口
 * 通过 openId 校验 customer_company 归属：openid = req.openId 且 id = req.companyId 才允许操作
 */
@RestController
@RequestMapping("/ua/company-employee")
public class UaCompanyEmployeeController {

    @Autowired
    private CompanyEmployeeManager manager;

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    /** 列表 */
    @PermitAll
    @PostMapping("/list")
    public AjaxResult list(@RequestBody @Valid UaCompanyEmployeeReq req) {
        CustomerCompany cc = checkOwnership(req.getOpenId(), req.getCompanyId());
        List<CompanyEmployee> list = manager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, cc.getId())
                .orderByAsc(CompanyEmployee::getId)
                .list();
        return AjaxResult.success(list);
    }

    /** 单条新增 / 更新（按是否带 id 决定，便于小程序逐条提交） */
    @PermitAll
    @PostMapping("/save")
    public AjaxResult save(@RequestBody @Valid UaCompanyEmployeeReq req) {
        CustomerCompany cc = checkOwnership(req.getOpenId(), req.getCompanyId());
        CompanyEmployee e = req.getEmployee();
        if (e == null) throw new BusinessException("员工信息不能为空");
        if (StringUtils.isBlank(e.getName())) throw new BusinessException("姓名必填");
        if (StringUtils.isBlank(e.getIdCard())) throw new BusinessException("身份证号必填");

        Date now = new Date();
        if (e.getId() == null) {
            e.setTenantId(cc.getTenantId());
            e.setCompanyId(cc.getId());
            e.setDeleted(0);
            e.setCreateBy(req.getOpenId());
            e.setCreateByName("小程序用户");
            e.setCreateTime(now);
            e.setUpdateTime(now);
            manager.save(e);
            return AjaxResult.success(e);
        }
        // 更新前再次确认这条员工属于该公司
        CompanyEmployee db = manager.getById(e.getId());
        if (db == null || !cc.getId().equals(db.getCompanyId())) {
            throw new BusinessException("员工不存在或不属于当前公司");
        }
        if (e.getName() != null) db.setName(e.getName());
        if (e.getIdCard() != null) db.setIdCard(e.getIdCard());
        if (e.getPhone() != null) db.setPhone(e.getPhone());
        if (e.getSalary() != null) db.setSalary(e.getSalary());
        db.setUpdateBy(req.getOpenId());
        db.setUpdateByName("小程序用户");
        db.setUpdateTime(now);
        manager.updateById(db);
        return AjaxResult.success(db);
    }

    /** 批量保存（全量替换该公司员工，方便小程序一次性提交整张表） */
    @PermitAll
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody @Valid UaCompanyEmployeeReq req) {
        CustomerCompany cc = checkOwnership(req.getOpenId(), req.getCompanyId());
        List<CompanyEmployee> incoming = req.getEmployees();
        Date now = new Date();

        // 软删现有
        manager.lambdaUpdate()
                .eq(CompanyEmployee::getCompanyId, cc.getId())
                .set(CompanyEmployee::getDeleted, 1)
                .set(CompanyEmployee::getUpdateBy, req.getOpenId())
                .set(CompanyEmployee::getUpdateByName, "小程序用户")
                .set(CompanyEmployee::getUpdateTime, now)
                .update();

        if (incoming == null || incoming.isEmpty()) {
            return AjaxResult.success();
        }
        for (CompanyEmployee e : incoming) {
            if (StringUtils.isBlank(e.getName()) || StringUtils.isBlank(e.getIdCard())) {
                throw new BusinessException("姓名和身份证号必填");
            }
            e.setId(null);
            e.setTenantId(cc.getTenantId());
            e.setCompanyId(cc.getId());
            e.setDeleted(0);
            e.setCreateBy(req.getOpenId());
            e.setCreateByName("小程序用户");
            e.setCreateTime(now);
            e.setUpdateTime(now);
            manager.save(e);
        }
        return AjaxResult.success();
    }

    /** 删除（按 employeeId） */
    @PermitAll
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody @Valid UaCompanyEmployeeReq req) {
        if (req.getEmployeeId() == null) throw new BusinessException("employeeId 不能为空");
        CompanyEmployee db = manager.getById(req.getEmployeeId());
        if (db == null) throw new BusinessException("员工不存在");
        // 反查公司，再校验 openId 归属
        checkOwnership(req.getOpenId(), db.getCompanyId());
        manager.removeById(req.getEmployeeId());
        return AjaxResult.success();
    }

    /**
     * 按税号保存员工名册（身份证去重）
     * 请求体：{ openId, taxNumber, employees: [...] }
     * 规则：相同身份证号已存在则跳过；本次未传入的旧员工软删
     */
    @PermitAll
    @PostMapping("/saveByTaxNumber")
    public AjaxResult saveByTaxNumber(@RequestBody @Valid UaCompanyEmployeeReq req) {
        if (StringUtils.isBlank(req.getTaxNumber())) throw new BusinessException("税号不能为空");
        CustomerCompany cc = checkOwnershipByTaxNumber(req.getOpenId(), req.getTaxNumber());

        List<CompanyEmployee> incoming = req.getEmployees();
        if (incoming == null) incoming = List.of();
        Date now = new Date();

        // 查询该公司当前活跃员工
        List<CompanyEmployee> existing = manager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, cc.getId())
                .list();

        // 按身份证号建索引，用于去重
        Map<String, CompanyEmployee> existingByIdCard = existing.stream()
                .filter(e -> StringUtils.isNotBlank(e.getIdCard()))
                .collect(Collectors.toMap(CompanyEmployee::getIdCard, e -> e, (a, b) -> a));

        // 本次传入的身份证号集合，用于判断哪些旧员工需软删
        Set<String> incomingIdCards = incoming.stream()
                .map(CompanyEmployee::getIdCard)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 软删：在 DB 中存在但本次未传入的员工
        for (CompanyEmployee ex : existing) {
            if (StringUtils.isBlank(ex.getIdCard()) || !incomingIdCards.contains(ex.getIdCard())) {
                manager.lambdaUpdate()
                        .eq(CompanyEmployee::getId, ex.getId())
                        .set(CompanyEmployee::getDeleted, 1)
                        .set(CompanyEmployee::getUpdateBy, req.getOpenId())
                        .set(CompanyEmployee::getUpdateByName, "小程序用户")
                        .set(CompanyEmployee::getUpdateTime, now)
                        .update();
            }
        }

        // 插入：身份证号不存在于该公司的才录入
        for (CompanyEmployee e : incoming) {
            if (StringUtils.isBlank(e.getName())) throw new BusinessException("姓名必填");
            if (StringUtils.isBlank(e.getIdCard())) throw new BusinessException("身份证号必填");
            if (existingByIdCard.containsKey(e.getIdCard())) continue; // 已存在，跳过
            e.setId(null);
            e.setTenantId(cc.getTenantId());
            e.setCompanyId(cc.getId());
            e.setDeleted(0);
            e.setCreateBy(req.getOpenId());
            e.setCreateByName("小程序用户");
            e.setCreateTime(now);
            e.setUpdateTime(now);
            manager.save(e);
        }
        return AjaxResult.success();
    }

    /** openId + companyId 归属校验 */
    private CustomerCompany checkOwnership(String openId, Long companyId) {
        if (StringUtils.isBlank(openId) || companyId == null) {
            throw new BusinessException("参数不完整");
        }
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, openId)
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (cc == null) throw new BusinessException("无权限操作该公司员工");
        return cc;
    }

    /** openId + taxNumber 归属校验 */
    private CustomerCompany checkOwnershipByTaxNumber(String openId, String taxNumber) {
        if (StringUtils.isBlank(openId) || StringUtils.isBlank(taxNumber)) {
            throw new BusinessException("参数不完整");
        }
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, openId)
                .eq(CustomerCompany::getTaxNumber, taxNumber)
                .last("LIMIT 1")
                .one();
        if (cc == null) throw new BusinessException("无权限操作该公司员工");
        return cc;
    }
}
