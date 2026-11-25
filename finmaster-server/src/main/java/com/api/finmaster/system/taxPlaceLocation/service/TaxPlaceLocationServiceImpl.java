package com.qykj.finmaster.system.taxPlaceLocation.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.system.taxPlaceLocation.entity.req.GetTaxPlaceLocationPageReq;
import com.qykj.finmaster.system.taxPlaceLocation.entity.resp.GetTaxPlaceLocationPageResp;
import com.qykj.finmaster.system.taxPlaceLocation.mapper.TaxPlaceLocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 产品类型Service业务层处理
 */
@Service
public class TaxPlaceLocationServiceImpl{

    @Autowired
    private TaxPlaceLocationMapper taxPlaceLocationMapper;

    public List<GetTaxPlaceLocationPageResp> getTaxPlaceLocationPage(GetTaxPlaceLocationPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetTaxPlaceLocationPageResp> respList = taxPlaceLocationMapper.getTaxPlaceLocationPage(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        return respList;
    }
}
