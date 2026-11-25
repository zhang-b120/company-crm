package com.qykj.finmaster.common.utils.third;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.utils.third.req.QccSearchReq;
import com.qykj.finmaster.common.utils.third.resp.QccSearchResp;
import com.qykj.finmaster.common.core.domain.constansts.QccConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Slf4j
public class QccUtil {

    private static final int TIME_OUT = 30000;

    /**
     * 模糊查询企业工商信息
     * @param req
     * @return
     */
    public static List<QccSearchResp> getCompanyInfo(QccSearchReq req){
        AtomicReference<List<QccSearchResp>> respList = new AtomicReference<>();
        if(StringUtils.isEmpty(req.getSearchKey())){
            return respList.get();
        }
        String status;
        String url = QccConstants.FUZZY_SEARCH_URL.concat("?key=").concat(QccConstants.APP_KEY)
                .concat("&searchKey=").concat(req.getSearchKey());
        if(StringUtils.isNotEmpty(req.getProvinceCode())){
            url = url.concat("&provinceCode=").concat(String.valueOf(req.getProvinceCode()));
        }
        if(StringUtils.isNotEmpty(req.getCityCode())){
            url = url.concat("&cityCode=").concat(String.valueOf(req.getCityCode()));
        }
        if(StringUtils.isNotEmpty(req.getPageIndex())){
            url = url.concat("&pageIndex=").concat(String.valueOf(req.getPageIndex()));
        }
        String body = getResultFromThirdParty(url);
        JSONObject jsonObject = JSON.parseObject(body);
        if (jsonObject != null) {
            status = String.valueOf(jsonObject.get("Status"));
            if (Objects.equals(status, String.valueOf(HttpStatus.OK.value()))) {
                Object result = jsonObject.get("Result");
                Optional.ofNullable(result).ifPresent(itm -> {
                    JSONArray array = (JSONArray) itm;
                    respList.set(JSON.parseArray(array.toJSONString(), QccSearchResp.class));
                });
            } else {
                log.error(body);
            }
        }
        return respList.get();
    }

    /**
     * 调用企查查接口获取工商信息
     * @return 查询结果
     */
    private static String getResultFromThirdParty(String url) {
        String[] header = randomAuthHeader();
        HttpRequest httpRequest = HttpRequest.get(url)
                .header("Token", header[0])
                .header("Timespan", header[1])
                .timeout(TIME_OUT);
        HttpResponse response = httpRequest.execute();
        return response.body();
    }

    /**
     * 组装企查查请求头
     * @return
     */
    protected  static String[] randomAuthHeader() {
        String timeSpan = String.valueOf(System.currentTimeMillis() / 1000);
        return new String[]{DigestUtils.md5Hex(QccConstants.APP_KEY.concat(timeSpan).concat(QccConstants.APP_SECRET)).toUpperCase(), timeSpan};
    }
}