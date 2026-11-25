package com.qykj.finmaster.data.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.customer.lead.entity.dto.GetLeadDataDTO;
import com.qykj.finmaster.data.domain.dto.SaleLeadOrCustomerDataDto;
import com.qykj.finmaster.data.domain.dto.SaleOrderDataDto;
import com.qykj.finmaster.data.domain.req.GetLeadAnalysisDataReq;
import com.qykj.finmaster.data.domain.req.GetSaleLeadListReq;
import com.qykj.finmaster.data.domain.resp.*;
import com.qykj.finmaster.data.mapper.LeadAnalysisMapper;
import com.qykj.finmaster.order.domain.dto.GetOrderDetailDataDTO;
import com.qykj.finmaster.system.appletUser.domain.dto.GetAppletUserDataDTO;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeadAnalysisServiceImpl {

    @Autowired
    private LeadAnalysisMapper leadAnalysisMapper;

    @Autowired
    private UserMapper userMapper;
    /**
     * 获取线索数据
     * @return
     */
    public List<GetLeadAnalysisDataResp> list(GetLeadAnalysisDataReq req) {
        List<GetLeadAnalysisDataResp> respList = new ArrayList<>();
        if(ObjectUtils.isEmpty(req.getDepartmentId())){
            return respList;
        }
        List<User> userList = userMapper.selectUsersByDeptWithChildren(req.getDepartmentId(), GlobalConstant.Number.ONE_INT);
        if(CollUtil.isNotEmpty(userList)){
            List<String> userNameList = userList.stream().map(User::getUsername).toList();
            Map<String, Object> params = new HashMap<>();
            params.put("userNameList", userNameList);
            //获取线索数据
            List<GetLeadDataDTO> leadList= leadAnalysisMapper.getLeadData(params);
            Map<String, List<GetLeadDataDTO>> leadCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(leadList)){
                leadCollect = leadList.stream().collect(Collectors.groupingBy(
                        lead -> StringUtils.isNotEmpty(lead.getOwnerUser()) ? lead.getOwnerUser() : lead.getCreateBy(),
                        Collectors.toList()
                ));
            }
            //获取订单明细数据
            List<GetOrderDetailDataDTO> orderDetailList = leadAnalysisMapper.getOrderDetailData(params);
            Map<String, List<GetOrderDetailDataDTO>> orderDetailCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(orderDetailList)){
                orderDetailCollect = orderDetailList.stream().collect(Collectors.groupingBy(GetOrderDetailDataDTO::getOwnerUser));
            }
            //获取小程序用户
            //获取小程序用户数据
            List<GetAppletUserDataDTO> appletUserList = leadAnalysisMapper.getAppletUserData(params);
            Map<String, List<GetAppletUserDataDTO>> appletUserCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(appletUserList)){
                appletUserCollect = appletUserList.stream().collect(Collectors.groupingBy(GetAppletUserDataDTO::getOwnerUser));
            }

            Map<String, List<GetAppletUserDataDTO>> finalAppletUserCollect = appletUserCollect;
            Map<String, List<GetOrderDetailDataDTO>> finalOrderDetailCollect = orderDetailCollect;
            Map<String, List<GetLeadDataDTO>> finalLeadCollect = leadCollect;
            userList.forEach(item -> {
                GetLeadAnalysisDataResp resp = new GetLeadAnalysisDataResp();
                resp.setOwnerUser(item.getUsername());
                resp.setOwnerUserName(item.getRealName());
                if(MapUtils.isNotEmpty(finalLeadCollect) && CollUtil.isNotEmpty(finalLeadCollect.get(item.getUsername()))){
                    List<GetLeadDataDTO> list = finalLeadCollect.get(item.getUsername());
                    resp.setAllLeadCount(list.size());
                    List<GetLeadDataDTO> todayList = list.stream()
                            .filter(f -> LocalDate.now().atStartOfDay().isBefore(f.getCreateTime()))
                            .toList();
                    if(CollUtil.isNotEmpty(todayList)){
                        resp.setTodayLeadCount(todayList.size());
                    }
                }
                if(MapUtils.isNotEmpty(finalOrderDetailCollect) && CollUtil.isNotEmpty(finalOrderDetailCollect.get(item.getUsername()))){
                    List<GetOrderDetailDataDTO> list = finalOrderDetailCollect.get(item.getUsername());
                    resp.setAllDealCount(list.size());
                    List<GetOrderDetailDataDTO> monthList = list.stream()
                            .filter(f -> DateUtils.getFirstDayOfMonthWithTime().isBefore(f.getCreateTime())
                                    && DateUtils.getLastDayOfMonthWithTime().isAfter(f.getCreateTime())
                            )
                            .toList();
                    if(CollUtil.isNotEmpty(monthList)){
                        resp.setMonthDealCount(monthList.size());
                        List<GetOrderDetailDataDTO> todayList = list.stream()
                                .filter(f -> LocalDate.now().atStartOfDay().isBefore(f.getCreateTime()))
                                .toList();
                        if(CollUtil.isNotEmpty(todayList)){
                            resp.setTodayLeadCount(todayList.size());
                        }
                    }
                }
                if(MapUtils.isNotEmpty(finalAppletUserCollect) && CollUtil.isNotEmpty(finalAppletUserCollect.get(item.getUsername()))){
                    List<GetAppletUserDataDTO> list = finalAppletUserCollect.get(item.getUsername());
                    resp.setAllRegisterApplet(list.size());
                    List<GetAppletUserDataDTO> todayList = list.stream()
                            .filter(f -> LocalDate.now().atStartOfDay().isBefore(f.getCreateTime()))
                            .toList();
                    if(CollUtil.isNotEmpty(todayList)){
                        resp.setTodayRegisterApplet(todayList.size());
                    }
                }
                if(resp.getAllLeadCount() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getAllRegisterApplet()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getAllLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setAllRegisterAllLeadRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(resp.getTodayLeadCount() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getTodayDealCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getTodayLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setTodayDealTodayLeadRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(resp.getTodayRegisterApplet() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getTodayDealCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getTodayRegisterApplet()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setTodayDealTodayRegisterRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(resp.getAllRegisterApplet() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getAllDealCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getAllRegisterApplet()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setAllDealAllRegisterRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(resp.getTodayLeadCount() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getTodayRegisterApplet()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getTodayLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setTodayRegisterTodayLeadRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(resp.getAllLeadCount() != GlobalConstant.Number.ZERO_INT){
                    BigDecimal divide = new BigDecimal(resp.getAllDealCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getAllLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setAllDealAllLeadRadio(divide + GlobalConstant.Symbol.PER_CENT);
                }
                respList.add(resp);
            });
        }
        return respList;
    }

    /**
     * 导出线束数据
     * @param req
     * @param response
     * @return
     */
    public void export(GetLeadAnalysisDataReq req, HttpServletResponse response) {
        List<ExportLeadAnalysisDataResp> exportList = CopierCopyUtils.convertTo(this.list(req), ExportLeadAnalysisDataResp.class);
        ExcelUtil<ExportLeadAnalysisDataResp> util = new ExcelUtil<>(ExportLeadAnalysisDataResp.class);
        util.exportExcel(response, exportList, "线索数据");
    }

    /**
     * 获取线索数据(推广部)
     * @param req
     * @return
     */
    public List<GetPromotionLeadAnalysisDataResp> promotionList(GetLeadAnalysisDataReq req) {
        List<GetPromotionLeadAnalysisDataResp> respList = new ArrayList<>();
        if(ObjectUtils.isEmpty(req.getDepartmentId())){
            return respList;
        }
        List<User> userList = userMapper.selectUsersByDeptWithChildren(req.getDepartmentId(), GlobalConstant.Number.ONE_INT);
        if(CollUtil.isNotEmpty(userList)) {
            List<String> userNameList = userList.stream().map(User::getUsername).toList();
            Map<String, Object> params = new HashMap<>();
            params.put("userNameList", userNameList);
            if (ObjectUtils.isNotEmpty(req.getStart())) {
                params.put("start", DateUtils.localDateTimeToDate(req.getStart().atStartOfDay()));
            }
            if (ObjectUtils.isNotEmpty(req.getEnd())) {
                params.put("end", DateUtils.localDateTimeToDate(req.getEnd().atTime(23, 59, 59, 999)));
            }
            //获取线索数据
            List<GetLeadDataDTO> leadList = leadAnalysisMapper.getPromotionLeadData(params);
            Map<String, List<GetLeadDataDTO>> leadCollect = new HashMap<>();
            if (CollUtil.isNotEmpty(leadList)) {
                leadCollect = leadList.stream().collect(Collectors.groupingBy(GetLeadDataDTO::getCreateBy));
            }
            //获取订单明细数据
            Map<String, List<GetOrderDetailDataDTO>> orderDetailCollect = new HashMap<>();
            List<GetOrderDetailDataDTO> orderDetailList = leadAnalysisMapper.getPromotionOrderDetailData(params);
            if (CollUtil.isNotEmpty(orderDetailList)) {
                orderDetailCollect = orderDetailList.stream().collect(Collectors.groupingBy(GetOrderDetailDataDTO::getOwnerUser));
            }

            Map<String, List<GetLeadDataDTO>> finalLeadCollect = leadCollect;
            Map<String, List<GetOrderDetailDataDTO>> finalOrderDetailCollect = orderDetailCollect;
            userList.forEach(item -> {
                GetPromotionLeadAnalysisDataResp resp = new GetPromotionLeadAnalysisDataResp();
                resp.setOwnerUser(item.getUsername());
                resp.setOwnerUserName(item.getRealName());
                if(MapUtils.isNotEmpty(finalLeadCollect) && CollUtil.isNotEmpty(finalLeadCollect.get(item.getUsername()))){
                    List<GetLeadDataDTO> list = finalLeadCollect.get(item.getUsername());
                    resp.setAllLeadCount(list.size());
                    List<GetLeadDataDTO> validList = list.stream()
                            .filter(f -> f.getIsValid() == GlobalConstant.Number.ONE_INT)
                            .toList();
                    if(CollUtil.isNotEmpty(validList)){
                        resp.setValidLeadCount(validList.size());
                    }
                }
                if(MapUtils.isNotEmpty(finalOrderDetailCollect) && CollUtil.isNotEmpty(finalOrderDetailCollect.get(item.getUsername()))){
                    List<GetOrderDetailDataDTO> list = finalOrderDetailCollect.get(item.getUsername());
                    resp.setDealCount(list.size());
                    resp.setDealAmount(list.stream()
                            .map(GetOrderDetailDataDTO::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                }
                respList.add(resp);
            });
        }
        return respList;
    }

    /**
     * 导出线束数据(推广部)
     * @param req
     * @param response
     * @return
     */
    public void exportPromotionList(GetLeadAnalysisDataReq req, HttpServletResponse response) {
        List<ExportPromotionLeadAnalysisDataResp> exportList = CopierCopyUtils.convertTo(this.promotionList(req), ExportPromotionLeadAnalysisDataResp.class);
        ExcelUtil<ExportPromotionLeadAnalysisDataResp> util = new ExcelUtil<>(ExportPromotionLeadAnalysisDataResp.class);
        util.exportExcel(response, exportList, "线索数据");
    }

    /**
     * 销售线索数据
     *
     * @param req
     * @param tenantId
     * @return
     */
    public List<GetSaleLeadResp> saleLeadList(GetSaleLeadListReq req, Long tenantId) {
        List<GetSaleLeadResp> respList = new ArrayList<>();
        //销售部
        List<User> userList = userMapper.selectUsersByDeptWithChildren(GlobalConstant.Number.SIX_LONG, GlobalConstant.Number.ONE_INT);
        if(CollUtil.isNotEmpty(userList)) {

            Map<String, Object> params = new HashMap<>();
            if(ObjectUtils.isNotEmpty(req.getStart())){
                params.put("start", DateUtils.localDateTimeToDate(req.getStart()));
            }
            if(ObjectUtils.isNotEmpty(req.getEnd())){
                params.put("end", DateUtils.localDateTimeToDate(req.getEnd()));
            }
            if(StringUtils.isNotEmpty(req.getSource())){
                params.put("source", req.getSource());
            }
            if(ObjectUtils.isNotEmpty(tenantId)){
                params.put("tenantId", tenantId);
            }
            List<String> userNameList = userList.stream().map(User::getUsername).toList();
            params.put("userNameList", userNameList);
            // 线索 全部
            List<SaleLeadOrCustomerDataDto> allLeadList = leadAnalysisMapper.getSaleAllLeadList(params);
            Map<String, Integer> allLeadCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(allLeadList)){
                allLeadCollect = allLeadList.stream().collect(Collectors.toMap(SaleLeadOrCustomerDataDto::getOwnerUser, SaleLeadOrCustomerDataDto::getCount));
            }
            // 线索 已成交
            List<SaleLeadOrCustomerDataDto> transactionLeadList = leadAnalysisMapper.getSaleTransactionLeadList(params);
            Map<String, Integer> transactionLeadCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(transactionLeadList)){
                transactionLeadCollect = transactionLeadList.stream().collect(Collectors.toMap(SaleLeadOrCustomerDataDto::getOwnerUser, SaleLeadOrCustomerDataDto::getCount));
            }
            // 订单数据，查询条件以客户维度查询， 历史数据线索、客户会存在负责人不同的问题
            List<SaleLeadOrCustomerDataDto> allOrderByLead = leadAnalysisMapper.getSalesAllOrderByLead(params);
            Map<String, Integer> allOrderByLeadCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(transactionLeadList)){
                allOrderByLeadCollect = allOrderByLead.stream().collect(Collectors.toMap(SaleLeadOrCustomerDataDto::getOwnerUser, SaleLeadOrCustomerDataDto::getCount));
            }

            /*// 客户 全部
            List<SaleLeadOrCustomerDataDto> allCustomerList = leadAnalysisMapper.getSaleAllCustomerList(params);
            Map<String, Integer> allCustomerCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(allCustomerList)){
                allCustomerCollect = allCustomerList.stream().collect(Collectors.toMap(SaleLeadOrCustomerDataDto::getOwnerUser, SaleLeadOrCustomerDataDto::getCount));
            }
            // 客户 已成交
            List<SaleLeadOrCustomerDataDto> transactionCustomerList = leadAnalysisMapper.getSaleTransactionCustomerList(params);
            Map<String, Integer> transactionCustomerCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(transactionCustomerList)){
                transactionCustomerCollect = transactionCustomerList.stream().collect(Collectors.toMap(SaleLeadOrCustomerDataDto::getOwnerUser, SaleLeadOrCustomerDataDto::getCount));
            }*/
            Map<String, Integer> finalAllLeadCollect = allLeadCollect;
            Map<String, Integer> finalTransactionCollect = transactionLeadCollect;
            Map<String, Integer> finalAllOrderByLeadCollect = allOrderByLeadCollect;
            /*Map<String, Integer> finalAllCustomerCollect = allCustomerCollect;
            Map<String, Integer> finalTransactionCustomerCollect = transactionCustomerCollect;*/
            userList.forEach(item -> {
                GetSaleLeadResp resp = new GetSaleLeadResp();
                resp.setOwnerUser(item.getUsername());
                resp.setOwnerUserName(item.getRealName());
                if(CollUtil.isNotEmpty(finalAllLeadCollect) && finalAllLeadCollect.containsKey(item.getUsername())){
                    resp.setAllLeadCount(finalAllLeadCollect.get(item.getUsername()));
                }
                if(CollUtil.isNotEmpty(finalTransactionCollect) && finalTransactionCollect.containsKey(item.getUsername())){
                    resp.setTransactionLeadCount(finalTransactionCollect.get(item.getUsername()));
                    BigDecimal divide = new BigDecimal(resp.getTransactionLeadCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getAllLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setLeadConversionRate(divide + GlobalConstant.Symbol.PER_CENT);
                }
                if(CollUtil.isNotEmpty(finalAllOrderByLeadCollect) && finalAllOrderByLeadCollect.containsKey(item.getUsername())){
                    resp.setAllOrderByLeadCount(finalAllOrderByLeadCollect.get(item.getUsername()));
                    BigDecimal divide = new BigDecimal(resp.getAllOrderByLeadCount()).multiply(new BigDecimal(GlobalConstant.Number.HUNDRED_INT))
                            .divide(new BigDecimal(resp.getAllLeadCount()), GlobalConstant.Number.TWO_INT, RoundingMode.HALF_UP);
                    resp.setOrderConversionRate(divide + GlobalConstant.Symbol.PER_CENT);
                }
                respList.add(resp);
            });
        }
        return respList;
    }

    /**
     * 导出销售线索数据
     * @param req
     * @param response
     * @param tenantId
     */
    public void exportSaleLeadList(GetSaleLeadListReq req, HttpServletResponse response, Long tenantId) {
        List<ExportSaleLeadResp> exportList = CopierCopyUtils.convertTo(this.saleLeadList(req, tenantId), ExportSaleLeadResp.class);
        ExcelUtil<ExportSaleLeadResp> util = new ExcelUtil<>(ExportSaleLeadResp.class);
        util.exportExcel(response, exportList, "线索数据");
    }

    /**
     * 销售订单数据
     * @param tenantId
     * @return
     */
    public List<GetSaleOrderListResp> saleOrderList(Long tenantId) {
        List<GetSaleOrderListResp> respList = new ArrayList<>();
        //销售部
        List<User> userList = userMapper.selectUsersByDeptWithChildren(GlobalConstant.Number.SIX_LONG, GlobalConstant.Number.ONE_INT);
        if(CollUtil.isNotEmpty(userList)) {
            Map<String, Object> params = new HashMap<>();
            if(ObjectUtils.isNotEmpty(tenantId)){
                params.put("tenantId", tenantId);
            }
            List<String> userNameList = userList.stream().map(User::getUsername).toList();
            params.put("userNameList", userNameList);
            params.put("today", LocalDate.now());
            params.put("yesterday", LocalDate.now().minusDays(GlobalConstant.Number.ONE_LONG));
            params.put("monthStart", DateUtils.getFirstDayOfCurrentMonth().atStartOfDay());
            params.put("monthEnd", DateUtils.getLastDayOfMonthWithTime());
            params.put("lastMonthStart", DateUtils.getFirstDayOfPreviousMonth().atStartOfDay());
            params.put("lastMonthEnd", LocalDateTime.of(DateUtils.getLastDayOfPreviousMonth(), LocalTime.MAX));
            params.put("quarterStart", DateUtils.getFirstDayOfQuarter().atStartOfDay());
            params.put("quarterEnd", LocalDateTime.of(DateUtils.getLastDayOfQuarter(), LocalTime.MAX));
            params.put("yearStart", DateUtils.getFirstDayOfYear().atStartOfDay());
            params.put("yearEnd", LocalDateTime.of(DateUtils.getLastDayOfYear(), LocalTime.MAX));
            List<SaleOrderDataDto> saleOrderList = leadAnalysisMapper.getSaleOrderList(params);
            Map<String, SaleOrderDataDto> saleOrderCollect = new HashMap<>();
            if(CollUtil.isNotEmpty(saleOrderList)){
                saleOrderCollect = saleOrderList.stream().collect(Collectors.toMap(SaleOrderDataDto::getOwnerUser, v -> v));
            }
            Map<String, SaleOrderDataDto> finalSaleOrderCollect = saleOrderCollect;
            userList.forEach(item -> {
                GetSaleOrderListResp resp = new GetSaleOrderListResp();
                if(CollUtil.isNotEmpty(finalSaleOrderCollect) && finalSaleOrderCollect.containsKey(item.getUsername())){
                    resp = CopierCopyUtils.convertTo(finalSaleOrderCollect.get(item.getUsername()), GetSaleOrderListResp.class);
                }
                resp.setOwnerUserName(item.getRealName());
                respList.add(resp);
            });

        }
        return respList;
    }

    /**
     * 导出销售订单数据
     * @param response
     * @param tenantId
     */
    public void exportSaleOrderList(HttpServletResponse response, Long tenantId) {
        List<ExportSaleOrderListResp> exportList = CopierCopyUtils.convertTo(this.saleOrderList(tenantId), ExportSaleOrderListResp.class);
        ExcelUtil<ExportSaleOrderListResp> util = new ExcelUtil<>(ExportSaleOrderListResp.class);
        util.exportExcel(response, exportList, "订单数据");
    }
}
