package com.qykj.finmaster.third.qcc.service;

import com.qykj.finmaster.common.utils.third.req.QccSearchReq;
import com.qykj.finmaster.common.utils.third.resp.QccSearchResp;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.QccUtil;
import com.qykj.finmaster.third.qcc.req.GetQccCompanyInfoReq;
import com.qykj.finmaster.third.qcc.resp.GetQccCompanyInfoResp;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class QccServiceImpl {

    public List<GetQccCompanyInfoResp> getQccCompanyInfo(GetQccCompanyInfoReq req) {
        QccSearchReq qccSearchReq = CopierCopyUtils.convertTo(req, QccSearchReq.class);
        List<QccSearchResp> companyInfoList = QccUtil.getCompanyInfo(qccSearchReq);
        return companyInfoList.stream()
                .map(item -> {
                    GetQccCompanyInfoResp resp = CopierCopyUtils.convertTo(item, GetQccCompanyInfoResp.class);
                    resp.setStartDate(item.getStartDate().toString());
                    return resp;
                })
                .collect(Collectors.toList());

    }
}