package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class AddCompanyLegalPersonReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "公司ID不能为空")
    private Long companyId;

    @NotNull(message = "姓名不能为空")
    private String name;

    private String idCard;
    private String phone;
    private BigDecimal shareholdingRatio;
    private String individualTaxPassword;
    private Integer individualTaxAppDownloaded;
    private String remark;
}
