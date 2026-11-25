package com.qykj.finmaster.payment.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.SignUtils;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.config.PaymentConfiguration;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.order.service.OrderServiceImpl;
import com.qykj.finmaster.payment.domain.PaymentRecord;
import com.qykj.finmaster.payment.domain.req.JsapiCloseReq;
import com.qykj.finmaster.payment.domain.req.JsapiPrepayReq;
import com.qykj.finmaster.payment.domain.PaymentConfig;
import com.qykj.finmaster.payment.domain.req.JsapiQueryReq;
import com.qykj.finmaster.payment.domain.req.JsapiRefundReq;
import com.qykj.finmaster.payment.domain.resp.JsapiPrepayResp;
import com.qykj.finmaster.payment.domain.resp.JsapiQueryResp;
import com.qykj.finmaster.payment.manager.impl.PaymentConfigManagerImpl;
import com.qykj.finmaster.payment.manager.impl.PaymentRecordManagerImpl;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.manager.impl.ProductManagerImpl;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.util.PemUtil;
import com.wechat.pay.java.service.payments.jsapi.JsapiService;
import com.wechat.pay.java.service.payments.jsapi.model.*;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @Author zhb
 * @Date 2025-06-12
 * @Description
 */
@Service
@Slf4j
public class PaymentServiceImpl {

    @Autowired
    private PaymentConfiguration paymentConfiguration;
    @Autowired
    private PaymentConfigManagerImpl paymentConfigManager;
    @Autowired
    private OrderServiceImpl orderService;
    @Autowired
    private PaymentRecordServiceImpl paymentRecordService;
    @Autowired
    private PaymentRecordManagerImpl paymentRecordManager;
    @Autowired
    private OrderManagerImpl orderManager;
    @Autowired
    private OrderDetailManagerImpl orderDetailManager;
    @Autowired
    private ProductManagerImpl productManager;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    /**
     * 创建JsapiService
     */
    private JsapiService createJsapiService(String paymentName) {
        Config config = paymentConfiguration.getConfig(paymentName);
        if (config == null) {
            throw new RuntimeException("商户配置不存在: " + paymentName);
        }
        return new JsapiService.Builder().config(config).build();
    }

    /**
     * 创建RefundService
     */
    private RefundService createRefundService(String paymentName) {
        Config config = paymentConfiguration.getConfig(paymentName);
        if (config == null) {
            throw new RuntimeException("商户配置不存在: " + paymentName);
        }
        return new RefundService.Builder().config(config).build();
    }

    /**
     * 创建PaymentConfig
     */
    private PaymentConfig createPaymentConfig(String paymentName) {
        PaymentConfig paymentConfig = paymentConfiguration.getPaymentConfig(paymentName);
        if (paymentConfig == null) {
            throw new RuntimeException("商户支付配置不存在: " + paymentName);
        }
        return paymentConfig;
    }

    /**
     * 创建NotificationParser
     */
    private NotificationParser createNotificationParser(String paymentName) {
        NotificationParser notificationParser = paymentConfiguration.getNotificationParser(paymentName);
        if (notificationParser == null) {
            throw new RuntimeException("商户支付配置不存在: " + paymentName);
        }
        return notificationParser;
    }


