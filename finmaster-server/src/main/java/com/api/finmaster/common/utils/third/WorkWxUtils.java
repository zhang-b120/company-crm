package com.qykj.finmaster.common.utils.third;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.common.utils.WebUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-10-15
 * @Description
 */
public class WorkWxUtils {

    // Token缓存对象
    private static class TokenCache {
        private static String accessToken;
        private static long expireTime;

        //刷新token
        public static synchronized void updateToken(String token, int expiresIn) {
            accessToken = token;
            // 提前5分钟过期，避免临界点问题
            expireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;
        }

        // 获取token
        public static synchronized String getToken() {
            if (accessToken == null || System.currentTimeMillis() > expireTime) {
                return null;
            }
            return accessToken;
        }

        // 清理token
        public static synchronized void clearToken() {
            accessToken = null;
            expireTime = 0L;
        }

        // 获取剩余有效时间（秒）
        public static synchronized long getRemainingTime() {
            if (accessToken == null) {
                return 0;
            }
            long remaining = (expireTime - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }
    }

    /**
     * 获取token
     * @return
     */
    public static String getAccessToken(){
        // 先从缓存获取
        String cachedToken = TokenCache.getToken();
        if (cachedToken != null) {
            return cachedToken;
        }
        String url = MessageFormat.format(WxConstants.GET_WORK_WX_ACCESS_TOKEN_URL, WxConstants.WORK_WX_CORP_ID, WxConstants.WORK_WX_MR_SECRET);
        try {
            String result = WebUtils.doGet(url, null);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.getInteger("errcode") != GlobalConstant.Number.ZERO_INT){
                System.out.println(jsonObject.getString("errmsg"));
                return null;
            }
            String accessToken = jsonObject.getString("access_token");
            int expiresIn = jsonObject.getInteger("expires_in");
            // 更新缓存
            TokenCache.updateToken(accessToken, expiresIn);
            return accessToken;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 获取群聊列表
     * @param limit
     * @param cursor
     * @return
     */
    public static JSONObject getGroupChat(Integer limit, String cursor, String accessToken){
        if(StringUtils.isEmpty(accessToken)){
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("limit", limit);
        map.put("cursor", cursor);
        String url = MessageFormat.format(WxConstants.GET_WORK_WX_GROUP_CHAT_URL, accessToken);
        try {
            String result = WebUtils.doPostReString(url, map);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.getInteger("errcode") != GlobalConstant.Number.ZERO_INT){
                System.out.println(jsonObject.getString("errmsg"));
                return null;
            }
            return jsonObject;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 获取群聊详情
     * @param chatId
     * @param needName
     * @return
     */
    public static JSONObject getGroupChatDetail(String chatId, Integer needName, String accessToken){
        if(StringUtils.isEmpty(accessToken)){
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("chat_id", chatId);
        map.put("need_name", needName);
        String url = MessageFormat.format(WxConstants.GET_WORK_WX_GROUP_CHAT_DETAIL_URL, accessToken);
        try {
            String result = WebUtils.doPostReString(url, map);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.getInteger("errcode") != GlobalConstant.Number.ZERO_INT){
                System.out.println(jsonObject.getString("errmsg"));
                return null;
            }
            return jsonObject;
        }catch (Exception e){
            return null;
        }
    }

    /**
     * 未开发完，接口不适用
     * @param type
     * @param message
     * @param accessToken
     * @return
     */
    @Deprecated
    public Boolean sendMessage(String type, String message, String accessToken){
        if(StringUtils.isEmpty(accessToken)){
            return Boolean.FALSE;
        }
        Map<String, Object> map = new HashMap<>();
        String url = MessageFormat.format(WxConstants.GET_WORK_WX_APPCHAT_SEND_URL, accessToken);
        try {
            String result = WebUtils.doPostReString(url, map);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.getInteger("errcode") != GlobalConstant.Number.ZERO_INT){
                System.out.println(jsonObject.getString("errmsg"));
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }catch (Exception e){
            return Boolean.FALSE;
        }
    }

    /**
     * 上传素材 有效期三天
     * @param type
     * @param accessToken
     * @return
     */
    public static String mediaUpload(String filePath, String type, String accessToken){
        if(StringUtils.isEmpty(accessToken)){
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        String url = MessageFormat.format(WxConstants.GET_WORK_WX_MEDIA_UPLOAD_URL, accessToken, type);
        try {
            File file = new File(filePath);

            String result = WebUtils.uploadFile(url, file, "media");
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.getInteger("errcode") != GlobalConstant.Number.ZERO_INT){
                System.out.println(jsonObject.getString("errmsg"));
                return null;
            }
            return jsonObject.getString("media_id");
        }catch (Exception e){
            return null;
        }
    }

    public static void main(String[] args){
        String accessToken = getAccessToken();
        System.out.println(mediaUpload("C:\\Users\\zhb\\Desktop\\734d762f-e784-4556-9bf8-4c4ce7e3ce65.png","image",accessToken));

        /*
        Map<String, String> map = new HashMap<>();
        JSONObject groupChat = getGroupChat(1000, null, accessToken);
        System.out.println("1111111111"+groupChat);*/
        /*if(ObjectUtils.isNotEmpty(groupChat)){
            String cursor = groupChat.getString("next_cursor");
            JSONArray jsonArray = groupChat.getJSONArray("group_chat_list");
            List<String> chatIdList = jsonArray.stream()
                    .filter(f -> ((JSONObject) f).getInteger("status") == GlobalConstant.Number.ZERO_INT)
                    .map(item -> ((JSONObject) item).getString("chat_id")).toList();
            if(CollUtil.isNotEmpty(chatIdList)){
                chatIdList.forEach(item -> {
                    JSONObject groupChatDetail = getGroupChatDetail(item, GlobalConstant.Number.ZERO_INT, accessToken);
                    JSONObject groupChat1 = groupChatDetail.getJSONObject("group_chat");
                    System.out.println(groupChatDetail);
                    if(ObjectUtils.isNotEmpty(groupChatDetail)){
                        map.put(groupChat1.getString("chat_id"), groupChat1.getString("name"));
                    }
                });
            }
        }
        System.out.println("``````````````````````"+map);*/
    }
}