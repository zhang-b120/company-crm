package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 外勤任务追加记录
 */
@Data
@TableName("work_order_field_record")
public class WorkOrderFieldRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 所属外勤任务 work_order.id */
    @TableField("work_order_id")
    private Long workOrderId;

    /** 跟进方式 PHONE / WECHAT / VISIT / OTHER */
    @TableField("follow_type")
    private String followType;

    /** 关联通话记录 ID（PHONE 类型时填写，转写完成后回填 content） */
    @TableField("call_record_id")
    private Long callRecordId;

    /** 完成情况说明（外勤）/ 跟进内容（续费） */
    private String content;

    /** 照片 URL 列表（JSON 数组字符串） */
    @TableField("photo_urls")
    private String photoUrls;

    @TableField("create_by")
    private String createBy;

    @TableField("create_by_name")
    private String createByName;

    @TableField("create_time")
    private Date createTime;

    @TableLogic
    private Integer deleted;
}
