package com.qykj.finmaster.system.department.entity.resp;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.system.department.entity.Department;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class GetDepartmentResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -290072139757327591L;

    /** 部门ID */
    private Long id;

    /** 父部门ID */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 负责人用户名 */
    private String leaderUsername;

    /** 负责人昵称 */
    private String leaderRealName;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 排序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / NONE */
    private String workbenchType;

    /** 子部门列表（非数据库字段） */
    private List<GetDepartmentResp> children;

    public void setChildren(List<GetDepartmentResp> children) {
        this.children = children;
    }

}