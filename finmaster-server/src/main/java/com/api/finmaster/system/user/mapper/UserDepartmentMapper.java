package com.qykj.finmaster.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.user.entity.UserDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserDepartmentMapper extends BaseMapper<UserDepartment> {

    /**
     * 根据用户ID查询部门ID列表
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> findDepartmentIdsByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的部门关联
     * @param userId 用户ID
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 批量插入用户部门关联
     * @param userId 用户ID
     * @param departmentIds 部门ID列表
     */
    void insertBatch(@Param("userId") Long userId, @Param("departmentIds") List<Long> departmentIds, @Param("tenantId") Long tenantId);
}
