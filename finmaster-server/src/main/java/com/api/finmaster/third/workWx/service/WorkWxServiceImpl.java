package com.qykj.finmaster.third.workWx.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.util.concurrent.RateLimiter;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.third.WorkWxUtils;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.manager.impl.WorkWxGroupChatManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-10-16
 * @Description
 */
@Service
public class WorkWxServiceImpl {

    @Autowired
    private WorkWxGroupChatManagerImpl workWxGroupChatManager;

    // API限流器，严格控制在30次/秒以内
    private final RateLimiter rateLimiter = RateLimiter.create(29.0);

    /**
     * 保存群聊数据
     * @param limit
     * @return
     */
    public Boolean saveGroupChatData(Integer limit, Boolean isFullSync){
        return saveGroupChatData(WorkWxUtils.getAccessToken(), limit, null, isFullSync);
    }

    /**
     * 保存群聊数据
     * @param accessToken
     * @param limit
     * @param cursor
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveGroupChatData(String accessToken, Integer limit, String cursor, Boolean isFullSync) {
        String nextCursor = null;
        JSONObject groupChatJSONObject = WorkWxUtils.getGroupChat(limit, cursor, accessToken);
        if (ObjectUtils.isNotEmpty(groupChatJSONObject)) {
            nextCursor = groupChatJSONObject.getString("next_cursor");
            JSONArray groupChatArray = groupChatJSONObject.getJSONArray("group_chat_list");
            List<WorkWxGroupChat> workWxGroupChatList = new ArrayList<>();
            for (int i = 0; i < groupChatArray.size(); i++) {
                JSONObject groupChat = groupChatArray.getJSONObject(i);
                String chatId = groupChat.getString("chat_id");
                // 顺序检查chatId，单独调用sql查询，防止程序执行过快，企业微信API调用存在并发限制，如果系统中存在chatId,
                // 全量同步，跳过本次循环
                // 非全量同步，立刻跳出循环 并将分页查询游标nextCursor置为空
                if (checkChatId(chatId)) {
                    if(isFullSync){
                        continue;
                    }else {
                        nextCursor = null;
                        break;
                    }
                }
                // 对需要处理的群聊获取详情
                // 企业微信获取群详情API限制并发30 暂不使用异步 修改为在获取群详情时进行限流操作
                WorkWxGroupChat workWxGroupChat = createWorkWxGroupChat(groupChat, accessToken);
                if (ObjectUtils.isNotEmpty(workWxGroupChat)) {
                    workWxGroupChatList.add(workWxGroupChat);
                }
                /*CompletableFuture<WorkWxGroupChat> future = CompletableFuture.supplyAsync(
                        () -> createWorkWxGroupChat(groupChat, accessToken)
                );
                futures.add(future);*/
            }
            // 批量保存
            if (!workWxGroupChatList.isEmpty()) {
                workWxGroupChatManager.saveBatch(workWxGroupChatList);
            }
        }
        // 存在分页游标，进行递归
        if (StringUtils.isNotEmpty(nextCursor)) {
            saveGroupChatData(accessToken, limit, nextCursor, isFullSync);
        }
        return Boolean.TRUE;
    }

    /**
     * 异步获取群聊详情
     * @param groupChat
     * @param accessToken
     * @return
     */
    private WorkWxGroupChat createWorkWxGroupChat(JSONObject groupChat, String accessToken) {
        String chatId = groupChat.getString("chat_id");
        //限流控制
        rateLimiter.acquire();
        WorkWxGroupChat workWxGroupChat = new WorkWxGroupChat();
        workWxGroupChat.setChatId(chatId);
        JSONObject groupChatDetail = WorkWxUtils.getGroupChatDetail(chatId, GlobalConstant.Number.ONE_INT, accessToken);
        if (ObjectUtils.isNotEmpty(groupChatDetail)) {
            JSONObject detail = groupChatDetail.getJSONObject("group_chat");
            workWxGroupChat.setChatName(detail.getString("name"));
            workWxGroupChat.setChatOwner(detail.getString("owner"));
            workWxGroupChat.setChatDetail(detail.getString("member_list"));
            workWxGroupChat.setChatStatus(detail.getInteger("status"));
        }
        return workWxGroupChat;
    }

    /**
     * 按 chatId 刷新单个群（工单 WCB-02 客户联系回调用）：拉群详情 → saveOrUpdate（存在则更新，否则新增）。
     * 仍只读/写 work_wx_group_chat，不触碰任何业务模块。
     * @return 是否成功刷新（accessToken/详情拿不到时返回 false，不抛异常）
     */
    public Boolean refreshOneGroup(String chatId) {
        if (StringUtils.isEmpty(chatId)) {
            return Boolean.FALSE;
        }
        String accessToken = WorkWxUtils.getAccessToken();
        if (StringUtils.isEmpty(accessToken)) {
            return Boolean.FALSE;
        }
        JSONObject detailResp = WorkWxUtils.getGroupChatDetail(chatId, GlobalConstant.Number.ONE_INT, accessToken);
        if (ObjectUtils.isEmpty(detailResp)) {
            return Boolean.FALSE;
        }
        JSONObject detail = detailResp.getJSONObject("group_chat");
        if (ObjectUtils.isEmpty(detail)) {
            return Boolean.FALSE;
        }
        WorkWxGroupChat existing = workWxGroupChatManager.lambdaQuery()
                .eq(WorkWxGroupChat::getChatId, chatId)
                .one();
        WorkWxGroupChat workWxGroupChat = existing != null ? existing : new WorkWxGroupChat();
        workWxGroupChat.setChatId(chatId);
        workWxGroupChat.setChatName(detail.getString("name"));
        workWxGroupChat.setChatOwner(detail.getString("owner"));
        workWxGroupChat.setChatDetail(detail.getString("member_list"));
        workWxGroupChat.setChatStatus(detail.getInteger("status"));
        workWxGroupChatManager.saveOrUpdate(workWxGroupChat);
        return Boolean.TRUE;
    }

    /**
     * 群解散（ChangeType=dismiss）：软删除该群记录（@TableLogic deleted=1）。
     */
    public void markGroupDismissed(String chatId) {
        if (StringUtils.isEmpty(chatId)) {
            return;
        }
        workWxGroupChatManager.lambdaUpdate()
                .eq(WorkWxGroupChat::getChatId, chatId)
                .remove();
    }

    /**
     * 校验chatId
     * @param chatId
     * @return
     */
    private Boolean checkChatId(String chatId){
        LambdaQueryWrapper<WorkWxGroupChat> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(WorkWxGroupChat::getChatId, chatId);
        return workWxGroupChatManager.getBaseMapper().exists(queryWrapper);
    }
}