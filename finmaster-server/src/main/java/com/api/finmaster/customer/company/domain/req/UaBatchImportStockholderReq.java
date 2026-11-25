package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.third.tianyancha.domain.resp.TycHolderItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 小程序批量导入股东请求
 */
@Data
public class UaBatchImportStockholderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 微信 openId（必填，用于归属校验） */
    @NotBlank(message = "openId 不能为空")
    private String openId;

    /** 公司ID */
    @NotNull(message = "companyId 不能为空")
    private Long companyId;

    /** 股东列表（来自 /ua/tianyancha/search 的 stockholders 字段） */
    private List<TycHolderItem> stockholders;
}
