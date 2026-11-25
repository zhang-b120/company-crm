package com.qykj.finmaster.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.role.entity.RoleDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RoleDataScopeMapper extends BaseMapper<RoleDataScope> {

    /**
     * 根据角色ID查询数据权限
     */
    RoleDataScope findByRoleId(@Param("roleId") Long roleId);

    /**
     * 插入或更新数据权限
     */
    void insertOrUpdate(RoleDataScope roleDataScope);

    /**
     * 根据用户id获取部門列表
     * @param userId
     * @return
     */
    String getDataScopeDeptsByUserId(Long userId);

    /**
     * 获取用户的数据權限
     * @param userId
     * @return
     */
    Long getDataScopeByUserId(Long userId);

    Long deleteByRoleId(Long roleId);
}
