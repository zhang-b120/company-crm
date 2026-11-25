package com.qykj.finmaster.order.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.order.domain.req.GetOrderLogInfoReq;
import com.qykj.finmaster.order.domain.resp.OrderLogResp;
import com.qykj.finmaster.order.manager.OrderLogManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qykj.finmaster.order.mapper.OrderLogMapper;
import com.qykj.finmaster.order.domain.OrderLog;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单日志Service业务层处理
 */
@Service
public class OrderLogServiceImpl {

    @Autowired
    private OrderLogMapper orderLogMapper;
    @Autowired
    private OrderLogManager orderLogManager;

    /**
     * 查询订单日志
     */
    public OrderLogResp selectOrderLogById(Long id) {
        OrderLog result = orderLogManager.lambdaQuery()
                .eq(OrderLog::getId, id)
                .one();
        return CopierCopyUtils.convertTo(result, OrderLogResp.class);
    }

    /**
     * 查询订单日志列表
     */
    public List<OrderLog> selectOrderLogList(GetOrderLogInfoReq getOrderLogInfoReq, Long tenantId) {
        OrderLog orderLog = CopierCopyUtils.convertTo(getOrderLogInfoReq, OrderLog.class);
        return orderLogManager.lambdaQuery()
                .eq(ObjectUtils.isNotEmpty(tenantId), OrderLog::getTenantId, tenantId)
                .eq(ObjectUtils.isNotEmpty(getOrderLogInfoReq.getCustomerId()), OrderLog::getCustomerId, orderLog.getCustomerId())
                .eq(ObjectUtils.isNotEmpty(getOrderLogInfoReq.getLogType()), OrderLog::getLogType, orderLog.getLogType())
                .list();
    }

    /**
     * 新增订单日志
     */
    public boolean insertOrderLog(OrderLogResp orderLogResp, Long tenantId) {
        // 设置租户ID
        if (orderLogResp.getTenantId() == null) {
            orderLogResp.setTenantId(tenantId);
        }
        OrderLog orderLog = CopierCopyUtils.convertTo(orderLogResp, OrderLog.class);
        Boolean flag = orderLogManager.save(orderLog);
        return flag;
    }

    /**
     * 修改订单日志
     */
    public int updateOrderLog(OrderLogResp orderLogResp) {
        // 确保租户ID不被修改
        OrderLog orderLog = CopierCopyUtils.convertTo(orderLogResp, OrderLog.class);
        return orderLogManager.getBaseMapper().updateOrderLog(orderLog);
    }

    /**
     * 批量删除订单日志
     */
    public int deleteOrderLogByIds(Long[] ids, Long tenantId) {
        return orderLogManager.getBaseMapper().deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 删除订单日志信息
     */
    public int deleteOrderLogById(Long id, Long tenantId) {
        int result = orderLogMapper.deleteOrderLogById(id);
        return result;
    }

    /**
     * 创建订单日志
     *
     * @param orderId
     * @param logType
     * @param content
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean createLog(Long orderId, String logType, String content) {
        OrderLog log = new OrderLog();
        log.setLogType(logType);
        log.setContent(content);
        log.setOperator("admin");
        log.setOperatorName("系统管理员");
        log.setCreateTime(new Date());
        orderLogManager.save(log);
        return Boolean.TRUE;
    }
}
