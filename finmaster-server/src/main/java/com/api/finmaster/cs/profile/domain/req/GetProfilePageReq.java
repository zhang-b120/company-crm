package com.qykj.finmaster.cs.profile.domain.req;

import lombok.Data;
import java.io.Serializable;

@Data
public class GetProfilePageReq implements Serializable {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String companyName;
    private String contactName;
    private String phone;
    private String accountingType;
}
