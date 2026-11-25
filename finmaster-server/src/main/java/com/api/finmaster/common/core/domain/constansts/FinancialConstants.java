package com.qykj.finmaster.common.core.domain.constansts;

/**
 * @Author zhb
 * @Date 2025-07-12
 * @Description
 */
public class FinancialConstants {

    /**
     * host
     */
    public static final String FINANCE_HOST = "localhost";

    /**
     * host
     */
    public static final String FINANCE_HOST_OTHER = "192.168.31.117";

    /**
     * port(正式)
     */
    public static final String FINANCE_PORT = "8012";

    /**
     * port(体验)
     */
    public static final String FINANCE_EXPERIENCE_PORT = "8013";

    /**
     * 根据税号获取账套信息
     */
    public static final String GET_ACCOUNT_SETS = "/ua/user/uaGetAccountSetsId";

    /**
     * 根据税号获取账套信息-批量
     */
    public static final String GET_ACCOUNT_SETS_LIST = "/ua/user/uaGetAccountSetsIdList";

    /**
     * 根据税号获取记账信息
     */
    public static final String GET_ACCOUNT_PROGRESS = "/ua/common/getAccountProgressByCreditCode";

    /**
     * 根据税号获取账套信息(创建账套前校验)
     */
    public static final String GET_ACCOUNT_SETS_BEFORE_CREATE = "/OutSystem2CRM/getAccountSetsId";

    /**
     * 创建账套
     */
    public static final String CREATE_ACCOUNT_SET = "/OutSystem2CRM/createAccountSet";

    /**
     * 获取体验账套
     */
    public static final String GET_EXPERIENCE_ACCOUNT_SETS = "/crmAccountSets/crmGetExperienceAccountSets";

    /**
     * 获取体验账套迁移数据
     */
    public static final String GET_EXPERIENCE_MIGRATION_DATA = "/OutSystem2CRM/crmGetExperienceMigrationData";

    public static final String ADD_ACCOUNT_SET_MIGRATION_DATA = "/OutSystem2CRM/crmAddAccountSetMigrationData";

    public static final String UPDATE_INDIVIDUAL_TAX = "/crmAccountSets/updateIndividualTaxCodeByCreditCode";

    public static final String IMPORT_FINANCIAL_INIT = "/account/importFinancialInit";

    public static final String UPDATE_ACCOUNT_SETS_USER = "/OutSystem2CRM/updateAccountSetsUser";

    public static final String UPDATE_ACCOUNT_SETS_DEACTIVATED = "/OutSystem2CRM/deactivated";

    /** 添加员工（单个，按税号定位账套，按身份证号去重） */
    public static final String ADD_EMPLOYEE = "/OutSystem2CRM/addEmployee";

    /** 批量添加员工（每条独立事务，含工资字段） */
    public static final String ADD_EMPLOYEES = "/OutSystem2CRM/addEmployees";

    /** 企业微信群聊采集入库（工单91 / WXKW-06） */
    public static final String COLLECT_WX_GROUP_CHAT = "/OutSystem2CRM/collectWxGroupChat";

    /** 按税号更新账套字段（部分更新，传什么改什么） */
    public static final String UPDATE_ACCOUNT_SET = "/OutSystem2CRM/updateAccountSet";

    /** 按税号更新账套截止日期（续费 / 账套截止日期变更场景） */
    public static final String UPDATE_ACCOUNT_SET_EFFECTIVE_DATE = "/OutSystem2CRM/updateAccountSetEffectiveDate";

    /**
     * 银行流水文件上传
     */
    public static final String BANK_STATEMENT_UPLOAD = "/ua/fileInfo/upload";

    /**
     * 银行流水文件列表
     */
    public static final String BANK_STATEMENT_LIST = "/ua/fileInfo/list";

    /**
     * 银行流水文件删除
     */
    public static final String BANK_STATEMENT_DELETE = "/ua/fileInfo/deleteFile";

    /**
     * 银行库列表（GET，无参；返回 [{ id, bankCode, bankName, enabled, isAutoRecognize }]）
     * 由 CRM 后端代理给前端，避免前端直连财务系统
     */
    public static final String BANK_LIB_LIST = "/OutSystem2CRM/getBankLibList";

    /**
     * 小企业会计准则 - 小规模纳税人
     */
    public static final Long TEMPLATE_ID_SMALL_SCALE = 432597845032308737L;

    /**
     * 小企业会计准则 - 一般纳税人
     */
    public static final Long TEMPLATE_ID_COMMONLY = 432597845602734082L;

    /**
     * 增值税种类 - 一般纳税人
     */
    public static final String VAT_TYPE_COMMONLY = "COMMONLY";

    /**
     * 增值税种类 - 小规模纳税人
     */
    public static final String VAT_TYPE_SMALL_SCALE = "SMALL_SCALE";

    /**
     * 电税手机号
     */
    public static final String ELECTRICITY_TAX_PHONE = "18963408530";

    /**
     * 电税密码
     */
    public static final String ELECTRICITY_TAX_CODE = "123456";

    /**
     * userId
     */
    public static final Long USER_ID = 721059376894490497L;
}