package com.qykj.finmaster.third.okcc.entity.req;

import com.qykj.finmaster.common.utils.MD5Utils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 外呼系统请求参数
 */
@Data
public class OutPhoneReq {
    private Map<String,String>  authentication = new HashMap<>();
    /**
     * 请求信息
     * 根据不同的接口自行组装map数据
     */
    private Map<String,Object> request;


    public OutPhoneReq(String customer,String outPassword,String seatNumber,String timestamp) {
        this.authentication.put("customer", customer);
        this.authentication.put("timestamp",timestamp);
        this.authentication.put("seq",seatNumber);
        try {
            this.authentication.put("digest",
                    MD5Utils.md5(customer
                                + "@" + timestamp
                                + "@" + seatNumber
                                + "@" + outPassword, "")
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.request = new HashMap<>();
    }
}
