package com.qykj.finmaster.system.menu.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.menu.entity.Menu;
import com.qykj.finmaster.system.menu.entity.req.AddMenuReq;
import com.qykj.finmaster.system.menu.entity.req.UpdateMenuReq;
import com.qykj.finmaster.system.menu.entity.resp.GetMenuListResp;
import com.qykj.finmaster.system.menu.manager.impl.MenuManagerImpl;
import com.qykj.finmaster.system.menu.mapper.MenuMapper;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.service.RoleServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu>{

    @Autowired
    private RoleServiceImpl roleService;
    @Autowired
    private MenuManagerImpl menuManager;

    public List<GetMenuListResp> getUserMenus(Long userId) {
        if(ObjectUtils.isEmpty(userId)){
            return Collections.emptyList();
        }
        //根据用户ID获取用户身份
        List<Role> roles = roleService.findRolesByUserId(userId);
        // 如果身份包含1，返回所有菜单
        if (roles.stream().anyMatch(role -> role.getId() == 1L)) {
            return buildTree(menuManager.list());
        }
        // 否则返回用户有权限的菜单
        return buildTree(baseMapper.findMenusByUserId(userId));
    }

    public List<GetMenuListResp> buildTree(List<Menu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        List<GetMenuListResp> getMenuListRespList = CopierCopyUtils.convertTo(menus, GetMenuListResp.class);
        // 按父ID分组
        Map<Long, List<GetMenuListResp>> parentMap = getMenuListRespList.stream()
                .collect(Collectors.groupingBy(menu -> menu.getParentId() == null ? 0L : menu.getParentId()));
        // 获取顶级菜单
        List<GetMenuListResp> rootMenus = parentMap.getOrDefault(0L, new ArrayList<>());
        // 递归设置子菜单
        rootMenus.forEach(menu -> setChildren(menu, parentMap));
        return rootMenus;
    }

    private void setChildren(GetMenuListResp menu, Map<Long, List<GetMenuListResp>> parentMap) {
        List<GetMenuListResp> children = parentMap.get(menu.getId());
        if (children != null) {
            menu.setChildren(children);
            children.forEach(child -> setChildren(child, parentMap));
        }
    }

    /**
     * 删除菜单
     *
     * @param id
     * @param tenantId
     * @param loginUser
     * @return
     */
    public Boolean removeMenu(Long id, Long tenantId, LoginUser loginUser) {
        menuManager.lambdaUpdate()
                .set(Menu::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Menu::getUpdateBy, loginUser.getUsername())
                .set(Menu::getUpdateByName, loginUser.getRealName())
                .set(Menu::getTenantId, tenantId)
                .set(Menu::getUpdateTime, new Date())
                .eq(Menu::getId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 新增菜单
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    public Boolean addMenu(AddMenuReq req, Long tenantId, LoginUser loginUser) {
        Menu menu = CopierCopyUtils.convertTo(req, Menu.class);
        menu.setTenantId(tenantId);
        menu.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            menu.setCreateBy(loginUser.getUsername());
            menu.setCreateByName(loginUser.getRealName());
        }
        return menuManager.save(menu);
    }

    public Boolean updateMenu(UpdateMenuReq req, Long tenantId, LoginUser loginUser) {
        Menu menu = CopierCopyUtils.convertTo(req, Menu.class);
        menu.setTenantId(tenantId);
        menu.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            menu.setUpdateBy(loginUser.getUsername());
            menu.setUpdateByName(loginUser.getRealName());
        }
        menuManager.updateById(menu);
        return Boolean.TRUE;
    }
}
