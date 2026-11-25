package com.qykj.finmaster.third.okcc.entity.resp;

import lombok.Data;

/**
 * 外呼系统Response
 */
@Data
public class OutPhoneResp {

    // 返回状态
    private String status;
    // 返回信息
    private String msg;
    // 返回数据
    private String data;
}
