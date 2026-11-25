package com.qykj.finmaster.third.tianyancha.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 天眼查 /ic/holder/2.0 返回的 items[] 子元素
 */
@Data
public class TycHolderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long cgid;
    private String hcgid;
    private String logo;
    /** 股东名 */
    private String name;
    /** 简称 */
    private String alias;
    /** 股东类型 1-公司 2-人 3-其它 */
    private Integer type;
    /** 实缴 */
    private List<CapitalItem> capitalActl;
    /** 认缴 */
    private List<CapitalItem> capital;
    /** 首次持股日期（时间戳） */
    private Long ftShareholding;
    /** 香港企业持股 */
    private HkCapital hkCapital;

    /**
     * 扩展字段：type=1 公司型股东，通过企查查模糊搜索补查的社会统一信用代码
     * 天眼查原始响应不含此字段，由 TianyanchaCompanyService 填入
     */
    private String creditCode;

    @Data
    public static class CapitalItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private String amomon;   // 出资金额
        private String time;     // 出资时间
        private String percent;  // 占比
        private String paymet;   // 出资/认缴方式
    }

    @Data
    public static class HkCapital implements Serializable {
        private static final long serialVersionUID = 1L;
        private String percent;   // 持股占比
        private String quantity;  // 持股数量
    }
}
