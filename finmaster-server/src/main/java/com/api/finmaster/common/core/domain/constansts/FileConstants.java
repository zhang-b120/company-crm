package com.qykj.finmaster.common.core.domain.constansts;

/**
 * @Author zhb
 * @Date 2025-08-20
 * @Description
 */
public class FileConstants {

    /**
     * 公共域名
     */
    public static String BASE_URL = "https://www.qingyicloud.com/";

    /**
     * 文件前缀
     */
    public static String FILE_PRE_ORD_ACCOUNT = "FOA"; //旧账
    public static String FILE_PRE_COMPANY_RECORD = "FCR"; //公司维护记录

    /**
     * 文件路径
     */
    public static String FILE_URL_ORD_ACCOUNT = "E:\\qykj\\file\\oldAccount"; //旧账 //E:\qykj\file\oldAccount
    public static String FILE_URL_COMPANY_RECORD = "E:\\qykj\\file\\companyServiceRecord"; //公司维护记录 //E:\qykj\file\companyServiceRecord
    public static String FILE_URL_ORDER_REFUND = "E:\\qykj\\file\\orderRefund"; //订单退款 //E:\qykj\file\orderRefund
    public static String FILE_URL_CONTRACT = "E:\\qykj\\file\\contract";


    /**
     * 文件类型
     */
    public static String FILE_TYPE_ORD_ACCOUNT = "oldAccount"; //旧账
    public static String FILE_TYPE_COMPANY_RECORD = "companyRecord"; //公司维护记录
    public static String FILE_TYPE_ORDER_REFUND = "orderRefund"; //订单退款
    public static String FILE_TYPE_CONTRACT = "contract"; //合同


    /**
     * 文件获取路径
     */
    public static String FILE_GET_ORD_ACCOUNT = BASE_URL + "common/file/oldAccount/"; //旧账 //BASE_URL + "common/file/oldAccount/"
    public static String FILE_GET_COMPANY_RECORD = BASE_URL + "common/file/companyServiceRecord/"; //公司维护记录

    /**
     * 图片获取路径 common/file/imgs/
     */
    public static String FILE_GET_IMG = BASE_URL + "common/file/imgs/";
    public static String FILE_GET_ORDER_REFUND = BASE_URL + "common/file/orderRefund/";//订单退款 BASE_URL + "common/file/orderRefund/"
    public static String FILE_GET_CONTRACT = BASE_URL + "common/file/contract/"; // 合同文件访问前缀

    /**
     * 合同模板文件名（位于 FILE_URL_CONTRACT 目录下）
     */
    public static String CONTRACT_TEMPLATE_FILENAME = "contract_file_template.docx";

    /**
     * 通话录音
     */
    public static String FILE_URL_CALL_RECORDING = "E:\\qykj\\file\\callRecording"; // 录音本地存储目录
    public static String FILE_GET_CALL_RECORDING = BASE_URL + "common/file/callRecording/"; // 录音访问地址前缀

    /**
     * 外勤任务图片
     */
    public static String FILE_URL_FIELD_WORK_IMAGE = "E:\\qykj\\file\\fieldWorkImage"; // 外勤图片本地存储目录
    public static String FILE_GET_FIELD_WORK_IMAGE = BASE_URL + "common/file/fieldWorkImage/"; // 外勤图片访问前缀

    /**
     * 任务单 V2 附件（旧账 / 外勤照片 / 其他）
     */
    public static String FILE_URL_WORK_ORDER = "E:\\qykj\\file\\workOrder";
    public static String FILE_GET_WORK_ORDER = BASE_URL + "common/file/workOrder/";

    /**
     * 知识库图片（富文本编辑器内插图）
     */
    public static String FILE_URL_KNOWLEDGE_IMAGE = "E:\\qykj\\file\\knowledge\\image";
    public static String FILE_GET_KNOWLEDGE_IMAGE = BASE_URL + "common/file/knowledge/image/";


    /**
     * 知识库附件（PDF/Word/Excel/PPT 等）
     */
    public static String FILE_URL_KNOWLEDGE_ATTACHMENT = "E:\\qykj\\file\\knowledge\\attachment";
    public static String FILE_GET_KNOWLEDGE_ATTACHMENT = BASE_URL + "common/file/knowledge/attachment/";
}