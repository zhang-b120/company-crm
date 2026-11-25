package com.qykj.finmaster.task.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
public class GetCommonTaskPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = -6448191437636471809L;

    /**
     * 任务编号
     */
    private String taskNo;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 来源单据号编码
     */
    private String sourceNumber;

    /**
     * 当前负责人
     */
    private String currentOwnerUserName;

    /**
     * 状态
     */
    private String status;
}