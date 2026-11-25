package com.qykj.finmaster.customer.leadLog.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-07-30
 * @Description
 */
@Data
public class AddLeadLog implements Serializable {
    @Serial
    private static final long serialVersionUID = -3316404996358519446L;

    /** 线索ID */
    private Long leadId;

    /** 日志类型（1创建线索 2跟进记录 3分配线索 4转化线索 5有效无效标记） */
    private Integer logType;

    /** 日志内容 */
    private String content;

    /** 下次跟进时间 */
    private Date nextFollowTime;

    /** 跟进方式 */
    private String followMethod;
}