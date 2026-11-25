package com.qykj.finmaster.common.core.domain.message;

/**
 * @Author zhb
 * @Date 2025-07-12
 * @Description
 */
public class ResultMsg {

    public static String COMMON_NOT_HAVE_ROLE = "您无权限进行此操作，请联系上级领导或管理员！";
    public static String PARAM_IS_NULL = "参数不能为空";

    /**
     * 公司
     */
    public static String CUSTOMER_COMPANY_CREATE_SUCCESS = "{0}体验账套已创建";
    public static String CUSTOMER_COMPANY_CREATE_FAIL = "体验账套创建失败, 请联系工作人员。";
    public static String CUSTOMER_COMPANY_CREATE_FAIL_REPEAT = "{0}体验账套已存在，无需重复创建。";
    public static String CUSTOMER_COMPANY_CREATE_FAIL_FORMAL = "您已获得相关权益，无需创建体验账套。请耐心等待！";
    public static String CUSTOMER_COMPANY_CREATE_FAIL_HAVE = "您已存在正式账套，无需创建体验账套。请联系客服处理！";
    public static String CUSTOMER_COMPANY_CREATE_FAIL_NOT_COMPANY_TYPE = "{0}体验账套创建失败, 请联系工作人员设置公司类型。";
    public static String CUSTOMER_COMPANY_CREATE_FAIL_IS_HAVE = "当前公司已存在有效归属，请确认公司信息。";
    public static String CUSTOMER_COMPANY_NOT_DATA = "操作失败，未获取到公司信息，请查询后再进行操作！";
    public static String CUSTOMER_COMPANY_ADD_NOT_OPENID = "请先带领客户体验微信小程序！（注：优先选择让客户在小程序添加公司！）";

    /**
     * 账套迁移
     */
    public static String MIGRATION_NOT_COMPANY_ID = "公司ID为空，请联系技术人员。";
    public static String MIGRATION_NOT_COMPANY_DATA = "未查询到公司数据，请联系技术人员。";
    public static String MIGRATION_NOT_EXPERIENCE = "当前公司没有体验账套，无需迁移！";
    public static String MIGRATION_DATA_FAIL = "数据迁移失败，请联系技术人员。";
    public static String MIGRATION_DATA_REPEAT = "数据以迁移成功，请勿重复迁移";
    public static String MIGRATION_NOT_DATA = "无体验数据，无需迁移";

    /**
     * 发票抬头
     */
    public static String INVOICE_TITLE_SUCCESS = "保存成功";
    public static String INVOICE_TITLE_NOT_DATA = "保存失败，未获取到有效数据。";
    public static String INVOICE_TITLE_NOT_COMPANY = "保存失败，未查询到有效公司，请联系客服。";

    /**
     * 发票信息
     */
    public static String INVOICE_APPLY_SUCCESS = "保存成功";
    public static String INVOICE_APPLY_NOT_DATA = "保存失败，未获取到有效数据。";
    public static String INVOICE_APPLY_NOT_DETAIL_DATA = "保存失败，未获取到发票明细数据。";
    public static String INVOICE_APPLY_NOT_COMPANY = "保存失败，未查询到有效公司，请联系客服。";
    public static String INVOICE_APPLY_NOT_TITLE = "保存失败，请先填写并保存销售方抬头。";

    /**
     * 订单
     */
    public static String ORDER_NOT_DATA = "操作失败，未获取到订单信息，请查询后再进行操作！";
    public static String ORDER_DETAIL_NOT_DATA = "操作失败，未获取到订单明细信息，请查询后再进行操作！";

    /**
     * 线索
     */
    public static String LEAD_ADD_EXISTS_PHONE = "该手机号已存在，不允许重复添加!";
    public static String LEAD_ADD_EXISTS_WECHAT = "该微信号已存在，不允许重复添加";
    public static String LEAD_UPDATE_EXISTS_PHONE = "该手机号已存在，不允许修改!";
    public static String LEAD_UPDATE_EXISTS_WECHAT = "该微信号已存在，不允许修改";
    public static String LEAD_ASSIGN_HAS_OWNER_SINGLE = "线索\"{0}\"已有负责人，不允许分配";
    public static String LEAD_ASSIGN_HAS_OWNER_MULTIPLE = "选中的线索中包含已有负责人的线索，不允许分配";
    public static String LEAD_ASSIGN_NOT_HAVE_LOGIN_USER = "未获取到有效操作人,请重新登录";
    public static String LEAD_ASSIGN_NOT_HAVE_ROLE = "您无权限进行此操作，请联系上级领导或管理员！";

