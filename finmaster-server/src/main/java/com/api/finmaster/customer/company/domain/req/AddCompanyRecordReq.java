package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-23
 * @Description
 */
@Data
public class AddCompanyRecordReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 7378497395070868204L;

    /**
     * 公司id
     */
    private Long companyId;

    /**
     * 沟通方式
     */
    private String followType;

    /**
     * 沟通时间
     */
    private LocalDateTime followTime;

    /**
     * 沟通内容
     */
    private String followContent;

    /**
     * 下次沟通时间
     */
    private LocalDateTime nextFollowTime;

    /**
     * 重要程度 1-低 2-中 3-高
     */
    private Integer importance;

    /**
     * 备注
     */
    private String remark;
}