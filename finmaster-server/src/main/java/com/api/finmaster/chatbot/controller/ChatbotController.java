package com.qykj.finmaster.chatbot.controller;

import com.qykj.finmaster.chatbot.service.ChatbotService;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController extends BaseController {

    @Autowired
    private ChatbotService chatbotService;

    /** 创建新会话 */
    @PostMapping("/conversation")
    public AjaxResult createConversation() {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        Long conversationId = chatbotService.createConversation(tenantId, loginUser);
        return AjaxResult.success(conversationId);
    }

    /** 查询会话列表 */
    @GetMapping("/conversations")
    public AjaxResult listConversations() {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return AjaxResult.success(chatbotService.listConversations(tenantId, loginUser.getUsername()));
    }

    /** 删除会话 */
    @DeleteMapping("/conversation/{id}")
    public AjaxResult deleteConversation(@PathVariable Long id) {
        chatbotService.deleteConversation(id);
        return AjaxResult.success();
    }

    /** 查询会话消息历史 */
    @GetMapping("/messages/{conversationId}")
    public AjaxResult listMessages(@PathVariable Long conversationId) {
        Long tenantId = getTenantId();
        return AjaxResult.success(chatbotService.listMessages(conversationId, tenantId));
    }

    /** 发送消息 */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody ChatRequest req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        try {
            String reply = chatbotService.chat(req.getConversationId(), req.getMessage(), tenantId, loginUser);
            return AjaxResult.success("ok", reply);
        } catch (Exception e) {
            return AjaxResult.error("AI对话失败：" + e.getMessage());
        }
    }

    @Data
    static class ChatRequest {
        private Long conversationId;
        private String message;
    }
}
