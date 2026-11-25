package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class UpdateCompanyContactReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "联系人姓名不能为空")
    private String name;

    private String phone;
    private String position;
    private String remark;
}
