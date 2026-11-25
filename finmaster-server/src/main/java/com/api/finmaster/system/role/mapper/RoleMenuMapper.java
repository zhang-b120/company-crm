package com.qykj.finmaster.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
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
public interface RoleMenuMapper extends BaseMapper<RoleMenu> {

}
