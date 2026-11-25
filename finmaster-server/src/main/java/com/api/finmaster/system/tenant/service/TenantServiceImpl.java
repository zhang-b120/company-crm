package com.qykj.finmaster.system.tenant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.tenant.entity.Tenant;
import com.qykj.finmaster.system.tenant.entity.req.AddTenantReq;
import com.qykj.finmaster.system.tenant.entity.req.GetTenantPageReq;
import com.qykj.finmaster.system.tenant.entity.req.UpdateTenantReq;
import com.qykj.finmaster.system.tenant.entity.resp.GetTenantInfoResp;
import com.qykj.finmaster.system.tenant.entity.resp.GetTenantPageResp;
import com.qykj.finmaster.system.tenant.manager.impl.TenantManagerImpl;
import com.qykj.finmaster.system.tenant.mapper.TenantMapper;
import com.qykj.finmaster.system.user.service.UserServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 租户Service实现类
 */
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant>{


    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private TenantManagerImpl tenantManager;

    public List<GetTenantPageResp> selectTenantPage(GetTenantPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        return baseMapper.selectTenantPage(params);
    }

    public boolean checkTenantCodeUnique(String tenantCode, Long id) {
        if (StringUtils.isEmpty(tenantCode)) {
            return true;
        }
        Tenant existTenant = baseMapper.selectByTenantCodeExcludeId(tenantCode, id);
        return existTenant == null;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean addTenant(AddTenantReq req, LoginUser loginUser) {
        Tenant tenant = CopierCopyUtils.convertTo(req, Tenant.class);
        // 1.验证租户编码唯一性
        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Tenant::getTenantCode,tenant.getTenantCode());
        tenantManager.getBaseMapper().exists(wrapper);
        if (tenantManager.getBaseMapper().exists(wrapper)) {
            throw new BusinessException(MessageFormat.format(ResultMsg.TENANT_ADD_EXISTS_TENANT_CODE, tenant.getTenantCode()));
        }
        if(ObjectUtils.isNotEmpty(loginUser)){
            tenant.setCreateBy(loginUser.getUsername());
            tenant.setCreateByName(loginUser.getRealName());
        }
        tenant.setCreateTime(new Date());
        // 2.保存租户信息
        tenantManager.save(tenant);

        // 3. 自动创建管理员
        userService.createTenantAdmin(req.getDefaultAdminUsername(), req.getDefaultAdminPassword(), req.getDefaultAdminRealName(),
                req.getDefaultAdminEmail(), req.getDefaultAdminPhone(), tenant.getId(), tenant.getTenantCode(), loginUser);
        return Boolean.TRUE;
    }

    /**
     * 更新租户信息
     *
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateTenant(UpdateTenantReq req, LoginUser loginUser) {
        Tenant tenant = CopierCopyUtils.convertTo(req, Tenant.class);
        if (ObjectUtils.isEmpty(tenant.getId())) {
            return Boolean.FALSE;
        }
        if(ObjectUtils.isNotEmpty(loginUser)){
            tenant.setUpdateBy(loginUser.getUsername());
            tenant.setUpdateByName(loginUser.getRealName());
        }
        tenant.setUpdateTime(new Date());
        // 1.修改租户信息
        tenantManager.updateById(tenant);
        return Boolean.TRUE;
    }

    /**
     * 删除租户
     *
     * @param id
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTenant(Long id, LoginUser loginUser) {
        // 检查是否为默认租户
        if (id == 1L) {
            throw new BusinessException(ResultMsg.TENANT_NOT_DELETE_DEFAULT);
        }
        return tenantManager.lambdaUpdate()
                .set(Tenant::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Tenant::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateByName, loginUser.getRealName())
                .eq(Tenant::getId, id)
                .update();
    }

    /**
     * 批量删除租户
     *
     * @param ids
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteTenants(Long[] ids, LoginUser loginUser) {
        if (Arrays.stream(ids).anyMatch(item -> item == 1L)) {
            throw new BusinessException(ResultMsg.TENANT_NOT_DELETE_DEFAULT);
        }
        return tenantManager.lambdaUpdate()
                .set(Tenant::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Tenant::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateByName, loginUser.getRealName())
                .in(Tenant::getId, Arrays.asList(ids))
                .update();
    }

    /**
     * 修改租户状态
     * @param id
     * @param status
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTenantStatus(Long id, Integer status, LoginUser loginUser) {
        return tenantManager.lambdaUpdate()
                .set(Tenant::getStatus, status)
                .set(Tenant::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Tenant::getUpdateByName, loginUser.getRealName())
                .eq(Tenant::getId, id)
                .update();
    }

    /**
     * 获取租户信息
     * @param id
     * @return
     */
    public GetTenantInfoResp getInfo(Long id) {
        GetTenantInfoResp resp = new GetTenantInfoResp();
        Tenant tenant = tenantManager.lambdaQuery()
                .eq(Tenant::getId, id)
                .one();
        if(ObjectUtils.isNotEmpty(tenant)){
            resp = CopierCopyUtils.convertTo(tenant, GetTenantInfoResp.class);
        }
        return resp;
    }
}
