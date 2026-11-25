package com.qykj.finmaster.data.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class GetLeadAnalysisDataReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 3531669772305174489L;

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 开始日期
     */
    private LocalDate start;

    /**
     * 结束日期
     */
    private LocalDate end;
}