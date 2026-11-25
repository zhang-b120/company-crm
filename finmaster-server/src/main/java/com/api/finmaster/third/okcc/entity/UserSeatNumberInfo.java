package com.qykj.finmaster.third.okcc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("user_seat_number_info")
public class UserSeatNumberInfo {

    /** 订单ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户Id */
    @TableField("user_id")
    private Long userId;
    /** 座席号 */
    @TableField("seat_number")
    private String seatNumber;
    /** 对接账户 */
    @TableField("out_account")
    private String outAccount;
    /** 对接密码 */
    @TableField("out_password")
    private String outPassword;


}
