package com.qykj.finmaster.system.bussiness.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.domain.SysBusinessProduct;
import com.qykj.finmaster.system.bussiness.domain.req.AddBusinessProductReq;
import com.qykj.finmaster.system.bussiness.domain.req.AddBusinessReq;
import com.qykj.finmaster.system.bussiness.domain.req.GetBusinessListReq;
import com.qykj.finmaster.system.bussiness.domain.req.UpdateBusinessReq;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessDetailResp;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessListResp;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessManagerImpl;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessProductManagerImpl;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessMapper;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessProductMapper;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务管理 Service
 */
@Service
public class SysBusinessServiceImpl extends ServiceImpl<SysBusinessMapper, SysBusiness> {

    @Autowired
    private SysBusinessManagerImpl sysBusinessManager;
    @Autowired
    private SysBusinessProductManagerImpl sysBusinessProductManager;
    @Autowired
    private SysBusinessMapper sysBusinessMapper;
    @Autowired
    private SysBusinessProductMapper sysBusinessProductMapper;

    /**
     * 查询业务列表（分页）
     */
    public List<GetBusinessListResp> getBusinessList(GetBusinessListReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetBusinessListResp> list = sysBusinessMapper.getBusinessList(params);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        list.forEach(item -> {
            if (item.getCreateTime() != null) {
                item.setCreateTime(DateUtils.dateToLocalDateTime(DateUtils.localDateTimeToDate(item.getCreateTime())));
            }
        });
        return list;
    }

    /**
     * 新增业务
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addBusiness(AddBusinessReq req, Long tenantId, LoginUser loginUser) {
        if (StringUtils.isEmpty(req.getBusinessCode()) || StringUtils.isEmpty(req.getBusinessName())) {
            throw new BusinessException("业务编码和业务名称不能为空！");
        }
        // 校验业务编码唯一
        if (checkBusinessCodeExist(req.getBusinessCode(), null, tenantId)) {
            throw new BusinessException("业务编码已存在，请重新输入！");
        }
        SysBusiness business = CopierCopyUtils.convertTo(req, SysBusiness.class);
        business.setTenantId(tenantId);
        business.setCreateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            business.setCreateBy(loginUser.getUsername());
            business.setCreateByName(loginUser.getRealName());
        }
        return sysBusinessManager.save(business);
    }

    /**
     * 编辑业务
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateBusiness(UpdateBusinessReq req, Long tenantId, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(req.getId())) {
            throw new BusinessException("未获取到要编辑的业务ID！");
        }
        if (StringUtils.isEmpty(req.getBusinessCode()) || StringUtils.isEmpty(req.getBusinessName())) {
            throw new BusinessException("业务编码和业务名称不能为空！");
        }
        // 校验业务编码唯一（排除自身）
        if (checkBusinessCodeExist(req.getBusinessCode(), req.getId(), tenantId)) {
            throw new BusinessException("业务编码已存在，请重新输入！");
        }
        SysBusiness business = CopierCopyUtils.convertTo(req, SysBusiness.class);
        business.setTenantId(tenantId);
        business.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            business.setUpdateBy(loginUser.getUsername());
            business.setUpdateByName(loginUser.getRealName());
        }
        return sysBusinessManager.updateById(business);
    }

    /**
     * 删除业务（逻辑删除：显式将 deleted 置为 1）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBusiness(Long id, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id)) {
            return Boolean.FALSE;
        }
        SysBusiness business = sysBusinessManager.getById(id);
        if (ObjectUtils.isEmpty(business)) {
            return Boolean.TRUE;
        }
        // 使用 lambdaUpdate 显式 SET deleted=1，避免 @TableLogic 导致 updateById 未更新该字段
        boolean ok = sysBusinessManager.lambdaUpdate()
                .set(SysBusiness::getDeleted, 1)
                .set(SysBusiness::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), SysBusiness::getUpdateBy, loginUser != null ? loginUser.getUsername() : null)
                .set(ObjectUtils.isNotEmpty(loginUser), SysBusiness::getUpdateByName, loginUser != null ? loginUser.getRealName() : null)
                .eq(SysBusiness::getId, id)
                .update();
        return ok;
    }

    /**
     * 获取业务详情
     */
    public GetBusinessDetailResp getBusinessDetail(Long id) {
        SysBusiness business = sysBusinessManager.getById(id);
        if (ObjectUtils.isEmpty(business)) {
            return null;
        }
        GetBusinessDetailResp resp = CopierCopyUtils.convertTo(business, GetBusinessDetailResp.class);
        if (business.getCreateTime() != null) {
            resp.setCreateTime(DateUtils.dateToLocalDateTime(business.getCreateTime()));
        }
        if (business.getUpdateTime() != null) {
            resp.setUpdateTime(DateUtils.dateToLocalDateTime(business.getUpdateTime()));
        }
        return resp;
    }

    /**
     * 查询业务下关联的产品列表
     */
    public List<GetBusinessProductResp> getBusinessProductList(Long businessId) {
        if (ObjectUtils.isEmpty(businessId)) {
            return Collections.emptyList();
        }
        return sysBusinessProductMapper.getProductListByBusinessId(businessId);
    }

    /**
     * 新增业务产品关联
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addBusinessProduct(AddBusinessProductReq req, Long tenantId, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(req.getBusinessId()) || CollUtil.isEmpty(req.getProductIdList())) {
            return Boolean.FALSE;
        }
        // 先删除当前业务下已存在的关联
        sysBusinessProductManager.lambdaUpdate()
                .set(SysBusinessProduct::getDeleted, GlobalConstant.Number.ONE_INT)
                .eq(SysBusinessProduct::getBusinessId, req.getBusinessId())
                .update();

        List<SysBusinessProduct> list = req.getProductIdList().stream().map(productId -> {
            SysBusinessProduct info = new SysBusinessProduct();
            info.setBusinessId(req.getBusinessId());
            info.setProductId(productId);
            info.setTenantId(tenantId);
            info.setCreateTime(new Date());
            if (ObjectUtils.isNotEmpty(loginUser)) {
                info.setCreateBy(loginUser.getUsername());
                info.setCreateByName(loginUser.getRealName());
            }
            return info;
        }).toList();
        return sysBusinessProductManager.saveBatch(list);
    }

    /**
     * 校验业务编码是否存在
     */
    private Boolean checkBusinessCodeExist(String code, Long id, Long tenantId) {
        if (StringUtils.isEmpty(code)) {
            return Boolean.FALSE;
        }
        return sysBusinessManager.lambdaQuery()
                .eq(SysBusiness::getBusinessCode, code)
                .eq(SysBusiness::getTenantId, tenantId)
                .ne(ObjectUtils.isNotEmpty(id), SysBusiness::getId, id)
                .exists();
    }
}

