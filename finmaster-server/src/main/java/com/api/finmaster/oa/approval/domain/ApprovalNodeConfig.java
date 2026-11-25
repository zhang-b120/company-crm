package com.qykj.finmaster.oa.approval.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 审批节点配置（JSON反序列化POJO，非数据库表实体）
 */
@Data
public class ApprovalNodeConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点序号 */
    private Integer sort;

    /** 节点名称 */
    private String name;

    /** 审批人类型：USER-指定用户，ROLE-指定角色，LEADER-部门领导 */
    private String approverType;

    /** 审批人ID列表 */
    private List<String> approverIds;

    /** 签署类型：AND=会签（全部通过）/ OR=或签（任一通过），默认AND */
    private String signType = "AND";

    /** 抄送用户列表（账号） */
    private List<String> ccUsers;

    /** 条件分支配置（为空则无条件始终执行） */
    private List<ApprovalCondition> conditions;

    /**
     * 条件配置
     */
    @Data
    public static class ApprovalCondition implements Serializable {
        /** 字段名：amount / duration */
        private String field;
        /** 运算符：GT / LT / GTE / LTE / EQ */
        private String operator;
        /** 比较值 */
        private String value;
    }
}
