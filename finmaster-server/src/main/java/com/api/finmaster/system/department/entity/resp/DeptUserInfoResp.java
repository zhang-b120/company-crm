package com.qykj.finmaster.system.department.entity.resp;

import lombok.Data;

/**
 * 部门及其员工信息，用于销售业绩配置等场景
 */
@Data
public class DeptUserInfoResp {

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门负责人用户名
     */
    private String leaderUsername;

    /**
     * 部门负责人姓名
     */
    private String leaderRealName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userRealName;
}

