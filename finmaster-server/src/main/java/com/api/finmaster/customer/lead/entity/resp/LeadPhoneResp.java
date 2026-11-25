package com.qykj.finmaster.customer.lead.entity.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 线索手机号子表条目（PC 端"更多"弹窗用）
 */
@Data
public class LeadPhoneResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 5413218903256412401L;

    private Long id;
    private Long leadId;
    private String phone;
    private String wechat;
    private String openId;
    /** 是否主通道 0/1 */
    private Integer isPrimary;
    /** 通道来源：lead_create / merge_from_lead / mini_program / pc_manual / lead_update / migration_initial */
    private String source;
    /** 若来自合并，源线索ID */
    private Long sourceLeadId;
    private String remark;
    private String createBy;
    private String createByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
