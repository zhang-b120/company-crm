package com.qykj.finmaster.system.appletUser.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Data
public class GetAppletUserInfoReq implements Serializable {
    private static final long serialVersionUID = -1060642954086869583L;

    /**
     * openID
     */
    private String openId;
}