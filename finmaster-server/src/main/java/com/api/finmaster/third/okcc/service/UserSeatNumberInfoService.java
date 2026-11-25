package com.qykj.finmaster.third.okcc.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.utils.WebUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.third.okcc.entity.UserSeatNumberInfo;
import com.qykj.finmaster.third.okcc.entity.req.OutPhoneReq;
import com.qykj.finmaster.third.okcc.entity.resp.GetUserSeatNumberInfoResp;
import com.qykj.finmaster.third.okcc.entity.resp.OutPhoneResp;
import com.qykj.finmaster.third.okcc.mapper.UserSeatNumberInfoMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public class UserSeatNumberInfoService extends ServiceImpl<UserSeatNumberInfoMapper, UserSeatNumberInfo> {


    private final String OUT_PHONE_URL = "http://182.40.103.105:8089/openapi/V2.0.6/";

    /**
     * 根据userId 获取座席信息
     *
     * @param userId
     * @return
     */
    GetUserSeatNumberInfoResp getUserSeatNumberInfo(Long userId) {
        return CopierCopyUtils.convertTo(baseMapper.selectOne(new QueryWrapper<UserSeatNumberInfo>()
                .eq("user_id", userId)), GetUserSeatNumberInfoResp.class);
    }

    /**
     * 座席登录
     *
     * @param userId
     * @return
     */
    public OutPhoneResp agentLogin(Long userId) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);
        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"agentLogin", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }
    /**
     * 座席退出
     *
     * @param userId
     * @return
     */
    public OutPhoneResp agentLogout(Long userId) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);
        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"agentLogout", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }


    /**
     * 拨打电话
     *
     * @param userId
     * @return
     */
    public OutPhoneResp callNumber(Long userId,Long phoneNumber) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);

        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("callee",phoneNumber);
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"callNumber", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }

    /**
     * 挂断指定座席呼叫
     *
     * @param userId
     * @return
     */
    public OutPhoneResp hangupCall(Long userId) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);

        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"hangupCall", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }

    /**
     * 坐席置忙
     *
     * @param userId
     * @return
     */
    public OutPhoneResp agentSetBusy(Long userId) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);

        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"agentSetBusy", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }

    /**
     * 坐席置闲
     *
     * @param userId
     * @return
     */
    public OutPhoneResp agentSetIdle(Long userId) {
        OutPhoneResp resp = new OutPhoneResp();
        GetUserSeatNumberInfoResp userSeatNumberInfo = getUserSeatNumberInfo(userId);
        String timestamp = Long.toString(System.currentTimeMillis());
        OutPhoneReq req = new OutPhoneReq(
                userSeatNumberInfo.getOutAccount(),
                userSeatNumberInfo.getOutPassword(),
                userSeatNumberInfo.getSeatNumber(),
                timestamp);

        req.getRequest().put("seq",userSeatNumberInfo.getSeatNumber());
        req.getRequest().put("agent",userSeatNumberInfo.getSeatNumber());
        try {
            String resultStr = WebUtils.doPostReString(OUT_PHONE_URL+"agentSetIdle", JSONUtil.parseObj(req));
            JSONObject object = JSONUtil.parseObj(resultStr);

            resp.setStatus(object.get("result",Map.class).get("error").toString().equals("0") ? "success":"error");
            resp.setMsg(object.get("result",Map.class).get("msg").toString());
            resp.setData(JSONUtil.toJsonStr(object.get("data",Map.class).get("response")));
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus("error");
            resp.setMsg(e.getMessage());
        }
        return resp;
    }
}
