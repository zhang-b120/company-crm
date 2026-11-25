package com.qykj.finmaster.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.entity.RoleMenu;
import com.qykj.finmaster.system.role.entity.resp.GetRoleInfoResp;
import com.qykj.finmaster.system.role.entity.resp.GetRolePageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 角色Mapper接口
 * 负责角色相关数据库操作
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 分页查询角色列表
     *
     * @param page 分页参数
     * @param name 角色名称
     * @return 角色分页列表
     */
    List<GetRolePageResp> selectRoleList(@Param("params") Map<String, Object> params);

    /**
     * 查询用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> findRolesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户角色列表（不使用租户过滤）
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> findRolesByUserIdWithoutTenant(@Param("userId") Long userId);

    /**
     * 查询角色菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> findMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 删除角色菜单关联
     *
     * @param roleId 角色ID
     */
    void deleteRoleMenus(@Param("roleId") Long roleId);

    /**
     * 插入角色菜单关联
     * @param roleMenu 角色菜单关联
     */
    void insertRoleMenu(RoleMenu roleMenu);

    /**
     * 删除用户角色关联
     *
     * @param userId 用户ID
     */
    void deleteUserRoles(@Param("userId") Long userId);

    /**
     * 批量插入用户角色关联
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    void insertUserRoles(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);

    /**
     * 查询用户角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 批量插入角色菜单关联
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     */
    void insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    /**
     * 获取所有菜单ID（除了租户管理相关菜单）
     * 用于为租户超级管理员角色分配权限
     *
     * @return 菜单ID列表
     */
    @Select("SELECT id FROM sys_menu WHERE deleted = 0 AND enabled = 1 " +
            "AND permission NOT LIKE '%tenant%' " +
            "AND permission NOT LIKE '%Tenant%' " +
            "AND name NOT LIKE '%租户%' " +
            "ORDER BY sort ASC")
    List<Long> getAllMenuIdsExceptTenantManagement();

    /**
     * 获取可分配的角色列表
     * 包含超级管理员角色和当前租户的角色
     *
     * @param tenantId 租户ID
     * @return 可分配的角色列表
     */
    List<GetRoleInfoResp> getAssignableRoles(@Param("tenantId") Long tenantId);
}
