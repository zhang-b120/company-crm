package com.qykj.finmaster.data.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-08-28
 * @Description
 */
@Data
public class GetPaymentDataDetailPageReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = -8302321717683368122L;
    /**
     * 起始时间
     */
    private LocalDate start;

    /**
     * 结束时间
     */
    private LocalDate end;

    /**
     * 支付方式
     */
    private String paymentType;

    /**
     * 线索月份
     */
    private String leadMonth;

    /**
     * 销售负责人
     */
    private String salesPerson;

    /**
     * 金额
     */
    private Integer amount;

    /**
     * 订单明细类型（NEW-新增 RENEW-续费 BALANCE-补款），仅作查询条件
     */
    private String orderDetailType;

    /**
     * 部门ID（仅查询该部门的数据）
     */
    private Long deptId;

    /**
     * 排除的部门ID列表（排除这些部门的数据）
     */
    private java.util.List<Long> excludeDeptIds;

    /**
     * 业务名称模糊匹配关键字（仅查询订单明细产品关联的业务名称包含该关键字的数据）
     * 工商业务页签传 "工商"
     */
    private String businessNameContains;

    /**
     * 业务名称模糊匹配排除关键字（排除订单明细产品关联的业务名称包含该关键字的数据）
     * 青艺科技 / 本地财税 / 联盟数商页签传 "工商"
     */
    private String excludeBusinessNameContains;

    public Map<String, Object> toBeanMap() {
        Map<String, Object> map = new HashMap<>();
        BeanWrapper beanWrapper = new BeanWrapperImpl(this);
        for (java.beans.PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
            String propertyName = pd.getName();
            if (!"class".equals(propertyName)) {
                Object value = beanWrapper.getPropertyValue(propertyName);
                if (value != null) {
                    map.put(propertyName, value);
                }
            }
        }
        return map;
    }
}