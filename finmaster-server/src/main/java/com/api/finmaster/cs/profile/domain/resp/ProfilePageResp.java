package com.qykj.finmaster.cs.profile.domain.resp;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class ProfilePageResp implements Serializable {

    // ---- finmaster 字段 ----
    private Long companyId;
    private String companyName;
    private String taxNumber;
    /** 是否个体工商户 0=否 1=是 */
    private Integer isIndividual;
    private String contactName;
    private String phone;
    private Long profileId;
    private String bookkeepingStatus;
    private String filingStatus;
    /** 客户星级 1-5，null=未设置 */
    private Integer starLevel;
    private String remark;

    // ---- financial 字段（跨库读取）----
    /** financial 账套ID */
    private Long accountSetId;
    /** 增值税类型：SMALL_SCALE / COMMONLY */
    private String vatType;
    /** 会计类型中文名 */
    private String vatTypeName;
    /** 个税密码 */
    private String individualTaxCode;
    /** 最新个税验证结果：0=未执行, 1=失败, 2=成功，null=无账套 */
    private Integer individualTaxResult;
    /** 个税验证状态名称 */
    private String individualTaxResultName;
    /** 办税员列表（默认办税员排第一） */
    private List<TaxOfficerItem> taxOfficers;

    @Data
    public static class TaxOfficerItem {
        private Long id;
        private String name;
        private String phone;
        private String password;
        private String loginType;
        private Boolean isDefault;
        private String remark;
    }
}
