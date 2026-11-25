package com.qykj.finmaster.task.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-11
 * @Description
 */
@Data
public class TaskChangeOwnerUserReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 9106542330056601019L;

    private Long id;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

}