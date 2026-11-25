package com.qykj.finmaster.system.serviceMaintenance.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 服务-业务关联（一个服务可关联多个业务）
 */
@Data
@TableName("service_business")
public class ServiceBusiness implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("service_id")
    private Long serviceId;

    @TableField("business_id")
    private Long businessId;
}
