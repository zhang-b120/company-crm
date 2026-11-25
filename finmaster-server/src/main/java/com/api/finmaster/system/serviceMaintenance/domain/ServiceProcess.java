package com.qykj.finmaster.system.serviceMaintenance.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("service_process")
public class ServiceProcess extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 8853728502818973624L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 流程编码
     */
    @TableField("process_code")
    private String processCode;

    /**
     * 流程名称
     */
    @TableField("process_name")
    private String processName;

    /**
     * 处理方式 0-人工, 1-自动
     */
    @TableField("hand_method")
    private Integer handMethod;

    /**
     * 备用字段
     */
    @TableField("backup_filed")
    private String backupFiled;

    /**
     * 流程描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 负责人
     */
    @TableField("owner_user")
    private String ownerUser;

    /**
     * 负责人名称
     */
    @TableField("owner_user_name")
    private String ownerUserName;

}
