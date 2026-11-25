package com.qykj.finmaster.task.domain.req;

import lombok.Data;

@Data
public class SubjectReq {

    /**
     * 科目编码
     */
    private String code;
    /**
     * 科目名称
     */
    private String name;
    /**
     * 余额方向
     */
    private String balanceDirection;
    /**
     * 科目类别
     */
    private String category;

    private String quantityAccounting;

    private String auxiliaryAccounting;

    /**
     * 旧账科目编码
     */
    private String oldAccountCode;

}