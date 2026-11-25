package com.qykj.finmaster.analytics.common.enums;

/**
 * 同环比模式
 * NONE: 不计算对比
 * WOW:  周环比 - 对比上一周期同长度区间
 * MOM:  月同比 - 对比上个月同期
 * YOY:  年同比 - 对比去年同期
 */
public enum CompareMode {
    NONE,
    WOW,
    MOM,
    YOY;

    public static CompareMode fromString(String v) {
        if (v == null || v.isEmpty()) {
            return NONE;
        }
        try {
            return CompareMode.valueOf(v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
