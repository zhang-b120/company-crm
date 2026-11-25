package com.qykj.finmaster.system.role.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.entity.RoleMenu;
import com.qykj.finmaster.system.role.entity.req.AddRoleReq;
import com.qykj.finmaster.system.role.entity.req.GetRolePageReq;
import com.qykj.finmaster.system.role.entity.req.UpdateRoleReq;
import com.qykj.finmaster.system.role.entity.resp.GetRoleDataScopeResp;
import com.qykj.finmaster.system.role.entity.resp.GetRoleInfoResp;
import com.qykj.finmaster.system.role.entity.resp.GetRolePageResp;
import com.qykj.finmaster.system.role.manager.impl.RoleManagerImpl;
import com.qykj.finmaster.system.role.manager.impl.RoleMenuManagerImpl;
import com.qykj.finmaster.system.role.mapper.RoleMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private RoleManagerImpl roleManager;
    @Autowired
    private RoleMenuManagerImpl roleMenuManager;
    @Autowired
    private RoleDataScopeServiceImpl roleDataScopeService;

    /**
     * 获取角色列表
     * @param req
     * @param tenantId
     * @return
     */
    public List<GetRolePageResp> selectRoleList(GetRolePageReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);

        List<GetRolePageResp> list = roleMapper.selectRoleList(params);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        return list;
    }

    public List<Role> findRolesByUserId(Long userId) {
        // 使用不使用租户过滤的方法，因为用户角色关联需要跨租户查询
        return roleMapper.findRolesByUserIdWithoutTenant(userId);
    }

    /**
     * 角色分配菜单权限
     *
     * @param roleId
     * @param menuIds
     * @param tenantId
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignMenus(Long roleId, List<Long> menuIds, Long tenantId, LoginUser loginUser) {
        if(CollUtil.isEmpty(menuIds)){
            return Boolean.FALSE;
        }
        // 先删除原有菜单
        roleMenuManager.lambdaUpdate()
                .set(RoleMenu::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), RoleMenu::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), RoleMenu::getUpdateByName, loginUser.getRealName())
                .eq(RoleMenu::getRoleId, roleId)
                .update();
        // 添加新菜单
        List<RoleMenu> roleMenuList = menuIds.stream().map(item -> {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(item);
            roleMenu.setTenantId(tenantId);
            if (ObjectUtils.isNotEmpty(loginUser)) {
                roleMenu.setUpdateBy(loginUser.getUsername());
                roleMenu.setCreateByName(loginUser.getRealName());
            }
            roleMenu.setCreateTime(new Date());
            return roleMenu;
        }).toList();
        roleMenuManager.saveBatch(roleMenuList);
        return Boolean.TRUE;
    }

    public List<Long> findMenuIdsByRoleId(Long roleId) {
        List<RoleMenu> roleMenuList = roleMenuManager.lambdaQuery()
                .eq(RoleMenu::getRoleId, roleId)
                .list();
        if(CollUtil.isEmpty(roleMenuList)){
            return Collections.emptyList();
        }
        List<Long> menuIdList = roleMenuList.stream().map(RoleMenu::getMenuId).toList();
        return menuIdList;
    }


    /**
     * 获取可分配的角色列表
     * @param tenantId
     * @return
     */
    public List<GetRoleInfoResp> getAssignableRoles(Long tenantId) {
        List<GetRoleInfoResp> list = baseMapper.getAssignableRoles(tenantId);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * 新增角色
     *
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    public Boolean addRole(AddRoleReq req, Long tenantId, LoginUser loginUser) {
        Role role = CopierCopyUtils.convertTo(req, Role.class);
        if(ObjectUtils.isNotEmpty(loginUser)){
            role.setCreateBy(loginUser.getUsername());
            role.setCreateByName(loginUser.getRealName());
        }
        role.setTenantId(tenantId);
        role.setCreateTime(new Date());
        roleManager.save(role);
        return Boolean.TRUE;
    }

    /**
     * 修改角色
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean updateRole(UpdateRoleReq req, LoginUser loginUser) {
        Role role = CopierCopyUtils.convertTo(req, Role.class);
        if(ObjectUtils.isNotEmpty(loginUser)){
            role.setUpdateBy(loginUser.getUsername());
            role.setUpdateByName(loginUser.getRealName());
        }
        role.setUpdateTime(new Date());
        roleManager.updateById(role);
        return Boolean.TRUE;
    }

    /**
     * 删除角色
     *
     * @param id
     * @param loginUser
     * @return
     */
    public Boolean removeRoleById(Long id, LoginUser loginUser) {
        if (id == GlobalConstant.Number.ONE_INT) {
            throw new BusinessException(ResultMsg.ROLE_NOT_DELETE_DEFAULT);
        }
        roleManager.lambdaUpdate()
                .set(Role::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), Role::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Role::getUpdateByName, loginUser.getRealName())
                .set(Role::getUpdateTime, new Date())
                .eq(Role::getId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 分配数据权限
     *
     * @param roleId
     * @param dataScope
     * @param deptIds
     * @param tenantId
     * @param loginUser
     */
    public Boolean assignDataScope(Long roleId, Integer dataScope, String deptIds, Long tenantId, LoginUser loginUser) {
        List<Long> deptIdList = null;
        if (StringUtils.isNotEmpty(deptIds)) {
            deptIdList = Arrays.stream(deptIds.split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        }
        return roleDataScopeService.saveRoleDataScope(roleId, dataScope, deptIdList, tenantId, loginUser);
    }

    /**
     * 获取数据权限
     * @param roleId
     * @return
     */
    public GetRoleDataScopeResp getRoleDataScope(Long roleId) {
        return roleDataScopeService.getRoleDataScope(roleId);
    }
}
