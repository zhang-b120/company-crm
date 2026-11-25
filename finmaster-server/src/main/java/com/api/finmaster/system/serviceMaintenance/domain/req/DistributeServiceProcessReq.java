package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import com.qykj.finmaster.system.serviceMaintenance.domain.dto.ServiceProcessInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-07
 * @Description
 */
@Data
public class DistributeServiceProcessReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -3894121293260106780L;

    private Long id;

    /**
     * 流程数据
     */
    private List<ServiceProcessInfo> processInfoList;
}