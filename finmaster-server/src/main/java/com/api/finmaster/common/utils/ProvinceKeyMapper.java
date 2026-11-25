package com.qykj.finmaster.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 省份 / 直辖市 / 计划单列市：中文名 → 财务系统所需的拼音 key
 * 来源：财务系统 AccountSets.province 字段字典
 */
public class ProvinceKeyMapper {

    private static final Map<String, String> NAME_TO_KEY = new HashMap<>();

    static {
        // 直辖市
        put("北京", "beijing");
        put("上海", "shanghai");
        put("天津", "tianjin");
        put("重庆", "chongqing");
        // 省份
        put("安徽", "anhui");
        put("福建", "fujian");
        put("甘肃", "gansu");
        put("广东", "guangdong");
        put("广西", "guangxi");
        put("贵州", "guizhou");
        put("海南", "hainan");
        put("河北", "hebei");
        put("黑龙江", "heilongjiang");
        put("河南", "henan");
        put("湖北", "hubei");
        put("湖南", "hunan");
        put("江苏", "jiangsu");
        put("江西", "jiangxi");
        put("吉林", "jilin");
        put("辽宁", "liaoning");
        put("内蒙古", "neimenggu");
        put("宁夏", "ningxia");
        put("青海", "qinghai");
        put("山东", "shandong");
        put("山西", "shanxi");
        put("陕西", "shaanxi");
        put("四川", "sichuan");
        put("新疆", "xinjiang");
        put("西藏", "xizang");
        put("云南", "yunnan");
        put("浙江", "zhejiang");
        // 计划单列市
        put("大连", "dalian");
        put("青岛", "qingdao");
        put("深圳", "shenzhen");
        put("厦门", "xiamen");
        put("宁波", "ningbo");
    }

    private static void put(String cn, String key) {
        NAME_TO_KEY.put(cn, key);
        // 兼容带"省 / 市 / 自治区"后缀的中文名
        NAME_TO_KEY.put(cn + "省", key);
        NAME_TO_KEY.put(cn + "市", key);
        NAME_TO_KEY.put(cn + "自治区", key);
    }

    /**
     * 中文 → 拼音 key。匹配不到返回 null（调用方需自行判断是否要传）。
     * 已经是拼音 key（小写英文）的也原样放行。
     */
    public static String toKey(String chineseOrKey) {
        if (StringUtils.isBlank(chineseOrKey)) return null;
        String s = chineseOrKey.trim();
        // 已经是拼音 key 直接返回
        if (s.matches("^[a-z]+$") && NAME_TO_KEY.containsValue(s)) {
            return s;
        }
        return NAME_TO_KEY.get(s);
    }
}
