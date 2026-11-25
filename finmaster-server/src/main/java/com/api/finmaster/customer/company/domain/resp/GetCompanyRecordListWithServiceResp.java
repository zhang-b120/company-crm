package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyRecordListWithServiceResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -8548497927546304697L;
    /**
     * 公司ID
     */
    private Long id;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 管家负责人
     */
    private String butlerOwnerUser;

    /**
     * 管家负责人
     */
    private String butlerOwnerUserName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 预约沟通时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 首测沟通时间
     */
    private LocalDateTime firstFollowTime;

    /**
     * 最后沟通时间
     */
    private LocalDateTime newFollowTime;

    /**
     * 最新沟通内容
     */
    private String newFollowContent;

    /**
     * 已服务时长（月）
     */
    private Integer serviceMonth;

    /**
     * 剩余服务时长（月）
     */
    private Integer remainServiceMonth;
}