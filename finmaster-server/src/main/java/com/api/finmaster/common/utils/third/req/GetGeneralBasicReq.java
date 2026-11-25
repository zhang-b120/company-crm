package com.qykj.finmaster.common.utils.third.req;

import com.beust.jcommander.internal.Maps;
import lombok.Data;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.cglib.beans.BeanMap;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
@Data
public class GetGeneralBasicReq {

    /**
     * 	图像数据，base64编码后进行urlencode，要求base64编码和urlencode后大小不超过8M，最短边至少15px，最长边最大4096px，支持jpg/jpeg/png/bmp格式
     * 优先级：image > url > pdf_file > ofd_file ，当image字段存在时，url、pdf_file、ofd_file 字段失效
     */
    private String image;

    /**
     * 图片完整url，url长度不超过1024字节，url对应的图片base64编码后大小不超过8M，最短边至少15px，最长边最大4096px，支持jpg/jpeg/png/bmp格式
     * 优先级：image > url > pdf_file > ofd_file，当image字段存在时，url字段失效
     * 请注意关闭URL防盗链
     */
    private String url;

    /**
     * 	PDF文件，base64编码后进行urlencode，要求base64编码和urlencode后大小不超过8M，最短边至少15px，最长边最大4096px
     * 优先级：image > url > pdf_file > ofd_file，当image、url字段存在时，pdf_file字段失效
     */
    private String pdf_file;

    /**
     * 需要识别的PDF文件的对应页码，当 pdf_file 参数有效时，识别传入页码的对应页面内容，若不传入，则默认识别第 1 页
     */
    private String pdf_file_num;

    /**
     * 	OFD文件，base64编码后进行urlencode，要求base64编码和urlencode后大小不超过8M，最短边至少15px，最长边最大4096px
     * 优先级：image > url > pdf_file > ofd_file，当image、url、pdf_file字段存在时，ofd_file字段失效
     */
    private String ofd_file;

    /**
     * 需要识别的OFD文件的对应页码，当 ofd_file 参数有效时，识别传入页码的对应页面内容，若不传入，则默认识别第 1 页
     */
    private String ofd_file_num;

    public Map<String, Object> toBeanMap() {
        Map<String, Object> map = Maps.newHashMap();
        BeanMap.create(this).forEach((k, v) -> {
            map.put(k.toString(), v);
        });
        return map;
    }
}