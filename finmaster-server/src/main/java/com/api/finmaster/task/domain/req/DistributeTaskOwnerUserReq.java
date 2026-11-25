package com.qykj.finmaster.task.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-11
 * @Description
 */
@Data
public class DistributeTaskOwnerUserReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -5409993926814497163L;

    private Long id;

    private List<DistributeDetailOwnerUserReq> detailOwnerUserList;
}