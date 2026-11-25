package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 保存白名单配置：先按角色圈人，再手动挑选成员
 * 保存时为指定 (task_type, step_code) 以覆盖方式重新设置白名单
 */
@Data
public class SaveRoleConfigReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /** 节点码（可空；null 表示 task_type 级全局） */
    private String stepCode;

    /** 角色 ID（先圈定人员来源） */
    private Long roleId;

    /** 勾选为白名单的用户 ID 列表 */
    private List<Long> userIds;
}
