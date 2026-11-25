package com.qykj.finmaster.contract.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合同列表响应
 */
@Data
public class GetContractListResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String contractNo;

    private Long companyId;

    private String companyName;

    private Long customerId;

    private String customerName;

    private Long businessId;

    private String businessName;

    private String contractType;

    private LocalDateTime contractStartTime;

    private LocalDateTime contractEndTime;

    private BigDecimal contractPrice;

    private Integer haveOrder;

    private Integer haveAddress;

    private String status;

    private Integer transferTask;

    private String createByName;

    private LocalDateTime createTime;

    private String remark;

    /** 关联的最新顶级工单 id（已派工时非空，前端按这个判断"派工"按钮显示为"工单详情"） */
    private Long workOrderId;

    /** 关联工单单号（前端展示用） */
    private String workOrderNo;

    /** 已生成合同文件 URL（空表示未生成；非空表示已生成可下载） */
    private String contractFileUrl;
}