    /**
     * JSAPI下单
     * @param req
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public JsapiPrepayResp wxJsapiPrepay(JsapiPrepayReq req) {
        JsapiPrepayResp resp = new JsapiPrepayResp();
        //创建订单
        // todo 临时在发起支付时创建订单， 后期可能会把创建订单进行前置操作
        String orderNo = getOrderNo(WxConstants.WX_PAY_ORDER_NO_PRE);
        resp.setOrderNo(orderNo);
        orderService.createOrder(orderNo, req.getOpenId(), req.getCompanyId(), req.getProductId());
        //商户支付配置
        PaymentConfig paymentConfig = createPaymentConfig(req.getPaymentName());
        //创建支付记录
        Integer payAmount = req.getAmount().multiply(new BigDecimal(100)).intValue();
        paymentRecordService.createPaymentRecord(orderNo, paymentConfig.getId(), payAmount);
        //微信下单参数
        PrepayRequest request = getPrepayRequest(req, paymentConfig, orderNo);
        try {
            JsapiService jsapiService = createJsapiService(req.getPaymentName());
            PrepayResponse prepayResponse = jsapiService.prepay(request);
            if(ObjectUtils.isNotEmpty(prepayResponse)){
                resp.setPrepayId(WxConstants.WX_PAY_PREPAY_PRE + prepayResponse.getPrepayId());
                getJsapiPrepayResp(resp, paymentConfig);
            }
        }catch (Exception e){
            log.error("微信支付下单失败：" + e);
        }
        return resp;
    }

    /**
     * 微信订单交易查询
     * @param req
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public JsapiQueryResp wxJsapiQuery(JsapiQueryReq req) {
        JsapiQueryResp resp = new JsapiQueryResp();
        PaymentConfig paymentConfig = createPaymentConfig(req.getPaymentName());
        //微信查询订单参数
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(paymentConfig.getMchId());
        request.setOutTradeNo(req.getOrderNo());
        try {
            JsapiService jsapiService = createJsapiService(req.getPaymentName());
            Transaction transaction = jsapiService.queryOrderByOutTradeNo(request);
            updateOrderData(transaction.getTradeState().name(), transaction.getOutTradeNo(),
                    transaction.getSuccessTime(), transaction.getTransactionId());
            resp.setTradeStatus(transaction.getTradeState().name());
        } catch (Exception e) {
            log.error("微信支付查询失败:" + req.getOrderNo() + e);
        }
        return resp;
    }

    /**
     * 微信支付回调
     * @param req
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean wxJsapiNotify(Map<String, Object> req) {
        RequestParam requestParam = new RequestParam.Builder()
                .serialNumber(req.get("Wechatpay-Serial").toString())
                .signature(req.get("Wechatpay-Signature").toString())
                .nonce(req.get("Wechatpay-Nonce").toString())
                .timestamp(req.get("Wechatpay-Timestamp").toString())
                .body(req.get("body").toString())
                .build();
        try {
            NotificationParser parser = createNotificationParser(WxConstants.DEFAULT_PAYMENT_NAME);
            Transaction transaction = parser.parse(requestParam, Transaction.class);
            updateOrderData(transaction.getTradeState().name(), transaction.getOutTradeNo(),
                    transaction.getSuccessTime(), transaction.getTransactionId());
        } catch (Exception e) {
            log.error("微信支付回调异常:"  + e);
        }
        return Boolean.TRUE;
    }

    /**
     * 关闭支付订单
     * @param req
     * @return
     */
    public Boolean wxJsapiClose(JsapiCloseReq req) {
        PaymentConfig paymentConfig = createPaymentConfig(req.getPaymentName());
        //微信关闭订单参数
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(paymentConfig.getMchId());
        request.setOutTradeNo(req.getOrderNo());
        try {
            JsapiService jsapiService = createJsapiService(req.getPaymentName());
            jsapiService.closeOrder(request);
            updateOrderData(WxConstants.TradeState.CLOSED, req.getOrderNo(), null, null);
            log.info("微信支付订单关闭:" + req.getOrderNo());
        } catch (Exception e) {
            log.error("微信支付查询失败:" + req.getOrderNo() + e);
        }
        return Boolean.TRUE;
    }

    /**
     * 微信订单退款
     * @param req
     * @return
     */
    public Boolean wxJsapiRefund(JsapiRefundReq req) {
        String refundNo = getOrderNo(WxConstants.WX_PAY_ORDER_REFUND_NO_PRE);
        PaymentConfig paymentConfig = createPaymentConfig(req.getPaymentName());
        //微信退款参数
        CreateRequest request = getCreateRequest(req, refundNo, paymentConfig);
        try {
            RefundService refundService = createRefundService(req.getPaymentName());
            Refund refund = refundService.create(request);
            updateOrderData(WxConstants.TradeState.REFUND, req.getOrderNo(), null, null);
            log.info("微信支付退款:" + req.getOrderNo());
        } catch (Exception e) {
            log.error("微信退款失败:" + req.getOrderNo() + e);
        }
        return Boolean.TRUE;
    }

