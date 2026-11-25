package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.third.tianyancha.domain.resp.TycHolderItem;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class BatchImportStockholderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "公司ID不能为空")
    private Long companyId;

    /** 股东列表（天眼查结构） */
    private List<TycHolderItem> stockholders;
}
