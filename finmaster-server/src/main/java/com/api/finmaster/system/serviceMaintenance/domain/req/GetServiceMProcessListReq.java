package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

@Data
public class GetServiceMProcessListReq extends PageEntity{
    @Serial
    private static final long serialVersionUID = -645407389965162475L;

    /**
     * 流程编码
     */
    private String processCode;

    /**
     * 流程名称
     */
    private String processName;

}
