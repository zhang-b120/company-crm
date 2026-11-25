package com.qykj.finmaster.task.domain.resp;

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
public class GetCommonTaskFileListResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -2635245797360288469L;

    private Long id;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 保存文件名
     */
    private String fileName;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 上传时间
     */
    private LocalDateTime createTime;
}