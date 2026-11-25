package com.qykj.finmaster.sale.fieldwork.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTaskImage;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class FieldWorkTaskResp {

    private Long id;
    private String taskNo;
    private Long customerId;
    private String customerName;
    private Long orderId;
    private String orderNo;
    private String taskType;
    private Long assigneeId;
    private String assigneeName;
    private Long dispatcherId;
    private String dispatcherName;
    private Integer plannedDuration;
    private Integer actualDuration;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date plannedStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date actualEndTime;

    private String status;
    private String description;
    private String completionNotes;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 图片列表（仅详情接口返回） */
    private List<FieldWorkTaskImage> images;
}
