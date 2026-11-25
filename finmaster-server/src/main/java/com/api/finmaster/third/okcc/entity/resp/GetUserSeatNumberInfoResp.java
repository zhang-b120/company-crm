package com.qykj.finmaster.third.okcc.entity.resp;


import lombok.Data;

@Data
public class GetUserSeatNumberInfoResp {
    private Long id;
    /** 用户Id */
    private Long userId;
    /** 座席号 */
    private String seatNumber;
    /** 对接账户 */
    private String outAccount;
    /** 对接密码 */
    private String outPassword;
}
