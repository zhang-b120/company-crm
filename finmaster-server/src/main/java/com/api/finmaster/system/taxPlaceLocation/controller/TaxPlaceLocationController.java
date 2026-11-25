package com.qykj.finmaster.system.taxPlaceLocation.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.system.taxPlaceLocation.entity.req.GetTaxPlaceLocationPageReq;
import com.qykj.finmaster.system.taxPlaceLocation.service.TaxPlaceLocationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品类型Controller
 */
@RestController
@RequestMapping("/system/taxPlaceLocation")
public class TaxPlaceLocationController extends BaseController {
    @Autowired
    private TaxPlaceLocationServiceImpl taxPlaceLocationService;

    @PostMapping("/list")
    public TableDataInfo getTaxPlaceLocationPage(@RequestBody GetTaxPlaceLocationPageReq req){
        return getDataTable(taxPlaceLocationService.getTaxPlaceLocationPage(req));
    }
}
