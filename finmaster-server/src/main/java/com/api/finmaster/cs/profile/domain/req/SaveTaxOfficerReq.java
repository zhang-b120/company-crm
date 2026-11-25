package com.qykj.finmaster.cs.profile.domain.req;

import lombok.Data;
import java.io.Serializable;

@Data
public class SaveTaxOfficerReq implements Serializable {
    /** 编辑时传 id，新增时不传 */
    private Long id;
    /** finmaster 公司 ID，用于查找 financial 账套 */
    private Long companyId;
    private String name;
    private String phone;
    private String password;
    /** officer/proxy/legal_person/ca */
    private String loginType;
    private String idCard;
    private Boolean isDefault;
    private String remark;
}
