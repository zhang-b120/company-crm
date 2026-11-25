package com.qykj.finmaster.customer.lead.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class LeadNotificationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 线索ID */
    private Long leadId;

    /** 线索名称 */
    private String leadName;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 线索来源 */
    private String source;

    /** 创建时间 */
    private Date createTime;

    /** 通知类型: "new" 新建 / "assign" 分配 */
    private String notificationType;
}
