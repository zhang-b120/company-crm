package com.qykj.finmaster.third.qcc.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企查查模糊查询结果落库对象 qcc_search_result
 */
@Data
@TableName("qcc_search_result")
public class QccSearchResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("source_company_id")
    private Long sourceCompanyId;

    @TableField("source_company_name")
    private String sourceCompanyName;

    @TableField("search_key")
    private String searchKey;

    @TableField("key_no")
    private String keyNo;

    @TableField("name")
    private String name;

    @TableField("credit_code")
    private String creditCode;

    @TableField("start_date")
    private String startDate;

    @TableField("oper_name")
    private String operName;

    @TableField("status")
    private String status;

    @TableField("no")
    private String no;

    @TableField("address")
    private String address;

    @TableField("created_time")
    private Date createdTime;
}
