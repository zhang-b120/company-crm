package com.qykj.finmaster.chatbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qykj.finmaster.chatbot.domain.ChatbotConversation;
import com.qykj.finmaster.chatbot.domain.ChatbotMessage;
import com.qykj.finmaster.chatbot.mapper.ChatbotConversationMapper;
import com.qykj.finmaster.chatbot.mapper.ChatbotMessageMapper;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.manager.impl.CustomerLogManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.manager.impl.LeadLogManagerImpl;
import com.qykj.finmaster.order.domain.req.OrderDetailInfoReq;
import com.qykj.finmaster.order.domain.req.QuickOrderReq;
import com.qykj.finmaster.order.service.QuickOrderService;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class ChatbotService {

    @Value("${third.deepseek.api-key}")
    private String apiKey;

    @Value("${third.deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    private static final String MODEL = "deepseek-chat";
    private static final int MAX_HISTORY = 20;
    private static final int MAX_TOOL_ROUNDS = 5;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private ChatbotConversationMapper conversationMapper;
    @Autowired private ChatbotMessageMapper messageMapper;
    @Autowired private LeadManagerImpl leadManager;
    @Autowired private CustomerInfoManagerImpl customerInfoManager;
    @Autowired private CustomerLogManagerImpl customerLogManager;
    @Autowired private LeadLogManagerImpl leadLogManager;
    @Autowired private QuickOrderService quickOrderService;

    // ─── 工具定义 ─────────────────────────────────────────────────────────────

    private static final String TOOLS_JSON = """
            [
              {
                "type":"function",
                "function":{
                  "name":"add_lead",
                  "description":"新增线索。当用户说想添加线索/潜在客户时调用。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "leadName":    {"type":"string","description":"线索名称（必填）"},
                      "contactName": {"type":"string","description":"联系人姓名"},
                      "phone":       {"type":"string","description":"联系手机号"},
                      "companyName": {"type":"string","description":"公司名称"},
                      "source":      {"type":"string","description":"来源"},
                      "remark":      {"type":"string","description":"备注"}
                    },
                    "required":["leadName"]
                  }
                }
              },
              {
                "type":"function",
                "function":{
                  "name":"query_leads",
                  "description":"查询线索列表。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "leadName":{"type":"string","description":"线索名称关键词"},
                      "phone":   {"type":"string","description":"手机号"},
                      "status":  {"type":"integer","description":"状态：1未跟进 2跟进中 3已转化 4已失效"},
                      "pageSize":{"type":"integer","description":"返回条数，默认10"}
                    }
                  }
                }
              },
              {
                "type":"function",
                "function":{
                  "name":"add_lead_follow",
                  "description":"给线索添加跟进记录。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "leadId":      {"type":"integer","description":"线索ID"},
                      "leadName":    {"type":"string","description":"线索名称（没有ID时用于查找）"},
                      "content":     {"type":"string","description":"跟进内容（必填）"},
                      "followMethod":{"type":"string","description":"跟进方式，如：电话/微信/拜访"}
                    },
                    "required":["content"]
                  }
                }
              },
              {
                "type":"function",
                "function":{
                  "name":"query_customers",
                  "description":"查询私海客户列表。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "customerName":{"type":"string","description":"客户名称关键词"},
                      "phone":       {"type":"string","description":"手机号"},
                      "status":      {"type":"integer","description":"状态：1未跟进 2跟进中 3已转化 4已失效"},
                      "pageSize":    {"type":"integer","description":"返回条数，默认10"}
                    }
                  }
                }
              },
              {
                "type":"function",
                "function":{
                  "name":"add_customer_follow",
                  "description":"给私海客户添加跟进记录。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "customerId":  {"type":"integer","description":"客户ID"},
                      "customerName":{"type":"string","description":"客户名称（没有ID时用于查找）"},
                      "content":     {"type":"string","description":"跟进内容（必填）"},
                      "followMethod":{"type":"string","description":"跟进方式"}
                    },
                    "required":["content"]
                  }
                }
              },
              {
                "type":"function",
                "function":{
                  "name":"quick_create_order",
                  "description":"快速建单，一步完成客户+公司+订单创建。",
                  "parameters":{
                    "type":"object",
                    "properties":{
                      "companyName": {"type":"string","description":"公司名称（必填）"},
                      "contactName": {"type":"string","description":"联系人"},
                      "phone":       {"type":"string","description":"联系电话"},
                      "totalAmount": {"type":"number","description":"订单总金额（必填）"},
                      "productName": {"type":"string","description":"产品/服务名称"},
                      "paymentType": {"type":"string","description":"支付方式"},
                      "status":      {"type":"string","description":"订单状态 PENDING/PAID，默认PENDING"},
                      "remark":      {"type":"string","description":"备注"}
                    },
                    "required":["companyName","totalAmount"]
                  }
                }
              }
            ]
            """;

    // ─── 对话入口 ──────────────────────────────────────────────────────────────

    public String chat(Long conversationId, String userMessage, Long tenantId, LoginUser loginUser) throws Exception {
        saveMessage(conversationId, tenantId, "user", userMessage, null, null, null);
        ensureConversationTitle(conversationId, userMessage);

        List<Map<String, Object>> messages = buildMessageHistory(conversationId, tenantId, loginUser);

        String assistantContent = null;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            Map<String, Object> response = callDeepSeek(messages);
            JsonNode choice = objectMapper.valueToTree(response).path("choices").get(0);
            JsonNode msgNode = choice.path("message");
            String finishReason = choice.path("finish_reason").asText();
            String content = msgNode.path("content").isNull() ? null : msgNode.path("content").asText(null);
            JsonNode toolCalls = msgNode.path("tool_calls");

            if ("tool_calls".equals(finishReason) && !toolCalls.isMissingNode() && !toolCalls.isEmpty()) {
                String toolCallsJson = objectMapper.writeValueAsString(toolCalls);
                saveMessage(conversationId, tenantId, "assistant", content, null, null, toolCallsJson);

                Map<String, Object> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                if (content != null) assistantMsg.put("content", content);
                assistantMsg.put("tool_calls", objectMapper.convertValue(toolCalls, List.class));
                messages.add(assistantMsg);

                for (JsonNode tc : toolCalls) {
                    String toolCallId = tc.path("id").asText();
                    String functionName = tc.path("function").path("name").asText();
                    String argumentsJson = tc.path("function").path("arguments").asText();
                    JsonNode args = objectMapper.readTree(argumentsJson);

                    String result = executeTool(functionName, args, tenantId, loginUser);
                    saveMessage(conversationId, tenantId, "tool", result, toolCallId, functionName, null);

                    messages.add(Map.of("role", "tool", "tool_call_id", toolCallId, "content", result));
                }
            } else {
                assistantContent = content != null ? content : "操作已完成。";
                saveMessage(conversationId, tenantId, "assistant", assistantContent, null, null, null);
                break;
            }
        }
        return assistantContent != null ? assistantContent : "操作已完成。";
    }

    // ─── 会话管理 ──────────────────────────────────────────────────────────────

    public Long createConversation(Long tenantId, LoginUser loginUser) {
        ChatbotConversation conv = new ChatbotConversation();
        conv.setTenantId(tenantId);
        conv.setUserId(loginUser.getUsername());
        conv.setUserName(loginUser.getRealName());
        conv.setTitle("新对话");
        conv.setCreateTime(new Date());
        conv.setDeleted(0);
        conversationMapper.insert(conv);
        return conv.getId();
    }

    public List<ChatbotConversation> listConversations(Long tenantId, String userId) {
        return conversationMapper.selectList(
                new LambdaQueryWrapper<ChatbotConversation>()
                        .eq(ChatbotConversation::getTenantId, tenantId)
                        .eq(ChatbotConversation::getUserId, userId)
                        .eq(ChatbotConversation::getDeleted, 0)
                        .orderByDesc(ChatbotConversation::getCreateTime)
                        .last("LIMIT 50")
        );
    }

    public List<ChatbotMessage> listMessages(Long conversationId, Long tenantId) {
        return messageMapper.selectList(
                new LambdaQueryWrapper<ChatbotMessage>()
                        .eq(ChatbotMessage::getConversationId, conversationId)
                        .eq(ChatbotMessage::getTenantId, tenantId)
                        .eq(ChatbotMessage::getDeleted, 0)
                        .in(ChatbotMessage::getRole, "user", "assistant")
                        .orderByAsc(ChatbotMessage::getCreateTime)
        );
    }

    public void deleteConversation(Long conversationId) {
        ChatbotConversation conv = new ChatbotConversation();
        conv.setId(conversationId);
        conv.setDeleted(1);
        conversationMapper.updateById(conv);
    }

    // ─── 内部方法 ──────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildMessageHistory(Long conversationId, Long tenantId, LoginUser loginUser) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(loginUser)));

        List<ChatbotMessage> history = messageMapper.selectList(
                new LambdaQueryWrapper<ChatbotMessage>()
                        .eq(ChatbotMessage::getConversationId, conversationId)
                        .eq(ChatbotMessage::getTenantId, tenantId)
                        .eq(ChatbotMessage::getDeleted, 0)
                        .orderByAsc(ChatbotMessage::getCreateTime)
                        .last("LIMIT " + MAX_HISTORY)
        );

        for (ChatbotMessage m : history) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role", m.getRole());
            if (StringUtils.isNotBlank(m.getContent())) {
                msg.put("content", m.getContent());
            } else if ("assistant".equals(m.getRole())) {
                msg.put("content", (Object) null);
            }
            if ("tool".equals(m.getRole()) && m.getToolCallId() != null) {
                msg.put("tool_call_id", m.getToolCallId());
            }
            if ("assistant".equals(m.getRole()) && StringUtils.isNotBlank(m.getToolCallsJson())) {
                try {
                    msg.put("tool_calls", objectMapper.readValue(m.getToolCallsJson(), List.class));
                } catch (Exception ignored) {}
            }
            messages.add(msg);
        }
        return messages;
    }

    private String buildSystemPrompt(LoginUser loginUser) {
        return "你是 FinMaster 财务管家系统的 AI 助手。当前登录用户：" + loginUser.getRealName() + "。" +
               "你可以帮助用户通过自然语言操作系统：查询/添加线索、添加跟进记录、查询客户、添加客户跟进、快速建单。" +
               "操作成功时告知结果，失败时说明原因。请用简洁友好的中文回复。";
    }

    private void saveMessage(Long convId, Long tenantId, String role, String content,
                              String toolCallId, String toolName, String toolCallsJson) {
        ChatbotMessage msg = new ChatbotMessage();
        msg.setConversationId(convId);
        msg.setTenantId(tenantId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setToolCallId(toolCallId);
        msg.setToolName(toolName);
        msg.setToolCallsJson(toolCallsJson);
        msg.setCreateTime(new Date());
        msg.setDeleted(0);
        messageMapper.insert(msg);
    }

    private void ensureConversationTitle(Long conversationId, String userMessage) {
        ChatbotConversation conv = conversationMapper.selectById(conversationId);
        if (conv != null && "新对话".equals(conv.getTitle())) {
            String title = userMessage.length() > 30 ? userMessage.substring(0, 30) + "…" : userMessage;
            conv.setTitle(title);
            conv.setUpdateTime(new Date());
            conversationMapper.updateById(conv);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callDeepSeek(List<Map<String, Object>> messages) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", messages);
        body.put("tools", objectMapper.readValue(TOOLS_JSON, List.class));
        body.put("tool_choice", "auto");
        body.put("temperature", 0.7);

        String bodyJson = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("DeepSeek API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("AI服务调用失败，HTTP " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), Map.class);
    }

    // ─── 工具函数执行 ──────────────────────────────────────────────────────────

    private String executeTool(String name, JsonNode args, Long tenantId, LoginUser loginUser) {
        try {
            return switch (name) {
                case "add_lead"            -> addLead(args, tenantId, loginUser);
                case "query_leads"         -> queryLeads(args, tenantId);
                case "add_lead_follow"     -> addLeadFollow(args, tenantId, loginUser);
                case "query_customers"     -> queryCustomers(args, tenantId);
                case "add_customer_follow" -> addCustomerFollow(args, tenantId, loginUser);
                case "quick_create_order"  -> quickCreateOrder(args, tenantId, loginUser);
                default                    -> "{\"error\":\"未知函数: " + name + "\"}";
            };
        } catch (Exception e) {
            log.error("工具 {} 执行失败", name, e);
            return "{\"success\":false,\"error\":\"" + safe(e.getMessage()) + "\"}";
        }
    }

    private String addLead(JsonNode args, Long tenantId, LoginUser loginUser) {
        Lead lead = new Lead();
        lead.setLeadName(args.path("leadName").asText());
        lead.setContactName(args.path("contactName").asText(null));
        lead.setPhone(args.path("phone").asText(null));
        lead.setCompanyName(args.path("companyName").asText(null));
        lead.setSource(args.path("source").asText(null));
        lead.setRemark(args.path("remark").asText(null));
        lead.setStatus(1);
        lead.setTenantId(tenantId);
        lead.setCreateBy(loginUser.getUsername());
        lead.setCreateByName(loginUser.getRealName());
        lead.setOwnerUser(loginUser.getUsername());
        lead.setOwnerUserName(loginUser.getRealName());
        leadManager.save(lead);
        return "{\"success\":true,\"leadId\":" + lead.getId() + ",\"message\":\"线索 [" + safe(lead.getLeadName()) + "] 添加成功\"}";
    }

    private String queryLeads(JsonNode args, Long tenantId) {
        LambdaQueryWrapper<Lead> wrapper = new LambdaQueryWrapper<Lead>()
                .eq(Lead::getTenantId, tenantId).eq(Lead::getDeleted, 0);
        if (has(args, "leadName")) wrapper.like(Lead::getLeadName, args.path("leadName").asText());
        if (has(args, "phone"))    wrapper.like(Lead::getPhone, args.path("phone").asText());
        if (has(args, "status"))   wrapper.eq(Lead::getStatus, args.path("status").asInt());
        int size = Math.min(args.path("pageSize").asInt(10), 20);
        wrapper.orderByDesc(Lead::getCreateTime).last("LIMIT " + size);

        List<Lead> list = leadManager.list(wrapper);
        StringBuilder sb = new StringBuilder("{\"count\":").append(list.size()).append(",\"items\":[");
        for (int i = 0; i < list.size(); i++) {
            Lead l = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(l.getId())
              .append(",\"leadName\":\"").append(safe(l.getLeadName())).append("\"")
              .append(",\"phone\":\"").append(safe(l.getPhone())).append("\"")
              .append(",\"status\":").append(l.getStatus()).append("}");
        }
        return sb.append("]}").toString();
    }

    private String addLeadFollow(JsonNode args, Long tenantId, LoginUser loginUser) {
        Long leadId = null;
        if (args.has("leadId") && !args.path("leadId").isNull()) {
            leadId = args.path("leadId").asLong();
        }
        if (leadId == null && has(args, "leadName")) {
            Lead found = leadManager.getOne(new LambdaQueryWrapper<Lead>()
                    .eq(Lead::getTenantId, tenantId).like(Lead::getLeadName, args.path("leadName").asText())
                    .eq(Lead::getDeleted, 0).last("LIMIT 1"));
            if (found != null) leadId = found.getId();
        }
        if (leadId == null) return "{\"success\":false,\"error\":\"未找到线索\"}";

        LeadLog log = new LeadLog();
        log.setLeadId(leadId);
        log.setLogType(2);
        log.setContent(args.path("content").asText());
        log.setFollowMethod(args.path("followMethod").asText(null));
        log.setOperatorName(loginUser.getRealName());
        log.setTenantId(tenantId);
        log.setCreateBy(loginUser.getUsername());
        log.setCreateByName(loginUser.getRealName());
        leadLogManager.save(log);
        return "{\"success\":true,\"message\":\"线索跟进记录已添加\"}";
    }

    private String queryCustomers(JsonNode args, Long tenantId) {
        LambdaQueryWrapper<CustomerInfo> wrapper = new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getTenantId, tenantId)
                .eq(CustomerInfo::getCustomerType, 1);
        if (has(args, "customerName")) wrapper.like(CustomerInfo::getCustomerName, args.path("customerName").asText());
        if (has(args, "phone"))        wrapper.like(CustomerInfo::getPhone, args.path("phone").asText());
        if (has(args, "status"))       wrapper.eq(CustomerInfo::getStatus, args.path("status").asInt());
        int size = Math.min(args.path("pageSize").asInt(10), 20);
        wrapper.orderByDesc(CustomerInfo::getCreateTime).last("LIMIT " + size);

        List<CustomerInfo> list = customerInfoManager.list(wrapper);
        StringBuilder sb = new StringBuilder("{\"count\":").append(list.size()).append(",\"items\":[");
        for (int i = 0; i < list.size(); i++) {
            CustomerInfo c = list.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":").append(c.getId())
              .append(",\"customerName\":\"").append(safe(c.getCustomerName())).append("\"")
              .append(",\"phone\":\"").append(safe(c.getPhone())).append("\"")
              .append(",\"status\":").append(c.getStatus()).append("}");
        }
        return sb.append("]}").toString();
    }

    private String addCustomerFollow(JsonNode args, Long tenantId, LoginUser loginUser) {
        Long customerId = null;
        if (args.has("customerId") && !args.path("customerId").isNull()) {
            customerId = args.path("customerId").asLong();
        }
        if (customerId == null && has(args, "customerName")) {
            CustomerInfo found = customerInfoManager.getOne(new LambdaQueryWrapper<CustomerInfo>()
                    .eq(CustomerInfo::getTenantId, tenantId)
                    .like(CustomerInfo::getCustomerName, args.path("customerName").asText())
                    .last("LIMIT 1"));
            if (found != null) customerId = found.getId();
        }
        if (customerId == null) return "{\"success\":false,\"error\":\"未找到客户\"}";

        CustomerLog clog = new CustomerLog();
        clog.setCustomerId(customerId);
        clog.setLogType(2);
        clog.setContent(args.path("content").asText());
        clog.setFollowType(args.path("followMethod").asText(null));
        clog.setTenantId(tenantId);
        clog.setCreateBy(loginUser.getUsername());
        clog.setCreateByName(loginUser.getRealName());
        customerLogManager.save(clog);
        return "{\"success\":true,\"message\":\"客户跟进记录已添加\"}";
    }

    private String quickCreateOrder(JsonNode args, Long tenantId, LoginUser loginUser) {
        QuickOrderReq req = new QuickOrderReq();
        req.setCompanyName(args.path("companyName").asText());
        req.setContactName(args.path("contactName").asText(null));
        req.setPhone(args.path("phone").asText(null));
        req.setTotalAmount(new BigDecimal(args.path("totalAmount").asText("0")));
        req.setFinalAmount(req.getTotalAmount());
        req.setStatus(args.path("status").asText("PENDING"));
        req.setPaymentType(args.path("paymentType").asText(null));
        req.setRemark(args.path("remark").asText(null));

        String productName = args.path("productName").asText(null);
        if (StringUtils.isNotBlank(productName)) {
            OrderDetailInfoReq detail = new OrderDetailInfoReq();
            detail.setProductName(productName);
            detail.setAmount(req.getTotalAmount());
            detail.setQuantity(1);
            detail.setUnitPrice(req.getTotalAmount());
            req.setOrderDetails(List.of(detail));
        }
        var result = quickOrderService.quickCreate(req, tenantId, loginUser);
        return "{\"success\":true,\"orderId\":" + result.getOrderId()
                + ",\"message\":\"订单创建成功，ID: " + result.getOrderId() + "\"}";
    }

    private boolean has(JsonNode args, String field) {
        return args.has(field) && !args.path(field).isNull() && !args.path(field).asText("").isEmpty();
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "'");
    }
}
