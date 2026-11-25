package com.qykj.finmaster.system.serviceMaintenance.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class GetServiceProcessListResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 7460056579897452906L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 流程编码
     */
    private String processCode;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 处理方式 0-人工, 1-自动
     */
    private Integer handMethod;

    /**
     * 备用字段
     */
    private String backupFiled;

    /**
     * 流程描述
     */
    private String description;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;


}
