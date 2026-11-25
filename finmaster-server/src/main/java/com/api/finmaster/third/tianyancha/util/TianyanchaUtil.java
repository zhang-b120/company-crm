package com.qykj.finmaster.third.tianyancha.util;

import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.third.tianyancha.constants.TianyanchaConstants;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycBaseInfoResp;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycHolderItem;
import com.qykj.finmaster.third.tianyancha.domain.resp.TycSearchItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * 天眼查开放平台 HTTP 调用工具
 */
@Slf4j
public class TianyanchaUtil {

    /**
     * 企业基础信息查询 /ic/baseinfo/normal
     * @param keyword 公司名称 / 公司id / 注册号 / 统一社会信用代码
     * @return 解析后的 baseinfo 对象；失败返回 null
     */
    public static TycBaseInfoResp getBaseInfo(String keyword) {
        if (StringUtils.isBlank(keyword)) return null;
        try {
            String url = TianyanchaConstants.BASEINFO_URL + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            log.info("[天眼查-基本信息] keyword={}", keyword);

            String body = HttpRequest.get(url)
                    .header("Authorization", TianyanchaConstants.TOKEN)
                    .timeout(TianyanchaConstants.TIME_OUT)
                    .execute()
                    .body();
            log.info("[天眼查-基本信息] response={}", body);

            JSONObject root = JSON.parseObject(body);
            if (root == null) return null;
            Integer errorCode = root.getInteger("error_code");
            if (errorCode == null || errorCode != 0) {
                log.error("[天眼查-基本信息] error_code={}, reason={}", errorCode, root.getString("reason"));
                return null;
            }
            JSONObject result = root.getJSONObject("result");
            if (result == null) return null;
            TycBaseInfoResp resp = JSON.parseObject(result.toJSONString(), TycBaseInfoResp.class);
            // 原始 JSON 回写到一个 transient 字段以便 Service 落库（这里通过在 Service 侧重新取 result.toJSONString() 实现）
            return resp;
        } catch (Exception e) {
            log.error("[天眼查-基本信息] 调用失败，keyword={}", keyword, e);
            return null;
        }
    }

    /**
     * 企业基础信息 + 原始 JSON 串（result 节点的 JSON），便于落库保存
     */
    public static BaseInfoWithRaw getBaseInfoWithRaw(String keyword) {
        if (StringUtils.isBlank(keyword)) return null;
        try {
            String url = TianyanchaConstants.BASEINFO_URL + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String body = HttpRequest.get(url)
                    .header("Authorization", TianyanchaConstants.TOKEN)
                    .timeout(TianyanchaConstants.TIME_OUT)
                    .execute()
                    .body();
            JSONObject root = JSON.parseObject(body);
            if (root == null) return null;
            Integer errorCode = root.getInteger("error_code");
            if (errorCode == null || errorCode != 0) {
                log.error("[天眼查-基本信息] error_code={}, reason={}", errorCode, root.getString("reason"));
                return null;
            }
            JSONObject result = root.getJSONObject("result");
            if (result == null) return null;
            BaseInfoWithRaw wrapper = new BaseInfoWithRaw();
            wrapper.baseInfo = JSON.parseObject(result.toJSONString(), TycBaseInfoResp.class);
            wrapper.rawJson = result.toJSONString();
            return wrapper;
        } catch (Exception e) {
            log.error("[天眼查-基本信息] 调用失败，keyword={}", keyword, e);
            return null;
        }
    }

