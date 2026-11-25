package com.qykj.finmaster.workbench.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workbench.entity.CommissionTierConfig;
import com.qykj.finmaster.workbench.mapper.CommissionTierMapper;
import com.qykj.finmaster.workbench.service.CommissionTierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 提成阶梯配置 CRUD
 *
 * 仅限管理员配置；普通用户在销售工作台只读消费
 */
@RestController
@RequestMapping("/system/commission-tier")
public class CommissionTierController extends BaseController {

    @Autowired
    private CommissionTierService service;

    @Autowired
    private CommissionTierMapper mapper;

    @GetMapping("/list")
    public AjaxResult list() {
        List<CommissionTierConfig> tiers = service.listAll(getTenantId());
        return AjaxResult.success(tiers);
    }

    @PostMapping
    public AjaxResult add(@RequestBody CommissionTierConfig req) {
        LoginUser u = getLoginUser();
        Long tenantId = getTenantId();
        req.setId(null);
        if (req.getTenantId() == null) {
            req.setTenantId(tenantId == null ? 0L : tenantId);
        }
        if (req.getEnabled() == null) req.setEnabled(1);
        if (req.getIsBusinessReg() == null) req.setIsBusinessReg(0);
        if (req.getSort() == null) req.setSort(0);
        if (u != null) {
            req.setCreateBy(u.getUsername());
            req.setCreateByName(u.getRealName());
        }
        req.setCreateTime(new Date());
        mapper.insert(req);
        return AjaxResult.success();
    }

    @PutMapping
    public AjaxResult update(@RequestBody CommissionTierConfig req) {
        if (req.getId() == null) return AjaxResult.error("id 不能为空");
        LoginUser u = getLoginUser();
        if (u != null) {
            req.setUpdateBy(u.getUsername());
            req.setUpdateByName(u.getRealName());
        }
        req.setUpdateTime(new Date());
        mapper.updateById(req);
        return AjaxResult.success();
    }

    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return AjaxResult.success();
    }
}
