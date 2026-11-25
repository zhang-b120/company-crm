package com.qykj.finmaster.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.entity.resp.GetUserPageResp;
import com.qykj.finmaster.system.department.entity.resp.DeptUserInfoResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 用户Mapper接口
 * 负责用户相关数据库操作
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 查询用户列表
     */
    List<GetUserPageResp> selectUserList(@Param("params") Map<String, Object> params);

    /**
     * 查询用户详细信息
     */
    User selectUserById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User selectUserByUsername(String username);

    /**
     * 新增用户
     */
    int insertUser(User user);

    /**
     * 修改用户
     */
    int updateUser(User user);

    /**
     * 删除用户
     */
    int deleteUserById(Long userId);

    /**
     * 重置用户密码
     */
    int resetUserPassword(@Param("userId") Long userId, @Param("password") String password);

    /**
     * 修改用户头像
     */
    int updateUserAvatar(@Param("userId") Long userId, @Param("avatar") String avatar);

    /**
     * 修改用户状态
     */
    int updateUserStatus(@Param("userId") Long userId, @Param("enabled") Boolean enabled);

    /**
     * 查询用户已分配的角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> findRoleIdsByUserId(Long userId);

    /**
     * 查询用户权限标识列表
     * @param userId 用户ID
     * @return 权限标识列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 查询所有权限标识
     * @return 权限标识列表
     */
    List<String> getAllPermissions();

    /**
     * 分页查询用户列表
     *
     * @param page 分页参数
     * @param username 用户名
     * @param realName 真实姓名
     * @param phone 手机号
     * @param enabled 是否启用
     * @return 用户分页列表
     */
    List<GetUserPageResp> selectUserPage(@Param("params") Map<String, Object> params);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 根据用户名查询用户数量
     *
     * @param username 用户名
     * @return 用户数量
     */
    int selectCountByUsername(@Param("username") String username);

    /**
     * 根据部门ids，获取用户信息
     * @param deptIds
     * @return
     */
    List<String> getDataScopeUserIds(@Param("deptIds") Long[] deptIds);
    /**
     * 根据部门ids，获取用户信息
     * @param deptIds
     * @return
     */
    List<Long> getDataScopeUserList(@Param("deptIds") Long[] deptIds);
    /**
     * 根据部门及其所有子部门查询用户列表
     *
     * @param deptId 部门ID（根部门）
     * @param isChild 0-父级 1-子级 其他-全部
     * @return 用户列表
     */
    List<User> selectUsersByDeptWithChildren(@Param("deptId") Long deptId, @Param("isChild") Integer isChild);

    /**
     * 获取所有用户的姓名列表
     * @return 姓名列表
     */
    List<String> selectAllRealNames();

    List<Long> selectDepartmentIds(@Param("id")  Long id);

    List<User> getRoleUser(@Param("roleId") Long roleId);

    /**
     * 根据部门及其所有子部门查询部门与员工信息
     *
     * @param deptId 部门ID（根部门）
     * @return 部门 + 员工信息列表
     */
    List<DeptUserInfoResp> selectDeptUsersWithChildren(@Param("deptId") Long deptId);

    List<User> getDepartmentUser(@Param("departmentId") Long departmentId);
}