    /**
     * 组装微信退款参数
     * @param req
     * @param refundNo
     * @param paymentConfig
     * @return
     */
    private static CreateRequest getCreateRequest(JsapiRefundReq req, String refundNo, PaymentConfig paymentConfig) {
        AmountReq amountReq = new AmountReq();
        amountReq.setRefund(req.getRefundAmount().multiply(new BigDecimal(100)).longValue());
        amountReq.setTotal(req.getTotalAmount().multiply(new BigDecimal(100)).longValue()); // 原订单金额
        amountReq.setCurrency("CNY");

        // 构建退款请求
        CreateRequest request = new CreateRequest();
        request.setOutTradeNo(req.getOrderNo());
        request.setOutRefundNo(refundNo);
        request.setAmount(amountReq);
        request.setNotifyUrl(paymentConfig.getRefundNotifyUrl()); // 退款结果通知地址
        return request;
    }

    /**
     * 修改订单数据
     * @param tradeState
     * @param orderNo
     * @param successTime
     * @param transactionId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrderData(String tradeState, String orderNo, String successTime, String transactionId){
        LocalDateTime payTime = null;
        String orderStatus = null;
        String payRecordStatus = null;
        Order order = orderManager.lambdaQuery()
                .eq(Order::getOrderNo, orderNo)
                .one();
        switch (tradeState) {
            case WxConstants.TradeState.SUCCESS:
                payTime = DateUtils.parseRfc3339(successTime, DateUtils.YYYY_MM_DD_HH_MM_SS);
                orderStatus = DictConstants.ORDER_STATUS_PAID;
                payRecordStatus = DictConstants.PAY_STATUS_SUCCESS;
                break;
            case WxConstants.TradeState.CLOSED:
                orderStatus = DictConstants.ORDER_STATUS_CANCELLED;
                payRecordStatus = DictConstants.PAY_STATUS_CANCEL;
                break;
            case WxConstants.TradeState.REFUND:
                orderStatus = DictConstants.ORDER_STATUS_REFUNDED;
                payRecordStatus = DictConstants.PAY_STATUS_FAIL;
                break;
            default:
                if(DateUtils.isTimeoutOfSeconds(order.getCreateTime(), 30 * 60)){
                    orderStatus = DictConstants.ORDER_STATUS_CANCELLED;
                    payRecordStatus = DictConstants.PAY_STATUS_CANCEL;
                }else {
                    orderStatus = DictConstants.ORDER_STATUS_PENDING;
                    payRecordStatus = DictConstants.PAY_STATUS_WAIT;
                }
        }
        if(ObjectUtils.isNotEmpty(order)){
            paymentRecordManager.lambdaUpdate()
                    .set(ObjectUtils.isNotEmpty(payTime), PaymentRecord::getPayTime, payTime)
                    .set(StringUtils.isNotEmpty(transactionId), PaymentRecord::getTransactionId, transactionId)
                    .set(PaymentRecord::getPaymentStatus, payRecordStatus)
                    .set(PaymentRecord::getUpdateTime, new Date())
                    .eq(PaymentRecord::getOrderNo, orderNo)
                    .update();
            orderManager.lambdaUpdate()
                    .set(Order::getStatus, orderStatus)
                    .set(ObjectUtils.isNotEmpty(payTime), Order::getPayTime, payTime)
                    .set(Order::getUpdateTime, new Date())
                    .set(DictConstants.ORDER_STATUS_PAID.equals(orderStatus), Order::getPaidAmount, order.getTotalAmount())
                    .eq(Order::getOrderNo, orderNo)
                    .update();
            // 如果订单含有发票服务，则给公司开通此功能
            if(isHavingInvoiceProduct(order.getId())){
                customerCompanyManager.lambdaUpdate()
                        .set(CustomerCompany::getIsInvoicing, GlobalConstant.Number.ONE_INT)
                        .eq(CustomerCompany::getId, order.getCompanyId())
                        .update();
            }

        }
        return Boolean.TRUE;
    }

    /**
     * 判断订单是否含有发票服务
     * @param orderId
     * @return
     */
    private Boolean isHavingInvoiceProduct(Long orderId){
        Boolean flag = Boolean.FALSE;
        List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .list();
        if(CollUtil.isNotEmpty(orderDetailList)){
            List<Long> productIdList = orderDetailList.stream().map(OrderDetail::getProductId).toList();
            Product product = productManager.lambdaQuery()
                    .in(Product::getId, productIdList)
                    .eq(Product::getProductNumber, "invoicing")
                    .one();
            if(ObjectUtils.isNotEmpty(product)){
                flag = Boolean.TRUE;
            }
        }
        return flag;
    }

