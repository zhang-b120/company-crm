package com.qykj.finmaster.system.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.department.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 部门Mapper接口
 * 负责部门相关数据库操作
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
    
}