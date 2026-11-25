package com.qykj.finmaster.customer.lead.entity.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-30
 * @Description
 */
@Data
public class GetLeadDataDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 9128161683612348806L;

    private Long id;

    /**
     * 负责人用户名
     */
    private String ownerUser;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否有效（0否 1是）
     */
    private Integer isValid;
}