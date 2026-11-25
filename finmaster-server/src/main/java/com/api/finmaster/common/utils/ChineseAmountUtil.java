package com.qykj.finmaster.common.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额转中文大写工具。
 * 用于合同模板 ${amountC} 渲染。
 */
public final class ChineseAmountUtil {

    private static final char[] DIGITS = {'零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖'};
    private static final String[] INTEGER_UNITS = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿", "拾", "佰", "仟", "万"};
    private static final String[] DECIMAL_UNITS = {"角", "分"};

    private ChineseAmountUtil() {
    }

    public static String toChinese(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = value.signum() < 0;
        if (negative) {
            value = value.abs();
        }
        long integerPart = value.longValue();
        int decimalPart = value.subtract(BigDecimal.valueOf(integerPart)).movePointRight(2).intValue();

        StringBuilder sb = new StringBuilder();
        if (negative) {
            sb.append("负");
        }
        sb.append(integerToChinese(integerPart));
        sb.append("元");
        if (decimalPart == 0) {
            sb.append("整");
        } else {
            int jiao = decimalPart / 10;
            int fen = decimalPart % 10;
            if (jiao == 0) {
                sb.append("零").append(DIGITS[fen]).append(DECIMAL_UNITS[1]);
            } else {
                sb.append(DIGITS[jiao]).append(DECIMAL_UNITS[0]);
                if (fen != 0) {
                    sb.append(DIGITS[fen]).append(DECIMAL_UNITS[1]);
                }
            }
        }
        return sb.toString();
    }

    private static String integerToChinese(long n) {
        if (n == 0) {
            return "零";
        }
        String digits = Long.toString(n);
        int len = digits.length();
        StringBuilder sb = new StringBuilder();
        boolean lastZero = false;
        for (int i = 0; i < len; i++) {
            int d = digits.charAt(i) - '0';
            int unitIdx = len - 1 - i;
            if (d == 0) {
                lastZero = true;
                if (unitIdx == 4 || unitIdx == 8) {
                    // 在万、亿位上必须保留单位（避免"一万亿"塌陷）
                    trimTrailingZero(sb);
                    sb.append(INTEGER_UNITS[unitIdx]);
                    lastZero = false;
                }
            } else {
                if (lastZero) {
                    sb.append('零');
                    lastZero = false;
                }
                sb.append(DIGITS[d]).append(INTEGER_UNITS[unitIdx]);
            }
        }
        trimTrailingZero(sb);
        // 去重相邻"亿万"等异常组合（如 100000000 → 壹亿万 → 壹亿）
        String s = sb.toString().replace("亿万", "亿");
        return s;
    }

    private static void trimTrailingZero(StringBuilder sb) {
        while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '零') {
            sb.deleteCharAt(sb.length() - 1);
        }
    }
}
