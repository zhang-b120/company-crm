package com.qykj.finmaster.system.product.domain.req;

import lombok.Data;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class AddProductionReq implements Serializable {
    private static final long serialVersionUID = -7234267601332062613L;
    /**
     * 产品ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编号
     */
    private String productNumber;

    /**
     * 产品类型（SOFTWARE-软件产品，HARDWARE-硬件产品，SERVICE-服务）
     */
    private String productType;

    /**
     * 单位
     */
    private String unit;

    private String unitId;

    /**
     * 规格
     */
    private String specification;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 最大折扣比例
     */
    private Integer maxDiscount;

    /**
     * 产品描述
     */
    private String description;

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
