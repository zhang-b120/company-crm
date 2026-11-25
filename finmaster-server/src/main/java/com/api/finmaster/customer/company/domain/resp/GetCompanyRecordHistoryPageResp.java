package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyRecordHistoryPageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -622649160245531048L;

    private Long id;

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

    /**
     * 文件路径集合
     */
    private List<String> filePathList;
}