package com.qykj.finmaster.cs.profile.domain.req;

import lombok.Data;
import java.io.Serializable;

@Data
public class SaveProfileReq implements Serializable {
    private Long companyId;
    private String bookkeepingStatus;
    private String filingStatus;
    private Integer starLevel;
    private String remark;
}
