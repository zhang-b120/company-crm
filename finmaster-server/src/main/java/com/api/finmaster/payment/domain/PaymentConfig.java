package com.qykj.finmaster.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 支付配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_config")
public class PaymentConfig extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -2101272632721485916L;
    /**
     * 线索ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付名称
     */
    @TableField("payment_name")
    private String paymentName;

    /**
     * 支付方式
     */
    @TableField("payment_type")
    private String paymentType;

    /**
     * 商户ID
     */
    @TableField("mch_id")
    private String mchId;

    /**
     * appID
     */
    @TableField("app_id")
    private String appId;

    /**
     * appKey
     */
    @TableField("app_key")
    private String appKey;

    /**
     * 公钥
     */
    @TableField("public_key")
    private String publicKey;

    /**
     * 私钥
     */
    @TableField("private_key")
    private String privateKey;

    /**
     * 证书路径
     */
    @TableField("cert_path")
    private String certPath;

    /**
     * 证书序列号
     */
    @TableField("cert_serial_number")
    private String certSerialNumber;

    /**
     * 私钥文件路径
     */
    @TableField("private_key_path")
    private String privateKeyPath;

    /**
     * 微信V3秘钥
     */
    @TableField("private_key_v3")
    private String privateKeyV3;

    /**
     * 回调路径
     */
    @TableField("notify_url")
    private String notifyUrl;

    /**
     * 退款回调路径
     */
    @TableField("refund_notify_url")
    private String refundNotifyUrl;

    /**
     * 是否启用（0-未启用 1-启用）
     */
    @TableField("enabled")
    private Boolean enabled;
} 