package com.qykj.finmaster.system.appletUser.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-21
 * @Description
 */
@Data
public class AddAppletUserRecommendReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 4643762831589585113L;

    /**
     * 推荐人openId
     */
    private String recommendOpenId;

    /**
     * 被推荐人openId
     */
    private String recommendedOpenId;
}