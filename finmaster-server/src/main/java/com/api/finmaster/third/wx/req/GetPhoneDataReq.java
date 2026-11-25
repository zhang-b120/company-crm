package com.qykj.finmaster.third.wx.req;

import lombok.Data;

/**
 * @Author zhb
 * @Date 2025-05-27
 * @Description
 */
@Data
public class GetPhoneDataReq {

    private String encryptedData;
    private String code;
    private String iv;
}