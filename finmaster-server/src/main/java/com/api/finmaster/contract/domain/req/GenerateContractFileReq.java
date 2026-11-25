package com.qykj.finmaster.contract.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 生成合同文件请求。
 * 两阶段调用：
 *  第一阶段（preparePreview）：传 contractId + supplement，返回预填的 16 个占位符值
 *  第二阶段（generateFile）：传 contractId + values（用户在预览页编辑后的全量值）
 */
@Data
public class GenerateContractFileReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 合同 ID（两阶段都需要） */
    private Long contractId;

    /** 第一阶段：模板补充字段 ${supplement} 的初始值（可空） */
    private String supplement;

    /** 第二阶段：用户预览页编辑后的全量占位符值 */
    private Map<String, String> values;
}