    /**
     * 组装支付下单参数
     * @param req
     * @param paymentConfig
     * @param orderNo
     * @return
     */
    private PrepayRequest getPrepayRequest(JsapiPrepayReq req, PaymentConfig paymentConfig, String orderNo) {
        PrepayRequest request = new PrepayRequest();
        Amount amount = new Amount();
        amount.setTotal(req.getAmount().multiply(new BigDecimal(100)).intValue());
        amount.setCurrency(WxConstants.WX_PAY_CURRENCY_CNY);
        Payer payer = new Payer();
        payer.setOpenid(req.getOpenId());
        request.setAppid(paymentConfig.getAppId());
        request.setMchid(paymentConfig.getMchId());
        request.setDescription(req.getDescription());
        request.setNotifyUrl(paymentConfig.getNotifyUrl());
        request.setOutTradeNo(orderNo);
        request.setTimeExpire(DateUtils.toRFC3339Str(DateUtils.localDateTimeAddMinutes(LocalDateTime.now(),
                        WxConstants.WX_PAY_TIME_OUT_INTERVAL), DateUtils.RFC3339_YYYY_MM_DD_HH_MM_SS));
        request.setAmount(amount);
        request.setPayer(payer);
        return request;
    }

    /**
     * 组装微信支付下单返回参数
     * @param resp
     * @param paymentConfig
     * @return
     */
    private void getJsapiPrepayResp(JsapiPrepayResp resp, PaymentConfig paymentConfig){
        resp.setAppId(paymentConfig.getAppId());
        resp.setTimeStamp(DateUtils.getCurrentUnixTimestampStr());
        resp.setNonceStr(UUID.randomUUID().toString().replace("-", ""));
        resp.setSignType(GlobalConstant.SIGN_RSA);
        resp.setPaySign(getSign(resp, paymentConfig.getPrivateKeyPath()));
    }

    /**
     * 获取签名值
     * @param resp
     * @param fileName
     * @return
     */
    private String getSign(JsapiPrepayResp resp, String fileName){
        PrivateKey privateKey = PemUtil.loadPrivateKeyFromPath(fileName);
        String sign = SignUtils.RSASign(resp.getAppId() + GlobalConstant.Symbol.ENTER
                        + resp.getTimeStamp() + GlobalConstant.Symbol.ENTER
                        + resp.getNonceStr() + GlobalConstant.Symbol.ENTER
                        + resp.getPrepayId() + GlobalConstant.Symbol.ENTER,
                privateKey, GlobalConstant.SIGN_RSA_256);
        return sign;
    }

    /**
     * 获取订单编号 前缀+当前日期+时间戳
     * @param pre
     * @return
     */
    private String getOrderNo(String pre){
        String orderNo = pre + DateUtils.dateToString(new Date(), DateUtils.YYYYMMDD) + System.currentTimeMillis();
        return orderNo;
    }
}