package com.qykj.finmaster.task.domain.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
public class CommonDetailInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = -4315808112171146737L;
    private Long id;

    /**
     * 任务Id
     */
    private Long taskId;

    /**
     * 执行顺序
     */
    private Integer sort;

    /**
     * 编码
     */
    private String processCode;

    /**
     * name
     */
    private String processName;

    /**
     * 处理方式 0-人工, 1-自动
     */
    private Integer handMethod;

    /**
     * 任务数据
     */
    private String taskData;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

}