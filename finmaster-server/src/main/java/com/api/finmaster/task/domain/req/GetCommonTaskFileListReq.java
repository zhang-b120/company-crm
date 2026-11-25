package com.qykj.finmaster.task.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-07-16
 * @Description
 */
@Data
public class GetCommonTaskFileListReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 8652481338152277198L;

    private Long taskId;

    /**
     * 文件上传类型
     */
    private String uploadType;

    private String taxNumber;
}