package com.qykj.finmaster.customer.company.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.FinancialConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.WebUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload;
import com.qykj.finmaster.customer.company.manager.impl.CompanyBankStatementUploadManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * 银行流水文件服务 - 代理调用财务系统接口
 */
@Slf4j
@Service
public class BankStatementService {

    @Autowired
    private CompanyBankStatementUploadManagerImpl bankStatementUploadManager;

    /**
     * 根据统一社会信用代码获取账套ID
     */
    public JSONObject getAccountSetsId(String creditCode) {
        return FinancialUtils.getAccountSets(creditCode);
    }

    /**
     * 获取银行库列表（CRM 代理财务接口 /OutSystem2CRM/getBankLibList）。
     * 前端「上传银行流水」弹窗的「银行编码」下拉走这个接口，避免前端直连财务系统。
     */
    public JSONArray getBankLibList() {
        try {
            JSONArray data = FinancialUtils.getBankLibList();
            return data != null ? data : new JSONArray();
        } catch (Exception e) {
            log.error("获取银行库列表失败", e);
            return new JSONArray();
        }
    }

    /**
     * 批量上传银行流水（CRM 适配层）。财务接口仍是单文件，本方法循环调用。
     * 支持多文件 + 多银行场景：每条 item 自带 businessName / businessCode，
     * 互不影响；任一上传失败不会中断其它文件，最终返回汇总结果。
     *
     * 返回结构：{ success: 全部成功?, total: N, successCount: M, failCount: N-M, items: [{fileName, success, msg}] }
     */
    public JSONObject batchUploadBankStatement(List<MultipartFile> files,
                                               Long accountSetsId,
                                               String period,
                                               List<String> businessNames,
                                               List<String> businessCodes,
                                               Long companyId) {
        JSONObject summary = new JSONObject();
        JSONArray items = new JSONArray();
        int successCount = 0;
        int failCount = 0;
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String businessName = (businessNames != null && i < businessNames.size()) ? businessNames.get(i) : null;
            String businessCode = (businessCodes != null && i < businessCodes.size()) ? businessCodes.get(i) : null;
            JSONObject item = new JSONObject();
            item.put("fileName", file.getOriginalFilename());
            try {
                JSONObject resp = uploadBankStatement(file, accountSetsId, period,
                        file.getOriginalFilename(), businessCode, businessName);
                boolean ok = resp != null && resp.getBooleanValue("success");
                item.put("success", ok);
                if (ok) {
                    successCount++;
                    incrementUploadCount(companyId, period);
                } else {
                    failCount++;
                    item.put("msg", resp != null ? resp.getString("msg") : "上传失败");
                }
            } catch (Exception e) {
                log.error("批量上传银行流水：单文件失败 {}", file.getOriginalFilename(), e);
                failCount++;
                item.put("success", false);
                item.put("msg", e.getMessage());
            }
            items.add(item);
        }
        summary.put("success", failCount == 0);
        summary.put("total", files.size());
        summary.put("successCount", successCount);
        summary.put("failCount", failCount);
        summary.put("items", items);
        return summary;
    }

    /**
     * 上传银行流水文件
     */
    public JSONObject uploadBankStatement(MultipartFile file, Long accountSetsId, String period,
                                          String fileName, String businessCode, String businessName) {
        String url = getFinancialUrl(FinancialConstants.BANK_STATEMENT_UPLOAD);
        try {
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            String contentType = "multipart/form-data; boundary=" + boundary;

            HttpURLConnection conn = createConnection(url, contentType);
            OutputStream out = conn.getOutputStream();

            // 写入表单字段
            writeFormField(out, boundary, "accountSetsId", String.valueOf(accountSetsId));
            writeFormField(out, boundary, "period", period);
            writeFormField(out, boundary, "type", "bankStatement");
            writeFormField(out, boundary, "fileName", fileName);
            if (businessCode != null && !businessCode.isEmpty()) {
                writeFormField(out, boundary, "businessCode", businessCode);
            }
            writeFormField(out, boundary, "businessName", businessName);

            // 写入文件字段
            writeFilePart(out, boundary, "file", file.getInputStream(),
                    file.getOriginalFilename(), file.getSize());

            // 写入结束边界
            out.write(("--" + boundary + "--\r\n").getBytes());
            out.flush();
            out.close();

            String resp = WebUtils.getStreamAsString(conn.getInputStream(), "UTF-8");
            conn.disconnect();
            return JSON.parseObject(resp);
        } catch (Exception e) {
            log.error("上传银行流水文件失败", e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("code", -1);
            error.put("msg", "上传银行流水文件失败: " + e.getMessage());
            return error;
        }
    }

    /**
     * 查询银行流水文件列表
     */
    public JSONObject listBankStatements(Long accountSetsId, String period, int page, int pageSize) {
        String url = getFinancialUrl(FinancialConstants.BANK_STATEMENT_LIST);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("accountSetsId", accountSetsId);
            params.put("period", period);
            params.put("type", "bankStatement");
            Map<String, Object> pageParam = new HashMap<>();
            pageParam.put("page", page);
            pageParam.put("pageSize", pageSize);
            params.put("page", pageParam);
            String resultJson = WebUtils.doPostReString(url, params, 10000, 10000);
            return JSON.parseObject(resultJson);
        } catch (Exception e) {
            log.error("查询银行流水文件列表失败", e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("code", -1);
            error.put("msg", "查询银行流水文件列表失败: " + e.getMessage());
            return error;
        }
    }

    /**
     * 删除银行流水文件
     */
    public JSONObject deleteBankStatement(Long accountSetsId, Long id) {
        String url = getFinancialUrl(FinancialConstants.BANK_STATEMENT_DELETE);
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("accountSetsId", accountSetsId);
            params.put("id", id);
            String resultJson = WebUtils.doPostReString(url, params, 5000, 5000);
            return JSON.parseObject(resultJson);
        } catch (Exception e) {
            log.error("删除银行流水文件失败", e);
            JSONObject error = new JSONObject();
            error.put("success", false);
            error.put("code", -1);
            error.put("msg", "删除银行流水文件失败: " + e.getMessage());
            return error;
        }
    }

    /**
     * 上传成功后增加本地计数
     */
    public void incrementUploadCount(Long companyId, String period) {
        CompanyBankStatementUpload record = bankStatementUploadManager.lambdaQuery()
                .eq(CompanyBankStatementUpload::getCompanyId, companyId)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .one();
        if (record != null) {
            record.setUploadCount(record.getUploadCount() + 1);
            record.setUpdateTime(new Date());
            bankStatementUploadManager.updateById(record);
        } else {
            record = new CompanyBankStatementUpload();
            record.setCompanyId(companyId);
            record.setPeriod(period);
            record.setUploadCount(1);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            bankStatementUploadManager.save(record);
        }
    }

    /**
     * 删除成功后减少本地计数
     */
    public void decrementUploadCount(Long companyId, String period) {
        CompanyBankStatementUpload record = bankStatementUploadManager.lambdaQuery()
                .eq(CompanyBankStatementUpload::getCompanyId, companyId)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .one();
        if (record != null && record.getUploadCount() > 0) {
            record.setUploadCount(record.getUploadCount() - 1);
            record.setUpdateTime(new Date());
            bankStatementUploadManager.updateById(record);
        }
    }

    /**
     * 统计指定公司集合中已上传银行流水的公司数（当前账期）
     */
    public long countUploadedCompanies(Set<Long> companyIds, String period) {
        if (companyIds == null || companyIds.isEmpty()) {
            return 0;
        }
        return bankStatementUploadManager.lambdaQuery()
                .in(CompanyBankStatementUpload::getCompanyId, companyIds)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .gt(CompanyBankStatementUpload::getUploadCount, 0)
                .count();
    }

    /**
     * 批量查询公司的上传记录（用于列表展示）
     */
    public Map<Long, Integer> getUploadCountMap(Set<Long> companyIds, String period) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CompanyBankStatementUpload> list = bankStatementUploadManager.lambdaQuery()
                .in(CompanyBankStatementUpload::getCompanyId, companyIds)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .gt(CompanyBankStatementUpload::getUploadCount, 0)
                .list();
        Map<Long, Integer> map = new HashMap<>();
        for (CompanyBankStatementUpload item : list) {
            map.put(item.getCompanyId(), item.getUploadCount());
        }
        return map;
    }

    /**
     * 数据收集：批量获取公司在指定账期的上传 / 无流水 状态。
     * 返回 companyId → CompanyBankStatementUpload 实体（含 uploadCount 和 noStatement）。
     */
    public Map<Long, CompanyBankStatementUpload> getStateMap(Set<Long> companyIds, String period) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CompanyBankStatementUpload> list = bankStatementUploadManager.lambdaQuery()
                .in(CompanyBankStatementUpload::getCompanyId, companyIds)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .list();
        Map<Long, CompanyBankStatementUpload> map = new HashMap<>();
        for (CompanyBankStatementUpload item : list) {
            map.put(item.getCompanyId(), item);
        }
        return map;
    }

    /**
     * 数据收集：勾选/取消勾选"本期无银行流水"。
     * 勾选后视为本期已完成（不要求实际上传文件）。
     */
    public void markNoStatement(Long companyId, String period, boolean noStatement) {
        if (companyId == null || period == null) return;
        CompanyBankStatementUpload record = bankStatementUploadManager.lambdaQuery()
                .eq(CompanyBankStatementUpload::getCompanyId, companyId)
                .eq(CompanyBankStatementUpload::getPeriod, period)
                .one();
        if (record == null) {
            record = new CompanyBankStatementUpload();
            record.setCompanyId(companyId);
            record.setPeriod(period);
            record.setUploadCount(0);
            record.setNoStatement(noStatement ? 1 : 0);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            bankStatementUploadManager.save(record);
        } else {
            record.setNoStatement(noStatement ? 1 : 0);
            record.setUpdateTime(new Date());
            bankStatementUploadManager.updateById(record);
        }
    }

    private String getFinancialUrl(String path) {
        return GlobalConstant.Domain.HTTP + FinancialConstants.FINANCE_HOST +
                GlobalConstant.Symbol.MH + FinancialConstants.FINANCE_PORT + path;
    }

    private HttpURLConnection createConnection(String urlStr, String contentType) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", contentType);
        return conn;
    }

    private void writeFormField(OutputStream out, String boundary,
                                String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n").getBytes());
        out.write(("\r\n").getBytes());
        out.write(value.getBytes("UTF-8"));
        out.write(("\r\n").getBytes());
    }

    private void writeFilePart(OutputStream out, String boundary, String fieldName,
                               InputStream inputStream, String fileName, long fileLength) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
        out.write(("Content-Type: application/octet-stream\r\n").getBytes());
        out.write(("\r\n").getBytes());
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        out.write(("\r\n").getBytes());
    }
}
