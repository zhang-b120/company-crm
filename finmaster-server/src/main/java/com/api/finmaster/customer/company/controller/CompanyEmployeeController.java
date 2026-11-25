package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 公司员工名册 CRUD（为公司详情页「人员信息」Tab + 代账派工预填用）
 */
@RestController
@RequestMapping("/company-employee")
public class CompanyEmployeeController extends BaseController {

    @Autowired
    private CompanyEmployeeManager manager;

    @GetMapping("/list")
    public Result<List<CompanyEmployee>> list(@RequestParam Long companyId) {
        return Result.ok(manager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, companyId)
                .orderByAsc(CompanyEmployee::getId)
                .list());
    }

    @PostMapping
    public Result<CompanyEmployee> add(@RequestBody CompanyEmployee body) {
        LoginUser user = getLoginUser();
        Date now = new Date();
        body.setId(null);
        body.setTenantId(getTenantId());
        body.setDeleted(0);
        body.setCreateBy(user.getUsername());
        body.setCreateByName(user.getRealName());
        body.setCreateTime(now);
        body.setUpdateTime(now);
        manager.save(body);
        return Result.ok(body);
    }

    @PutMapping
    public Result<Void> update(@RequestBody CompanyEmployee body) {
        LoginUser user = getLoginUser();
        body.setTenantId(null); // 不允许改 tenant
        body.setCompanyId(null); // 不允许改公司归属
        body.setUpdateBy(user.getUsername());
        body.setUpdateByName(user.getRealName());
        body.setUpdateTime(new Date());
        manager.updateById(body);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        manager.removeById(id);
        return Result.ok();
    }
}
