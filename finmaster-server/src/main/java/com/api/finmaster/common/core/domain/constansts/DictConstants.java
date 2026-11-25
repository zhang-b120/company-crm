package com.qykj.finmaster.common.core.domain.constansts;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
public class DictConstants {

    /**
     * 行业字典
     */
    public static final Long INDUSTRY_PARENT = 1L;//父字典
    public static final String INDUSTRY_INTERNET = "internet";//互联网
    public static final String INDUSTRY_FINANCE = "finance";//金融
    public static final String INDUSTRY_EDUCATION = "education";//教育
    public static final String INDUSTRY_MEDICAL = "medical";//医疗
    public static final String INDUSTRY_MANUFACTURING = "manufacturing";//制造业1

    /**
     * 客户等级字典
     */
    public static final Long CUSTOMER_LEVEL = 2L;//父字典
    public static final String CUSTOMER_LEVEL_A = "A";//A
    public static final String CUSTOMER_LEVEL_B = "B";//B
    public static final String CUSTOMER_LEVEL_C = "C";//C
    public static final String CUSTOMER_LEVEL_D = "D";//D

    /**
     * 客户意向字典
     */
    public static final Long CUSTOMER_INTENTION = 3L;//父字典
    public static final String CUSTOMER_INTENTION_HIGH = "high";//高
    public static final String CUSTOMER_INTENTION_MEDIUM = "medium";//中
    public static final String CUSTOMER_INTENTION_LOW = "low";//低

    /**
     * 客户标签字典
     */
    public static final Long CUSTOMER_TAG = 4L;//父字典
    public static final String CUSTOMER_TAG_IMPORTANT = "important";//重要客户
    public static final String CUSTOMER_TAG_POTENTIAL = "potential";//潜在客户
    public static final String CUSTOMER_TAG_OLD = "old";//老客户
    public static final String CUSTOMER_TAG_NEW = "new";//新客户

    /**
     * 客户来源
     */
    public static final Long CUSTOMER_SOURCE = 100L;//父字典
    public static final String CUSTOMER_SOURCE_1 = "small_produce";//小程序注册
    public static final String CUSTOMER_SOURCE_2 = "2";//引流
    public static final String CUSTOMER_SOURCE_3 = "3";//抖音
    public static final String CUSTOMER_SOURCE_4 = "4";//小红书
    public static final String CUSTOMER_SOURCE_5 = "5";//官网
    public static final String CUSTOMER_SOURCE_6 = "6";//客户介绍

    /**
     * 跟进方式
     */
    public static final Long FOLLOW_UP_METHOD = 101L;//父字典
    public static final String FOLLOW_UP_METHOD_1 = "1";//电话
    public static final String FOLLOW_UP_METHOD_2 = "2";//微信

    /**
     * 线索类型
     */
    public static final Long LEAD_TYPE = 106L;//父字典
    public static final String LEAD_TYPE_KEEP_FUNDS = "keep_funds";//留资

    /**
     * 线索日志类型 lead_log.log_type
     */
    public static final Integer LEAD_LOG_TYPE_CREATE   = 1;  // 创建线索
    public static final Integer LEAD_LOG_TYPE_FOLLOW   = 2;  // 跟进
    public static final Integer LEAD_LOG_TYPE_ASSIGN   = 3;  // 分配 / 释放
    public static final Integer LEAD_LOG_TYPE_CONVERT  = 4;  // 转化为客户
    public static final Integer LEAD_LOG_TYPE_MARK     = 5;  // 有效 / 无效标记
    public static final Integer LEAD_LOG_TYPE_MERGE    = 6;  // 合并（家族通道合并入此 lead）

    /**
     * 客户日志类型 customer_log.log_type
     */
    public static final Integer CUSTOMER_LOG_TYPE_CREATE          = 1;  // 创建
    public static final Integer CUSTOMER_LOG_TYPE_FOLLOW          = 2;  // 跟进
    public static final Integer CUSTOMER_LOG_TYPE_CONVERT_FROM_LEAD = 3; // 转化（线索阶段）
    public static final Integer CUSTOMER_LOG_TYPE_EDIT            = 4;  // 编辑
    public static final Integer CUSTOMER_LOG_TYPE_TO_PUBLIC_SEA   = 5;  // 转入公海
    public static final Integer CUSTOMER_LOG_TYPE_CLAIM           = 6;  // 认领
    public static final Integer CUSTOMER_LOG_TYPE_ASSIGN          = 7;  // 分配
    public static final Integer CUSTOMER_LOG_TYPE_DISCARD         = 8;  // 废弃
    public static final Integer CUSTOMER_LOG_TYPE_MERGE_REBIND    = 9;  // 因线索合并归并到目标 lead

