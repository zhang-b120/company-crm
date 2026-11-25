package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AddServiceProcessListReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -6555912112776524159L;

    /**
     * 流程编码
     */
    private String processCode;

    /**
     * 服务流程名称
     */
    private String processName;

    /**
     * 服务流程描述
     */
    private String description;

    /**
     * 处理方式 0-人工, 1-自动
     */
    private Integer handMethod;

    /**
     * 备用字段
     */
    private String backupFiled;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

}
