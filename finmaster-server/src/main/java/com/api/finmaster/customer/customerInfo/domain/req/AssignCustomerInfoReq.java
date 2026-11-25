package com.qykj.finmaster.customer.customerInfo.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2026-01-13
 * @Description
 */
@Data
public class AssignCustomerInfoReq implements Serializable {
    private static final long serialVersionUID = 5676870448969030600L;

    /**
     * ID
     */
    private List<Long> ids;

    /**
     * 负责人用户名
     */
    private String ownerUser;

    /**
     * 负责人姓名
     */
    private String ownerUserName;

}