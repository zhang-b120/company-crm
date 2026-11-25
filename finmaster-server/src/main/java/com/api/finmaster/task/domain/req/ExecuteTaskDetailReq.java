package com.qykj.finmaster.task.domain.req;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-12
 * @Description
 */
@Data
public class ExecuteTaskDetailReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -5255975368026011990L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务流程ID
     */
    private Long id;

    /**
     * 任务数据
     */
    private String taskData;
}