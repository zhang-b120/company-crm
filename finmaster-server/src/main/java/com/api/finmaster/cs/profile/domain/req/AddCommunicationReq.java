package com.qykj.finmaster.cs.profile.domain.req;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AddCommunicationReq implements Serializable {
    private Long companyId;
    private String communicateType;
    private LocalDateTime communicateTime;
    private String content;
    private String remark;
}
