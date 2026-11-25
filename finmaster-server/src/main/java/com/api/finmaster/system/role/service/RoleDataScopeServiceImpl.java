package com.qykj.finmaster.system.role.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.role.entity.RoleDataScope;
import com.qykj.finmaster.system.role.entity.resp.GetRoleDataScopeResp;
import com.qykj.finmaster.system.role.manager.impl.RoleDataScopeManagerImpl;
import com.qykj.finmaster.system.role.mapper.RoleDataScopeMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;

import java.util.Date;
import java.util.List;

@Service
public class RoleDataScopeServiceImpl extends ServiceImpl<RoleDataScopeMapper, RoleDataScope> {

    @Resource
    private RoleDataScopeMapper roleDataScopeMapper;
    @Autowired
    private RoleDataScopeManagerImpl roleDataScopeManager;

    @Transactional(rollbackFor = Exception.class)
    public Boolean saveRoleDataScope(Long roleId, Integer dataScope, List<Long> deptIds, Long tenantId, LoginUser loginUser) {
        RoleDataScope roleDataScope = new RoleDataScope();
        roleDataScope.setRoleId(roleId);
        roleDataScope.setDataScope(dataScope);
        if(CollUtil.isNotEmpty(deptIds)){
            roleDataScope.setDeptIds(String.join(",", deptIds.stream().map(String::valueOf).toArray(String[]::new)));
        }
        saveRoleDataScope(roleDataScope, tenantId, loginUser);
        return Boolean.TRUE;
    }

    public GetRoleDataScopeResp getRoleDataScope(Long roleId) {
        GetRoleDataScopeResp resp = new GetRoleDataScopeResp();
        RoleDataScope roleDataScope = roleDataScopeManager.lambdaQuery()
                .eq(RoleDataScope::getRoleId, roleId)
                .one();
        if(ObjectUtils.isNotEmpty(roleDataScope)){
            resp = CopierCopyUtils.convertTo(roleDataScope, GetRoleDataScopeResp.class);
            resp.setCreateTime(DateUtils.dateToLocalDateTime(roleDataScope.getCreateTime()));
            resp.setUpdateTime(DateUtils.dateToLocalDateTime(roleDataScope.getUpdateTime()));
        }
        return resp;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean saveRoleDataScope(RoleDataScope roleDataScope, Long tenantId, LoginUser loginUser) {
        roleDataScopeMapper.deleteByRoleId(roleDataScope.getRoleId());
        roleDataScopeManager.lambdaUpdate()
                .set(RoleDataScope::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), RoleDataScope::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), RoleDataScope::getUpdateByName, loginUser.getRealName())
                .set(RoleDataScope::getUpdateTime, new Date())
                .eq(RoleDataScope::getRoleId, roleDataScope.getRoleId())
                .update();
        if(ObjectUtils.isNotEmpty(loginUser)) {
            roleDataScope.setCreateBy(loginUser.getUsername());
            roleDataScope.setCreateByName(loginUser.getRealName());
        }
        roleDataScope.setCreateTime(new Date());
        roleDataScope.setTenantId(tenantId);
        roleDataScopeManager.save(roleDataScope);
        return true;
    }
}
