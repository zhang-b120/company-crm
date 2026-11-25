package com.qykj.finmaster.contract.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 合同列表查询请求
 */
@Data
public class GetContractListReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 公司名称（模糊查询）
     */
    private String companyName;

    /**
     * 公司ID（精确）
     */
    private Long companyId;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 合同类型
     */
    private String contractType;

    /**
     * 合同状态
     */
    private String status;

    /**
     * 是否存在订单 0-否 1-是
     */
    private Integer haveOrder;

    /**
     * 是否存在地址 0-否 1-是
     */
    private Integer haveAddress;

    /**
     * 合同开始时间-起
     */
    private String contractStartTimeBegin;

    /**
     * 合同开始时间-止
     */
    private String contractStartTimeEnd;
}

