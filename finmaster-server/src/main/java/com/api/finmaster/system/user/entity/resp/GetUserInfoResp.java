package com.qykj.finmaster.system.user.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-09-15
 * @Description
 */
@Data
public class GetUserInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -1764476243815114047L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;
}