    /**
     * 支付状态
     */
    public static final String PAY_STATUS = "payStatus";//支付状态
    public static final String PAY_STATUS_START = "start";//付款中
    public static final String PAY_STATUS_WAIT = "wait";//付款中
    public static final String PAY_STATUS_SUCCESS = "success";//支付成功
    public static final String PAY_STATUS_FAIL = "fail";//支付失败
    public static final String PAY_STATUS_CANCEL = "cancel";//取消支付

    /**
     * 回调状态
     */
    public static final String NOTIFY_STATUS = "notifyStatus";//支付状态
    public static final String NOTIFY_STATUS_START = "start";//发起回调
    public static final String NOTIFY_STATUS_WAIT = "wait";//等待回调
    public static final String NOTIFY_STATUS_SUCCESS = "success";//支付成功

    /**
     * 企业类型 COMPANY_TYPE
     */
    public static final String COMPANY_TYPE = "COMPANY_TYPE";//企业类型
    public static final String COMPANY_TYPE_SCALE = "small-scale-tax";//小规模纳税人
    public static final String COMPANY_TYPE_GENERAL = "general-tax";//一般纳税人

    /**
     * 发票抬头类型 INVOICE_TITLE_TYPE
     */
    public static final String INVOICE_TITLE_TYPE = "INVOICE_TITLE_TYPE";//发票抬头类型
    public static final String INVOICE_TITLE_TYPE_PERSON = "person";//个人
    public static final String INVOICE_TITLE_TYPE_COMPANY = "company";//企业

    /**
     * 发票类型 INVOICE_TYPE
     */
    public static final String INVOICE_TYPE = "INVOICE_TYPE";//发票类型
    public static final String INVOICE_TYPE_GENERAL = "electronic-general";//电子普票
    public static final String INVOICE_TYPE_SPECIAL = "electronic-special";//电子专票

    /**
     * 发票状态 INVOICE_STATUS
     */
    public static final String INVOICE_STATUS = "INVOICE_STATUS";//发票状态
    public static final String INVOICE_STATUS_APPLY = "apply";//已申请
    public static final String INVOICE_STATUS_SUCCESS = "success";//开票成功

    /**
     * 服务流程 SERVICE_PROCESS
     */
    public static final String SERVICE_PROCESS = "SERVICE_PROCESS"; //服务流程
    public static final String SERVICE_PROCESS_INVOICE_PROCESS = "INVOICE_PROCESS"; //开通发票服务
    public static final String SERVICE_PROCESS_CREATE_ACCOUNT_SET = "CREATE_ACCOUNT_SET"; //创建账套
    public static final String SERVICE_PROCESS_ACCOUNT_SET_DATA_MIGRATION = "ACCOUNT_SET_DATA_MIGRATION"; //数据迁移
    public static final String SERVICE_PROCESS_VERITY_TAX = "VERITY_TAX"; //验证税密
    public static final String SERVICE_PROCESS_ADD_TAX_PERSON = "ADD_TAX_PERSON"; //添加办税员
    public static final String SERVICE_PROCESS_VERITY_PERSON_TAX = "VERITY_PERSON_TAX"; //验证个税
    public static final String SERVICE_PROCESS_FINANCE_DEAL = "FINANCE_DEAL"; //财务处理
    public static final String SERVICE_PROCESS_OLD_ACCOUNT_IMPORT = "OLD_ACCOUNT_IMPORT"; //旧账导入

    /**
     * 通用任务状态
     */
    public static final String COMMON_TASK_STATUS = "COMMON_TASK_STATUS"; //通用任务状态
    public static final String COMMON_TASK_STATUS_PENDING = "PENDING"; //待处理
    public static final String COMMON_TASK_STATUS_PROCESSING = "PROCESSING"; //处理中
    public static final String COMMON_TASK_STATUS_COMPLETED = "COMPLETED"; //已完成
    public static final String COMMON_TASK_STATUS_CANCELLED = "CANCELLED"; //已取消
    public static final String COMMON_TASK_STATUS_PUTUP = "PUTUP"; //挂起

    /**
     * 通用任务状态
     */
    public static final String COMMON_TASK_DETAIL_STATUS = "COMMON_TASK_STATUS"; //通用任务状态
    public static final String COMMON_TASK_DETAIL_STATUS_PENDING = "PENDING"; //待处理
    public static final String COMMON_TASK_DETAIL_STATUS_COMPLETED = "COMPLETED"; //已完成
    public static final String COMMON_TASK_DETAIL_STATUS_CANCELLED = "CANCELLED"; //已取消
    public static final String COMMON_TASK_DETAIL_STATUS_PUTUP = "PUTUP"; //挂起

