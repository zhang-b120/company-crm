package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyListWithServiceReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = 3874045031327300126L;
    /**
     * 关键词（保留：兼容老调用方，前端公司列表已拆分）
     */
    private String key;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 所属客户
     */
    private String customerName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 银行流水上传状态 0-未上传 1-已上传 2-无流水
     */
    private Integer bankStatementUploaded;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 管家负责人
     */
    private String butlerOwnerUserName;

    /**
     * 是否记账户 0-否 1-是
     */
    private Integer isBookkeeping;

    /**
     * 双免个体 0-否 1-是
     */
    private Integer isDoubleExemption;

    /**
     * 统计账期 yyyy-MM-dd（用于银行流水统计）
     */
    private String statsPeriod;

}