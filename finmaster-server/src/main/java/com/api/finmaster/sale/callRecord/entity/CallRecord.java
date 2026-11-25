package com.qykj.finmaster.sale.callRecord.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 通话记录实体，映射 call_record 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("call_record")
public class CallRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 关联线索ID */
    @TableField("lead_id")
    private Long leadId;

    /** 关联客户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 关联公司ID */
    @TableField("company_id")
    private Long companyId;

    /** 联系人姓名 */
    @TableField("contact_name")
    private String contactName;

    /** 被叫号码 */
    @TableField("phone_number")
    private String phoneNumber;

    /** 发起人用户ID */
    @TableField("caller_user_id")
    private Long callerUserId;

    /** 发起人姓名 */
    @TableField("caller_user_name")
    private String callerUserName;

    /**
     * 通话状态
     * 0=待拨出 1=拨出中 2=已完成 3=未接通
     */
    @TableField("call_status")
    private Integer callStatus;

    /** 接通时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("call_start_time")
    private Date callStartTime;

    /** 挂断时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("call_end_time")
    private Date callEndTime;

    /** 通话时长（秒） */
    @TableField("call_duration")
    private Integer callDuration;

    /** 录音访问地址 */
    @TableField("recording_url")
    private String recordingUrl;

    /** 录音本地路径 */
    @TableField("recording_local_path")
    private String recordingLocalPath;

    /**
     * 转写状态
     * 0=无 1=转写中 2=完成 3=失败
     */
    @TableField("transcription_status")
    private Integer transcriptionStatus;

    /** 转写文字 */
    @TableField("transcription_text")
    private String transcriptionText;

    /**
     * 质量分析状态
     * 0=未分析 1=分析中 2=完成 3=失败
     */
    @TableField("quality_status")
    private Integer qualityStatus;

    /** 质量评分 0-100 */
    @TableField("quality_score")
    private Integer qualityScore;

    /** 质量分析详情 JSON */
    @TableField("quality_analysis")
    private String qualityAnalysis;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
