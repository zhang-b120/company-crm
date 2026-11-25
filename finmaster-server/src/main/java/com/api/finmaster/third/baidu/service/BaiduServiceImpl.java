package com.qykj.finmaster.third.baidu.service;

import com.baidu.aip.ocr.AipOcr;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.FileUtils;
import com.qykj.finmaster.common.utils.third.BaiduUtils;
import com.qykj.finmaster.common.utils.third.req.GetGeneralBasicReq;
import com.qykj.finmaster.config.BaiduApiConfig;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
@Service
public class BaiduServiceImpl {

    @Autowired
    private AipOcr aipOcr;

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    /**
     * 通用文字识别
     * @param filePath
     * @return
     */
    public String getBasicGeneral(String filePath){
        String result = null;
        try {
            GetGeneralBasicReq req = new GetGeneralBasicReq();
            String fileData = FileUtils.getFileContentAsBase64(filePath, Boolean.FALSE);
            if(filePath.endsWith(GlobalConstant.Suffix.LOWER_PDF) || filePath.endsWith(GlobalConstant.Suffix.CAP_PDF)){
                req.setPdf_file(fileData);
            }
            result = BaiduUtils.generalBasic(req, baiduApiConfig.getBaiduAccessToken());
        } catch (Exception e) {
            System.out.println(e);
        }
        return result;
    }

    /**
     * 读取身份证信息
     * @param url        图片URL
     * @param idCardSide front：身份证含照片的一面；back：身份证带国徽的一面
     * @return
     */
    public JSONObject getIdCard(String url, String idCardSide) {
        JSONObject idcard = aipOcr.idcard(FileUtils.readFileByBytes(url), idCardSide, null);
        return idcard;
    }
}