package com.qykj.finmaster.system.department.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.department.entity.Department;
import com.qykj.finmaster.system.department.entity.req.AddDepartmentReq;
import com.qykj.finmaster.system.department.entity.req.UpdateDepartmentReq;
import com.qykj.finmaster.system.department.entity.resp.GetDepartmentResp;
import com.qykj.finmaster.system.department.manager.impl.DepartmentManagerImpl;
import com.qykj.finmaster.system.department.mapper.DepartmentMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>{

    @Autowired
    private DepartmentManagerImpl departmentManager;

    public List<GetDepartmentResp> buildTree(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return new ArrayList<>();
        }

        // 确保所有部门的parentId不为null
        departments.forEach(dept -> {
            if (dept.getParentId() == null) {
                dept.setParentId(0L);
            }
        });
        List<GetDepartmentResp> respList = CopierCopyUtils.convertTo(departments, GetDepartmentResp.class);
        // 按parentId分组
        Map<Long, List<GetDepartmentResp>> childrenMap = respList.stream().collect(Collectors.groupingBy(GetDepartmentResp::getParentId));
        // 构建树形结构
        List<GetDepartmentResp> result = new ArrayList<>();
        respList.forEach(dept -> {
            if (dept.getParentId() == 0) {
                buildChildren(dept, childrenMap);
                GetDepartmentResp getDepartmentResp = CopierCopyUtils.convertTo(dept, GetDepartmentResp.class);
                result.add(getDepartmentResp);
            }
        });

        return result;
    }

    /**
     * 递归构建子部门树
     * @param parent 父部门
     * @param childrenMap 子部门映射
     */
    private void buildChildren(GetDepartmentResp parent, Map<Long, List<GetDepartmentResp>> childrenMap) {
        List<GetDepartmentResp> children = childrenMap.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            // 递归处理每个子部门
            children.forEach(child -> buildChildren(child, childrenMap));
        } else {
            parent.setChildren(new ArrayList<>());
        }
    }

    public boolean save(Department department) {
        // 生成部门编码
        if (department.getCode() == null || department.getCode().trim().isEmpty()) {
            department.setCode(generateDepartmentCode(department));
        }
        // 确保parentId不为null
        if (department.getParentId() == null) {
            department.setParentId(0L);
        }
        //查询是否有该部门编码
       Department i = super.getById(department.getId());
        return super.save(department);
    }

    /**
     * 生成部门编码
     * 规则：父部门编码 + 当前部门序号（3位数字）
     */
    private String generateDepartmentCode(Department department) {
        String prefix = "DEPT";
        if (department.getParentId() != null && department.getParentId() != 0) {
            Department parent = this.getById(department.getParentId());
            if (parent != null) {
                prefix = parent.getCode();
            }
        }

        // 查询同级部门数量
        long count = baseMapper.selectCount(new QueryWrapper<Department>()
                .eq("parent_id", department.getParentId() != null ? department.getParentId() : 0)
                .eq("deleted", 0));

        // 生成3位序号
        String sequence = String.format("%03d", count + 1);
        return prefix + sequence;
    }

    public boolean hasChildDept(Long deptId) {
        return baseMapper.selectCount(new QueryWrapper<Department>()
                .eq("parent_id", deptId)
                .eq("deleted", 0)) > 0;
    }

    /**
     * 获取部门列表
     * @return
     */
    public List<GetDepartmentResp> selectDepartmentList() {
        List<Department> departmentList = departmentManager.list();
        if (CollUtil.isEmpty(departmentList)) {
            return Collections.emptyList();
        }
        return this.buildTree(departmentList);
    }

    /**
     * 新增部门
     *
     * @param req
     * @param tenantId
     * @param loginUser
     */
    public Boolean addDepartment(AddDepartmentReq req, Long tenantId, LoginUser loginUser) {
        Department department = CopierCopyUtils.convertTo(req, Department.class);
        department.setTenantId(tenantId);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            department.setCreateBy(loginUser.getUsername());
            department.setCreateByName(loginUser.getRealName());
        }
        department.setCreateTime(new Date());
        department.setUpdateTime(new Date());
        departmentManager.save(department);
        return Boolean.TRUE;
    }

    /**
     * 修改部门
     *
     * @param req
     * @param tenantId
     * @param loginUser
     */
    public Boolean updateDepartment(UpdateDepartmentReq req, Long tenantId, LoginUser loginUser) {
        Department department = CopierCopyUtils.convertTo(req, Department.class);
        department.setTenantId(tenantId);
        department.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)) {
            department.setUpdateBy(loginUser.getUsername());
            department.setUpdateByName(loginUser.getRealName());
        }
        departmentManager.updateById(department);
        return Boolean.TRUE;
    }

    /**
     * 删除公司
     * @param id
     * @param loginUser
     * @return
     */
    public Boolean deleteDepartment(Long id, LoginUser loginUser) {
        departmentManager.lambdaUpdate()
                .set(Department::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), Department::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Department::getUpdateByName, loginUser.getRealName())
                .set(Department::getUpdateTime, new Date())
                .eq(Department::getId, id)
                .update();
        return Boolean.TRUE;
    }
}