    /**
     * 付款方式
     */
    public static final String PAYMENT_TYPE = "PAYMENT_TYPE"; //通用任务状态
    public static final String PAYMENT_TYPE_WX_APPLET_HZZ = "WX_APPLET_HZZ"; //小程序付款-彗做账
    public static final String PAYMENT_TYPE_PUBLIC_QYGL_MS = "PUBLIC_QYGL_MS"; //对公-青艺管理咨询-民生
    public static final String PAYMENT_TYPE_PUBLIC_QYKJ_NS = "PUBLIC_QYKJ_NS"; //对公-青艺科技-农商
    public static final String PAYMENT_TYPE_PUBLIC_QYKJ_MS = "PUBLIC_QYKJ_MS"; //对公-青艺科技-民生
    public static final String PAYMENT_TYPE_QRCODE_QYGL = "QRCODE_QYGL"; //扫码-青艺管理咨询
    public static final String PAYMENT_TYPE_QRCODE_QYKJ = "QRCODE_QYKJ"; //扫码-青艺科技
    public static final String PAYMENT_TYPE_OTHER = "OTHER"; //其他

    /**
     * 付款方式
     */
    public static final String PAYMENT_TYPE_LMSS = "PAYMENT_TYPE_LMSS"; //通用任务状态
    public static final String PAYMENT_TYPE_LMSS_WXPAY = "WXPAY_LMSS"; //微信支付



    /**
     * 订单状态 PENDING待付款、PAID已付款、CANCELLED已取消，REFUNDING退款中， REFUND已退款
     */
    public static final String ORDER_STATUS = "ORDER_STATUS"; //订单状态
    public static final String ORDER_STATUS_PENDING = "PENDING"; //待付款
    public static final String ORDER_STATUS_PAID = "PAID"; //已付款
    public static final String ORDER_STATUS_CANCELLED = "CANCELLED"; //已取消
    public static final String ORDER_STATUS_REFUNDING = "REFUNDING"; //退款中
    public static final String ORDER_STATUS_REFUNDED = "REFUNDED"; //已退款


    /**
     * 订单类型 正常订单-NORMAL，退款订单-REFUND，补款订单-BALANCE
     */
    public static final String ORDER_TYPE = "ORDER_TYPE"; //订单类型
    public static final String ORDER_TYPE_NORMAL = "NORMAL"; //正常订单
    public static final String ORDER_TYPE_REFUND = "REFUND"; //退款订单
    public static final String ORDER_TYPE_BALANCE = "BALANCE"; //补款订单

    /**
     * 订单明细类型 NEW-新增 RENEW-续费 BALANCE-补款 REFUND-退款
     */
    public static final String ORDER_DETAIL_TYPE = "ORDER_DETAIL_TYPE";
    public static final String ORDER_DETAIL_TYPE_NEW = "NEW";
    public static final String ORDER_DETAIL_TYPE_RENEW = "RENEW";
    public static final String ORDER_DETAIL_TYPE_BALANCE = "BALANCE";
    public static final String ORDER_DETAIL_TYPE_REFUND = "REFUND";

    /**
     * 公司合作状态 COOPERATING-合作中 CANCELLED-合作取消 DEREGISTERED-注销
     * （对应 customer_company.company_status；注意区别于 reg_status 工商状态）
     */
    public static final String COMPANY_STATUS_COOPERATING = "COOPERATING"; //合作中
    public static final String COMPANY_STATUS_CANCELLED = "CANCELLED"; //合作取消
    public static final String COMPANY_STATUS_DEREGISTERED = "DEREGISTERED"; //注销

    /** 补款业务编码（业务编码为此值时该业务下明细类型为补款） */
    public static final String BUSINESS_CODE_BALANCE = "BALANCE";

    /**
     * 订单退款状态 REJECT-驳回 PASS-通过 REVIEW-待审核
     */
    public static final String ORDER_REFUND_STATUS = "ORDER_REFUND_TYPE"; //订单退款状态
    public static final String ORDER_REFUND_STATUS_REJECT = "REJECT"; //驳回
    public static final String ORDER_REFUND_STATUS_PASS = "PASS"; //通过
    public static final String ORDER_REFUND_STATUS_REVIEW = "REVIEW"; //待审核

    /**
     * 合同状态 DRAFT-草稿 COMPLETED-已完成 TERMINATED-已终止 DISUSED-已废弃
     */
    public static final String CONTRACT_STATUS_DRAFT = "DRAFT";
    public static final String CONTRACT_STATUS_COMPLETED = "COMPLETED";
    public static final String CONTRACT_STATUS_TERMINATED = "TERMINATED";
    public static final String CONTRACT_STATUS_DISUSED = "DISUSED";

    /**
     * 合同类型 新增合同-contract_new 续费合同-contract_renew
     */
    public static final String CONTRACT_TYPE_NEW = "contract_new";
    public static final String CONTRACT_TYPE_RENEW = "contract_renew";

    /** 合同转任务单标识：0-否 1-是 2-已转 */
    public static final int TRANSFER_TASK_NO = 0;
    public static final int TRANSFER_TASK_YES = 1;
    public static final int TRANSFER_TASK_TRANSFERRED = 2;
}