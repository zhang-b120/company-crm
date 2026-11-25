package com.qykj.finmaster.system.dict.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.system.dict.entity.req.AddDictTypeReq;
import com.qykj.finmaster.system.dict.entity.req.UpdateDictTypeReq;
import com.qykj.finmaster.system.dict.entity.req.GetDictTypePageReq;
import com.qykj.finmaster.system.dict.entity.req.AddDictDataReq;
import com.qykj.finmaster.system.dict.entity.req.UpdateDictDataReq;
import com.qykj.finmaster.system.dict.service.SysDictTypeServiceImpl;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 字典管理Controller
 */
@RestController
@RequestMapping("/system/dict")
public class SysDictController extends BaseController {

    @Autowired
    private SysDictTypeServiceImpl dictTypeService;

    @Autowired
    private SysDictDataServiceImpl dictDataService;

    /**
     * 查询字典类型列表（分页）
     */
    @GetMapping("/type/list")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    public TableDataInfo listDictType(GetDictTypePageReq req) {
        Long tenantId = getTenantId();
        return getDataTable(dictTypeService.selectDictTypeList(req, tenantId));
    }

    /**
     * 获取字典类型详细信息
     */
    @GetMapping("/type/{id}")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    public AjaxResult getDictType(@PathVariable Long id) {
        return AjaxResult.success(dictTypeService.getById(id));
    }

    /**
     * 新增字典类型
     */
    @PostMapping("/type")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    public AjaxResult addDictType(@RequestBody AddDictTypeReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(dictTypeService.addDictType(req, tenantId, getLoginUser()));
    }

    /**
     * 修改字典类型
     */
    @PutMapping("/type")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    public AjaxResult updateDictType(@RequestBody UpdateDictTypeReq req) {
        return AjaxResult.success(dictTypeService.updateDictType(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除字典类型
     */
    @DeleteMapping("/type/{id}")
    @PreAuthorize("@ss.hasPermi('system:dict:delete')")
    public AjaxResult deleteDictType(@PathVariable Long id) {
        return AjaxResult.success(dictTypeService.removeDictTypeById(id));
    }

    /**
     * 查询字典数据列表
     */
    @GetMapping("/data/list/{typeId}")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    public AjaxResult listDictData(@PathVariable Long typeId) {
        return AjaxResult.success(dictDataService.listByTypeId(typeId));
    }

    /**
     * 获取字典数据详细信息
     */
    @GetMapping("/data/{id}")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    public AjaxResult getDictData(@PathVariable Long id) {
        return AjaxResult.success(dictDataService.getById(id));
    }

    /**
     * 新增字典数据
     */
    @PostMapping("/data")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    public AjaxResult addDictData(@RequestBody AddDictDataReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(dictDataService.addDictData(req, tenantId, getLoginUser()));
    }

    /**
     * 修改字典数据
     */
    @PutMapping("/data")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    public AjaxResult updateDictData(@RequestBody UpdateDictDataReq req) {
        return AjaxResult.success(dictDataService.updateDictData(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除字典数据
     */
    @DeleteMapping("/data/{id}")
    @PreAuthorize("@ss.hasPermi('system:dict:delete')")
    public AjaxResult deleteDictData(@PathVariable Long id) {
        return AjaxResult.success(dictDataService.removeDictDataById(id));
    }

    /**
     * 通过字典编码获取字典数据列表
     */
    @GetMapping("/listDictDataByCode/{code}")
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    public AjaxResult listDictDataByCode(@PathVariable String code) {
        return AjaxResult.success(dictTypeService.getDictMap(code));
    }

    /**
     * 通过字典编码获取字典数据列表
     */
    @GetMapping("/getDictDataListByCode")
    public AjaxResult getDictDataListByCode(String code) {
        return AjaxResult.success(dictTypeService.getDictMap(code));
    }
}
