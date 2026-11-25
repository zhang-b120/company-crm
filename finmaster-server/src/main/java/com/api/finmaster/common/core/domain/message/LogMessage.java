package com.qykj.finmaster.common.core.domain.message;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
public class LogMessage {
    /**
     * customer_Log
     */
    public static String CREATE_CUSTOMER = "创建客户:{0}";
    public static String UPDATE_CUSTOMER = "编辑客户信息:{0}";
    public static String RECEIVE_CUSTOMER = "认领客户:{0}";
    public static String LEAD_TO_CUSTOMER_PUBLIC_LOG = "释放线索客户转入公海,原负责人：{0},操作人：{1}";
    public static String LEAD_TO_CUSTOMER_PRIVATE_LOG = "分配线索客户转入私海,现负责人：{0},操作人：{1}";
    public static String RELEASE_CUSTOMER_LOG = "释放客户转入公海,原负责人：{0},操作人：{1}";
    /**
     * lead_log
     */
    public static String CREATE_LEAD_LOG = "创建线索:{0}";
    public static String UPDATE_LEAD_LOG_PRIVATE = "小程序线索自动转化为私海客户,客户名称:{0},负责人:{1}";
    public static String UPDATE_LEAD_LOG_PUBLIC = "小程序线索自动转化为公海客户,客户名称:{0}";
    public static String CUSTOMER_TO_RELEASE_LEAD_LOG = "客户转入公海释放线索,原负责人：{0},操作人：{1}";
    public static String CUSTOMER_TO_ASSIGN_LEAD_LOG = "认领客户分配线索,现负责人：{0},操作人：{1}";
    public static String RELEASE_LEAD_LOG = "释放线索,原负责人：{0},操作人：{1}";


    /**
     * company_log
     */
    public static String CREATE_COMPANY_LOG = "创建公司:{0}";
    public static String UPDATE_COMPANY_LOG = "修改公司,公司名称:{0},法人:{1},企业类型{2}";
    public static String DELETE_COMPANY_LOG = "修改公司:{0}";

    /**
     * order_log
     */
    public static String CREATE_ORDER_LOG = "创建订单";
    public static String UPDATE_ORDER_LOG = "编辑订单";
    public static String UPDATE_ORDER_DETAIL_COMPANY_LOG = "修改订单明细Id:{0}的公司,修改该前公司ID:{1},修改后公司ID:{2}";
    public static String UPDATE_ORDER_LEAD_MONTH_LOG = "修改订单Id:{0}的线索月份,修改该前:{1},修改后:{2}";
    public static String DELETE_ORDER_LOG = "删除订单{0}";
    public static String UPDATE_ORDER_STATUS_LOG = "更新订单状态为:{0}";
}