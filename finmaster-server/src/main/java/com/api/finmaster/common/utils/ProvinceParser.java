package com.qykj.finmaster.common.utils;

import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * 公司注册地址 → 省份 / 税局所在地 解析（与前端 utils/province.js 规则一致）
 *
 * <ul>
 *   <li>省份：34 个省级行政区（含港澳台）短名称</li>
 *   <li>税局所在地：优先识别 5 个计划单列市（厦门/深圳/青岛/宁波/大连），否则回退到省份</li>
 * </ul>
 *
 * 设计原则：传入字符串解析。所有 customer_company 写入入口都经过 {@link #fillIfBlank}，
 * 保证规则只此一份；前端如显式传值则以前端为准（fillIfBlank 不覆盖已有值）。
 */
public final class ProvinceParser {

    private ProvinceParser() {}

    /** 34 个省级行政区：[完整名称, 短名称]，长名称排前面避免误命中（如"内蒙古自治区" 优先于 "内"） */
    private static final List<String[]> PROVINCES = List.of(
            new String[]{"新疆维吾尔自治区", "新疆"},
            new String[]{"广西壮族自治区", "广西"},
            new String[]{"宁夏回族自治区", "宁夏"},
            new String[]{"香港特别行政区", "香港"},
            new String[]{"澳门特别行政区", "澳门"},
            new String[]{"内蒙古自治区", "内蒙古"},
            new String[]{"西藏自治区", "西藏"},
            new String[]{"黑龙江省", "黑龙江"},
            new String[]{"北京市", "北京"},
            new String[]{"上海市", "上海"},
            new String[]{"天津市", "天津"},
            new String[]{"重庆市", "重庆"},
            new String[]{"河北省", "河北"},
            new String[]{"山西省", "山西"},
            new String[]{"辽宁省", "辽宁"},
            new String[]{"吉林省", "吉林"},
            new String[]{"江苏省", "江苏"},
            new String[]{"浙江省", "浙江"},
            new String[]{"安徽省", "安徽"},
            new String[]{"福建省", "福建"},
            new String[]{"江西省", "江西"},
            new String[]{"山东省", "山东"},
            new String[]{"河南省", "河南"},
            new String[]{"湖北省", "湖北"},
            new String[]{"湖南省", "湖南"},
            new String[]{"广东省", "广东"},
            new String[]{"海南省", "海南"},
            new String[]{"四川省", "四川"},
            new String[]{"贵州省", "贵州"},
            new String[]{"云南省", "云南"},
            new String[]{"陕西省", "陕西"},
            new String[]{"甘肃省", "甘肃"},
            new String[]{"青海省", "青海"},
            new String[]{"台湾省", "台湾"}
    );

    /** 5 个计划单列市：税务局独立于所在省 */
    private static final List<String[]> INDEPENDENT_CITIES = List.of(
            new String[]{"厦门市", "厦门"},
            new String[]{"深圳市", "深圳"},
            new String[]{"青岛市", "青岛"},
            new String[]{"宁波市", "宁波"},
            new String[]{"大连市", "大连"}
    );

    /**
     * 从地址解析省份短名称（仅省级行政区）
     * @return 短名称 / null
     */
    public static String parseProvince(String address) {
        if (StringUtils.isBlank(address)) return null;
        String s = address.trim();
        // 完整名称前缀匹配
        for (String[] p : PROVINCES) {
            if (s.startsWith(p[0])) return p[1];
        }
        // 短名称前缀匹配（用户可能直接写"北京朝阳区"）
        for (String[] p : PROVINCES) {
            if (s.startsWith(p[1])) return p[1];
        }
        return null;
    }

    /**
     * 从地址解析税局所在地：优先 5 计划单列市，否则回退省份
     */
    public static String parseTaxDistrict(String address) {
        if (StringUtils.isBlank(address)) return null;
        String s = address.trim();
        for (String[] c : INDEPENDENT_CITIES) {
            if (s.contains(c[0])) return c[1];
        }
        return parseProvince(s);
    }

    /**
     * 把识别值填进 customer_company，**已有值不覆盖**（保留人工选择优先级）
     * 在所有 customer_company 写入入口（add / update / 派工保存等）调用即可。
     */
    public static void fillIfBlank(CustomerCompany cc) {
        if (cc == null || StringUtils.isBlank(cc.getRegisteredAddress())) return;
        if (StringUtils.isBlank(cc.getProvince())) {
            cc.setProvince(parseProvince(cc.getRegisteredAddress()));
        }
        if (StringUtils.isBlank(cc.getTaxDistrict())) {
            cc.setTaxDistrict(parseTaxDistrict(cc.getRegisteredAddress()));
        }
    }
}