    /**
     * 股东信息 /ic/holder/2.0
     */
    public static List<TycHolderItem> getHolders(String keyword) {
        if (StringUtils.isBlank(keyword)) return Collections.emptyList();
        try {
            String url = TianyanchaConstants.HOLDER_URL
                    + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&pageNum=1&pageSize=20";
            log.info("[天眼查-股东] keyword={}", keyword);

            String body = HttpRequest.get(url)
                    .header("Authorization", TianyanchaConstants.TOKEN)
                    .timeout(TianyanchaConstants.TIME_OUT)
                    .execute()
                    .body();
            log.info("[天眼查-股东] response={}", body);

            JSONObject root = JSON.parseObject(body);
            if (root == null) return Collections.emptyList();
            Integer errorCode = root.getInteger("error_code");
            if (errorCode == null || errorCode != 0) {
                log.error("[天眼查-股东] error_code={}, reason={}", errorCode, root.getString("reason"));
                return Collections.emptyList();
            }
            JSONObject result = root.getJSONObject("result");
            if (result == null) return Collections.emptyList();
            JSONArray items = result.getJSONArray("items");
            if (items == null || items.isEmpty()) return Collections.emptyList();
            return JSON.parseArray(items.toJSONString(), TycHolderItem.class);
        } catch (Exception e) {
            log.error("[天眼查-股东] 调用失败，keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }

    public static class BaseInfoWithRaw {
        public TycBaseInfoResp baseInfo;
        public String rawJson;
    }

    /**
     * 一般纳税人资格查询 /m/taxpayer/2.0
     * 业务规则：result.items 非空 → 一般纳税人；空 / 接口错误 → 视为小规模。
     *
     * @param keyword 公司名 / 公司 id / 注册号 / 统一社会信用代码
     * @return TaxpayerLookup（含 generalTaxpayer / items 原始数组，由调用方决定如何呈现）
     */
    public static TaxpayerLookup getTaxpayer(String keyword) {
        TaxpayerLookup lookup = new TaxpayerLookup();
        lookup.generalTaxpayer = false;
        lookup.items = new JSONArray();
        if (StringUtils.isBlank(keyword)) return lookup;
        try {
            String url = TianyanchaConstants.TAXPAYER_URL
                    + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&pageNum=1&pageSize=20";
            log.info("[天眼查-一般纳税人] keyword={}", keyword);
            String body = HttpRequest.get(url)
                    .header("Authorization", TianyanchaConstants.TOKEN)
                    .timeout(TianyanchaConstants.TIME_OUT)
                    .execute()
                    .body();
            log.info("[天眼查-一般纳税人] response={}", body);

            JSONObject root = JSON.parseObject(body);
            if (root == null) return lookup;
            Integer errorCode = root.getInteger("error_code");
            if (errorCode == null || errorCode != 0) {
                log.error("[天眼查-一般纳税人] error_code={}, reason={}", errorCode, root.getString("reason"));
                return lookup;
            }
            JSONObject result = root.getJSONObject("result");
            if (result == null) return lookup;
            JSONArray items = result.getJSONArray("items");
            if (items != null && !items.isEmpty()) {
                lookup.generalTaxpayer = true;
                lookup.items = items;
            }
            return lookup;
        } catch (Exception e) {
            log.error("[天眼查-一般纳税人] 调用失败，keyword={}", keyword, e);
            return lookup;
        }
    }

    /** 一般纳税人资格查询返回 */
    public static class TaxpayerLookup {
        public boolean generalTaxpayer;
        public JSONArray items;
    }

    /**
     * 企业模糊搜索 /services/open/search/2.0
     * @param keyword  关键字（公司名/简称/法人名/地址片段 等）
     * @param pageNum  页码（1 起）
     * @param pageSize 每页条数
     * @return 候选列表；失败返回空列表
     */
    public static List<TycSearchItem> searchByKeyword(String keyword, int pageNum, int pageSize) {
        if (StringUtils.isBlank(keyword)) return Collections.emptyList();
        try {
            String url = TianyanchaConstants.SEARCH_URL
                    + "?word=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&pageNum=" + pageNum
                    + "&pageSize=" + pageSize;
            log.info("[天眼查-模糊搜索] keyword={}, pageNum={}, pageSize={}", keyword, pageNum, pageSize);

            String body = HttpRequest.get(url)
                    .header("Authorization", TianyanchaConstants.TOKEN)
                    .timeout(TianyanchaConstants.TIME_OUT)
                    .execute()
                    .body();
            log.info("[天眼查-模糊搜索] response={}", body);

            JSONObject root = JSON.parseObject(body);
            if (root == null) return Collections.emptyList();
            Integer errorCode = root.getInteger("error_code");
            if (errorCode == null || errorCode != 0) {
                log.error("[天眼查-模糊搜索] error_code={}, reason={}", errorCode, root.getString("reason"));
                return Collections.emptyList();
            }
            JSONObject result = root.getJSONObject("result");
            if (result == null) return Collections.emptyList();
            JSONArray items = result.getJSONArray("items");
            if (items == null || items.isEmpty()) return Collections.emptyList();
            return JSON.parseArray(items.toJSONString(), TycSearchItem.class);
        } catch (Exception e) {
            log.error("[天眼查-模糊搜索] 调用失败，keyword={}", keyword, e);
            return Collections.emptyList();
        }
    }
}
