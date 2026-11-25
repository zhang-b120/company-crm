package com.qykj.finmaster.sale.fieldwork.domain.req;

import lombok.Data;

@Data
public class UpdateStatusReq {

    /**
     * 新状态：IN_PROGRESS（开始外勤）/ COMPLETED（完成外勤）/ CANCELLED（取消）
     */
    private String status;

    /**
     * 完工情况说明（完成外勤时必填）
     */
    private String completionNotes;
}
