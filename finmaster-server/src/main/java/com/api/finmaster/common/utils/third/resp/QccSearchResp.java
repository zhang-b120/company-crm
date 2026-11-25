package com.qykj.finmaster.common.utils.third.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Data
public class QccSearchResp implements Serializable {

    private static final long serialVersionUID = 6823750153575818105L;
    /**
     * 内部KeyNo
     */
    private String keyNo;

    /**
     * 企业名称
     */
    private String name;

    /**
     * 统一社会信用代码，若查询企业为中国香港企业时：该字段返回商业登记号码
     */
    private String creditCode;

    /**
     * 成立日期
     */
    private String startDate;

    /**
     * 法定代表人姓名
     */
    private String operName;

    /**
     * 企业状态
     */
    private String status;

    /**
     * 注册号，若查询企业为中国香港企业时：该字段返回企业编号
     */
    private String no;

    /**
     * 更新日期
     */
    private String address;


}