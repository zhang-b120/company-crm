package com.qykj.finmaster.system.serviceMaintenance.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("service_maintenance_process")
public class ServiceMaintenanceProcess extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 4048538415200743922L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联产品信息ID
     */
    @TableField("service_id")
    private Long serviceId;

    /**
     * 关联流程编码
     */
    @TableField("process_code")
    private String processCode;

    /**
     * 关联流程名称
     */
    @TableField("process_name")
    private String processName;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
