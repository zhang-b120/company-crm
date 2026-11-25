package com.qykj.finmaster.system.user.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class UpdatePasswordReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 466705385238879954L;

    private Long id;

    /** 密码 */
    private String password;
}