package com.qykj.finmaster.common.utils.third.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
@Data
public class GetBaiduAccessTokenResp implements Serializable {

    /**
     * refresh_token
     */
    private String refreshToken;

    /**
     * expires_in
     */
    private Long expiresIn;

    /**
     * scope
     */
    private String scope;

    /**
     * session_key
     */
    private String sessionKey;

    /**
     * access_token
     */
    private String accessToken;

    /**
     * session_secret
     */
    private String sessionSecret;
}