package com.qykj.finmaster.invoice.domain.req;

import lombok.Data;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class GetInvoiceApplyListReq implements Serializable {
    private static final long serialVersionUID = -5374260994976278112L;

    /**
     * 公司ID
     */
    private String companyId;

    /**
     * 购买方名称
     */
    private String buyerCompanyName;

    /**
     * 开始时间
     */
    private LocalDate startDate;

    /**
     * 结束时间
     */
    private LocalDate endDate;

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