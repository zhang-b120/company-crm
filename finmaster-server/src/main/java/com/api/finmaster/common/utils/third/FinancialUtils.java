package com.qykj.finmaster.common.utils.third;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.FinancialConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.WebUtils;
import com.qykj.finmaster.common.utils.third.req.AddAccountSetMigrationDataReq;
import com.qykj.finmaster.common.utils.third.req.CreateAccountSetReq;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-06-04
 * @Description
 */
public class FinancialUtils {

    /**
     * 根据税号获取账套信息
     * @param creditCode
     * @return
     */
    public static JSONObject getAccountSets(String creditCode){
        Map<String,Object> map = new HashMap<>();
        map.put("creditCode", creditCode);
        String url = getUrl(FinancialConstants.GET_ACCOUNT_SETS);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONObject dataJson = JSON.parseObject(resultJson).getJSONObject("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 根据税号获取账套信息-批量
     * @param creditCodeList
     * @return
     */
    public static JSONArray getGetAccountSetsList(List<String> creditCodeList){
        Map<String, Object> map = new HashMap<>();
        String creditCodes = StringUtils.join(creditCodeList, GlobalConstant.Symbol.COMMA);
        map.put("creditCodes", creditCodes);
        String url = getUrl(FinancialConstants.GET_ACCOUNT_SETS_LIST);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONArray dataJson = JSON.parseObject(resultJson).getJSONArray("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据税号获取记账信息
     * @param creditCode
     * @return
     */
    public static JSONArray getAccountProgressByCreditCode(String creditCode){
        Map<String, Object> map = new HashMap<>();
        map.put("creditCode", creditCode);
        String url = getUrl(FinancialConstants.GET_ACCOUNT_PROGRESS);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONArray dataJson = JSON.parseObject(resultJson).getJSONArray("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建账套
     * @return
     */
    public static JSONObject createExperienceAccountSet(CreateAccountSetReq req){
        Map<String, Object> map = req.toBeanMap();
        String url = getExperienceUrl(FinancialConstants.CREATE_ACCOUNT_SET);
        try {
            String resultJson = WebUtils.doPostReString(url, map);
            JSONObject dataJson = JSON.parseObject(resultJson).getJSONObject("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 创建账套
     * @return
     */
    public static JSONObject createAccountSet(CreateAccountSetReq req){
        Map<String, Object> map = req.toBeanMap();
        String url = getUrl(FinancialConstants.CREATE_ACCOUNT_SET);
        try {
            String resultJson = WebUtils.doPostReString(url, map);
            JSONObject dataJson = JSON.parseObject(resultJson);
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取体验账套
     * @param experienceAccountSetId
     * @return
     */
    public static JSONObject getExperienceAccountSets(Long experienceAccountSetId) {
        Map<String,Object> map = new HashMap<>();
        map.put("id", experienceAccountSetId);
        String url = getExperienceUrl(FinancialConstants.GET_EXPERIENCE_ACCOUNT_SETS);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONObject dataJson = JSON.parseObject(resultJson).getJSONObject("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取体验账套迁移数据
     * @param experienceAccountSetId
     * @return
     */
    public static JSONArray getExperienceMigrationData(Long experienceAccountSetId) {
        Map<String,Object> map = new HashMap<>();
        map.put("id", experienceAccountSetId);
        String url = getExperienceUrl(FinancialConstants.GET_EXPERIENCE_MIGRATION_DATA);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONArray dataJson = JSON.parseObject(resultJson).getJSONArray("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 数据迁移至正式账套
     * @param list
     * @return
     */
    public static Boolean addAccountSetMigrationData(List<AddAccountSetMigrationDataReq> list){
        Map<String, Object> map = new HashMap<>();
        map.put("list", JSON.toJSONString(list));
        String url = getUrl(FinancialConstants.ADD_ACCOUNT_SET_MIGRATION_DATA);
        try {
            String resultJson = WebUtils.doPostReString(url, map);
            Boolean dataJson = JSON.parseObject(resultJson).getBoolean("data");
            return dataJson;
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    /**
     * 获取银行库列表（GET，无参）。
     * 财务系统返回 { success, code, msg, data: [{id, bankCode, bankName, enabled, isAutoRecognize}] }
     * 由 BankStatementService 代理给前端，避免前端直连财务系统。
     */
    public static JSONArray getBankLibList() {
        String url = getUrl(FinancialConstants.BANK_LIB_LIST);
        try {
            String resultJson = WebUtils.doGet(url, new HashMap<>());
            if (StringUtils.isBlank(resultJson)) return new JSONArray();
            JSONObject root = JSON.parseObject(resultJson);
            return root != null ? root.getJSONArray("data") : new JSONArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取正式版路由
     * @param url
     * @return
     */
    private static String getUrl(String url){
        return GlobalConstant.Domain.HTTP + FinancialConstants.FINANCE_HOST +
                GlobalConstant.Symbol.MH + FinancialConstants.FINANCE_PORT +
                url;
    }

    /**
     * 获取体验版路由
     * @param url
     * @return
     */
    private static String getExperienceUrl(String url){
        return GlobalConstant.Domain.HTTP + FinancialConstants.FINANCE_HOST +
                GlobalConstant.Symbol.MH + FinancialConstants.FINANCE_EXPERIENCE_PORT +
                url;
    }

    /**
     * 添加员工（按税号定位账套，按身份证号去重）。
     * 调用方不需要传 id / accountSetsId，财务系统按 creditCode 反查后写入。
     *
     * @param creditCode 公司税号（query 参数）
     * @param employee   员工字段 Map（name / idCard / gender / telPhone 等，详见接口文档）
     * @return 财务系统响应 JSON
     */
    public static JSONObject addEmployee(String creditCode, Map<String, Object> employee) {
        String url = getUrl(FinancialConstants.ADD_EMPLOYEE) + "?creditCode=" + creditCode;
        try {
            String resultJson = WebUtils.doPostReString(url, employee);
            return JSON.parseObject(resultJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 批量添加员工（每条独立事务，按身份证号去重，单条失败不影响其它条）。
     * Body 是 [{ employee: {...}, positionSalary: 5000 }, ...] 形式。
     *
     * @param creditCode 公司税号（query 参数）
     * @param items      逐条员工 + 工资 payload 列表
     * @return 财务系统响应 JSON（含 total / created / skipped / failed / items）
     */
    public static JSONObject addEmployees(String creditCode, List<Map<String, Object>> items) {
        String url = getUrl(FinancialConstants.ADD_EMPLOYEES) + "?creditCode=" + creditCode;
        try {
            String body = JSON.toJSONString(items);
            String resultJson = WebUtils.doPostJsonReString(url, body);
            return JSON.parseObject(resultJson);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 企业微信群聊采集入库（工单91 / WXKW-06）：把"群名 + 客户消息"推送财务系统 collectWxGroupChat。
     * 与 {@link #addEmployees} 同款 JSON 提交；推送非关键，异常 catch 不抛、返回 null（不中断采集循环）。
     *
     * @return 财务系统响应 JSON（含 success/code/msg/data），调用方按 success 判错；异常时 null
     */
    public static JSONObject collectWxGroupChat(String chatName, String content) {
        Map<String, Object> body = new HashMap<>();
        body.put("chatName", chatName);
        body.put("content", content);
        String url = getUrl(FinancialConstants.COLLECT_WX_GROUP_CHAT);
        try {
            String resultJson = WebUtils.doPostJsonReString(url, JSON.toJSONString(body));
            return JSON.parseObject(resultJson);
        } catch (Exception e) {
            return null; // 推送非关键，不抛断采集循环
        }
    }

    /**
     * 按税号更新账套字段（部分更新，传什么改什么）。
     * 受保护字段：id / creditCode / createDate / creatorId 即使传入也不会修改。
     *
     * @param payload 必含 creditCode，其余字段按需传
     * @return 财务系统响应 JSON
     */
    public static JSONObject updateAccountSet(Map<String, Object> payload) {
        String url = getUrl(FinancialConstants.UPDATE_ACCOUNT_SET);
        try {
            String resultJson = WebUtils.doPostReString(url, payload);
            return JSON.parseObject(resultJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 按税号停用账套（退款·取消办税场景）。
     * @param creditCode 公司税号（query 参数）
     * @return 财务系统响应 JSON
     */
    public static JSONObject deactivateAccountSet(String creditCode) {
        String url = getUrl(FinancialConstants.UPDATE_ACCOUNT_SETS_DEACTIVATED) + "?creditCode=" + creditCode;
        try {
            String resultJson = WebUtils.doPostReString(url, new HashMap<>());
            return JSON.parseObject(resultJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 按税号更新账套截止日期（续费场景 / 截止日期变更）。
     * @param creditCode 公司税号
     * @param effectiveDate 截止日期 yyyy-MM-dd
     * @return 财务系统响应 JSON
     */
    public static JSONObject updateAccountSetEffectiveDate(String creditCode, String effectiveDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("creditCode", creditCode);
        body.put("effectiveDate", effectiveDate);
        String url = getUrl(FinancialConstants.UPDATE_ACCOUNT_SET_EFFECTIVE_DATE);
        try {
            String resultJson = WebUtils.doPostReString(url, body);
            return JSON.parseObject(resultJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 修改账套个税密码
     * @return
     */
    public static Boolean updateIndividualTaxCodeByCreditCode(String creditCode ,String individualTaxCode){
        Map<String,Object> map = new HashMap<>();
        map.put("creditCode", creditCode);
        map.put("individualTaxCode", individualTaxCode);
        String url = getUrl(FinancialConstants.UPDATE_INDIVIDUAL_TAX);
        try {
            WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Boolean.TRUE;
    }

    /**
     * 导入科目
     * @param taxNumber
     * @param filePath
     * @return
     */
    public static Boolean importSubjectInit(String taxNumber,String filePath,String oldFile){
        Map<String,Object> map = new HashMap<>();
        map.put("taxNumber", taxNumber);
        map.put("filePath", filePath);
        map.put("oldFile",oldFile);
        String url = getUrl(FinancialConstants.IMPORT_FINANCIAL_INIT);
        try {
            WebUtils.doPostReString(url, map);
            return Boolean.TRUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 修改账套归属人
     * @param mobile
     * @param preMobile
     * @param taxNumber
     * @return
     */
    public static Boolean updateAccountSetsUser(String mobile, String preMobile, String taxNumber){
        Map<String,Object> map = new HashMap<>();
        map.put("mobile", mobile);
        map.put("preMobile", preMobile);
        map.put("creditCode",taxNumber);
        String url = getUrl(FinancialConstants.UPDATE_ACCOUNT_SETS_USER);
        try {
            WebUtils.doPostReString(url, map);
            return Boolean.TRUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 账套停用
     * @param creditCodeList
     * @return
     */
    public static Boolean deactivatedAccountSets(List<String> creditCodeList){
        String creditCode = StringUtils.join(creditCodeList, GlobalConstant.Symbol.COMMA);
        Map<String,Object> map = new HashMap<>();
        map.put("creditCode", creditCode);
        String url = getUrl(FinancialConstants.UPDATE_ACCOUNT_SETS_DEACTIVATED);
        try {
            WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET, 5000, 5000);
            return Boolean.TRUE;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 根据税号获取账套信息
     * @param creditCode
     * @param creditCode
     * @return
     */
    public static JSONObject getAccountSetsBeforeCreate(String creditCode, Long creatorId){
        Map<String,Object> map = new HashMap<>();
        map.put("creditCode", creditCode);
        map.put("creatorId", creatorId);
        String url = getUrl(FinancialConstants.GET_ACCOUNT_SETS_BEFORE_CREATE);
        try {
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET,5000, 5000);
            JSONObject dataJson = JSON.parseObject(resultJson).getJSONObject("data");
            return dataJson;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}