    /**
     * 用户
     */
    public static String USER_USERNAME_PASSWORD_ERROR = "用户名或密码错误";
    public static String USER_USERNAME_DISABLE = "账号已被禁用";
    public static String USER_ADD_EXISTS_USERNAME= "用户名已存在";
    public static String USER_NOT_USER = "用户不存在";

    /**
     * 租户
     */
    public static String TENANT_ADD_EXISTS_TENANT_CODE = "租户编码已存在:{0}";
    public static String TENANT_ADMIN_EXISTS_USERNAME = "管理员用户名已存在:{0}";
    public static String TENANT_NOT_DELETE_DEFAULT = "不能删除默认租户";

    /**
     * 角色
     */
    public static String ROLE_NOT_DELETE_DEFAULT = "不能删除超级管理员角色";


    /**
     * 产品类型
     */
    public static String PRODUCT_TYPE_ADD_EXISTS_CODE = "类型编码已存在";

    /**
     * 服务
     */
    public static String SERVICE_ADD_NOT_PRODUCT = "请选择关联产品！";
    public static String SERVICE_ADD_NAME_EXIST = "服务名称已存在！";
    public static String SERVICE_PRODUCT_SAVE_EXIST = "产品：{0}, 已关联服务！";

    /**
     * 服务流程
     */
    public static String SERVICE_PROCESS_ADD_NAME_EXIST = "服务流程名称已存在！";
    public static String SERVICE_PROCESS_ADD_CODE_EXIST = "服务流程编码已存在！";

    /**
     * 任务
     */
    public static String COMMON_TASK_NOT_SERVICE = "请选择服务类型！";
    public static String COMMON_TASK_NOT_START_OWNER = "请选择任务总负责人！";
    public static String COMMON_TASK_NOT_SOURCE_NUMBER_OR_ID = "请选择任务任务来源单据号和任务来源单据ID！";
    public static String COMMON_TASK_EXISTS = "任务已存在，无需重新创建！";
    public static String COMMON_TASK_NOT_DATA = "未获取到要进行操作任务的数据，请重新查询后在进行操作！";
    public static String COMMON_TASK_NOT_PROVINCE_USER = "省份财务人员为配置，请联系管理员！";
    public static String COMMON_TASK_DISTRIBUTE_OWNER_USER_NOT_EXISTS_STATUS = "当前任务状态无需分配负责人，请重新查询后在进行操作！";
    public static String COMMON_TASK_CHANGE_OWNER_USER_NOT_EXISTS_STATUS = "当前任务无需移交，请重新查询后在进行操作！";
    public static String COMMON_TASK_RESTORE_NOT_EXISTS_STATUS = "当前任务无需恢复，请重新查询后在进行操作！";
    public static String COMMON_TASK_STATUS_NOT_PENDING = "任务状态非待处理，请重新查询后在进行操作！";
    public static String COMMON_TASK_STATUS_NOT_PROCESSING = "任务状态非处理中，请重新查询后在进行操作！";
    public static String COMMON_TASK_STATUS_NOT_PROCESSING_OR_PUTUP = "任务状态非处理中或挂起，请重新查询后在进行操作！";
    public static String COMMON_TASK_DETAIL_EXECUTE_FAIL = "任务流程{0}执行失败，请核对数据！";
    public static String COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER = "您不是此任务负责人/总负责人，无法进行操作！";
    public static String COMMON_TASK_EXECUTE_NOT_EXISTS_GENERAL_OWNER = "您不是此任务总负责人，无法进行操作！";
    public static String COMMON_TASK_DETAIL_NOT_DATA = "未获取到要进行操作的任务流程数据，请重新查询后在进行操作！";
    public static String COMMON_TASK_RESTORE_CANCEL_NOT_EXISTS_OWN = "已取消任务只有总负责人才能恢复！";
    public static String COMMON_TASK_DETAIL_PUTUP_NOT_DATA = "未查询到可移交流程数据，无法移交";
    public static String COMMON_TASK_DETAIL_NOT_FILE = "此流程需上传文件！";

    /**
     * 文件上传
     */
    public static String FILE_UPLOAD_NOT_SAVE_PATH = "文件上传失败, 文件保存路径不存在!";
    public static String FILE_UPLOAD_FAIL = "文件上传失败, 请联系技术人员!";

    /**
     * 百度
     */
    public static String BAIDU_ACCESS_TOKEN_ERROR = "获取百度失败：{0}";

    /**
     * 文件
     */
    public static String FILE_READ_ERROR = "文件{0}读取失败：{1}!";
    public static String FILE_READ_NOT_EXISTS = "文件{0}不存在!";
    public static String FILE_READ_TOO_LARGE = "文件{0}过大! 超出{1}限制！";
}