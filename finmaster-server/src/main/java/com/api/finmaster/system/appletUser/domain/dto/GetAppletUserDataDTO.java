package com.qykj.finmaster.system.appletUser.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class GetAppletUserDataDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -4511374066509116875L;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 小程序用户名
     */
    private String userName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}