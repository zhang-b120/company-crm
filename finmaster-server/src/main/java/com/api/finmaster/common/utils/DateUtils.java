package com.qykj.finmaster.common.utils;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
public class DateUtils {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String YYYYMMDD = "yyyyMMdd";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";
    public static final String RFC3339_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd'T'HH:mm:ss+08:00";

    /**
     * 获取SimpleDateFormat
     * @Author zhb
     * @Date 2025-06-17
     * @param pattern
     * @return
     */
    private static SimpleDateFormat getDateFormat(String pattern) throws RuntimeException {
        return new SimpleDateFormat(pattern,Locale.CHINA);
    }

    /**
     * RFC3339S 格式
     */
    private static DateTimeFormatter getRFC3339SFormat(String pattern){
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of("Asia/Shanghai"));
    }

    /**
     * 字符串转化为日期
     * @param date
     * @param pattern
     * @return 日期
     */
    public static Date stringToDate(String date, String pattern) {
        Date myDate = null;
        if (date != null) {
            try {
                myDate = getDateFormat(pattern).parse(date);
            } catch (Exception e) {
            }
        }
        return myDate;
    }

    /**
     * 日期转化为字符串
     * @param date
     * @param pattern
     * @return
     */
    public static String dateToString(Date date, String pattern) {
        String dateString = null;
        if (date != null) {
            try {
                dateString = getDateFormat(pattern).format(date);
            } catch (Exception e) {
            }
        }
        return dateString;
    }

    /**
     * LocalDateTime格式化为string
     * @param localDateTime
     * @param pattern
     * @return
     */
    public static String localDateTimeToString(LocalDateTime localDateTime,String pattern) {
        if (null == localDateTime || null==pattern) {
            return null;
        }
        return  DateTimeFormatter.ofPattern(pattern).format(localDateTime);
    }

    /**
     * LocalDate格式化为string
     * @param localDate
     * @param pattern
     * @return
     */
    public static String localDateToString(LocalDate localDate,String pattern) {
        if (null == localDate || null==pattern) {
            return null;
        }
        return  DateTimeFormatter.ofPattern(pattern).format(localDate);
    }

    /**
     * 字符串转成LocalDate
     * @param time
     * @return
     */
    public static LocalDate timeStrToLocalDate(String time, String pattern) {
        return LocalDate.parse(time, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 字符串转成LocalDateTime
     * @param str
     * @return
     */
    public static LocalDateTime stringToLocalDateTime(String str, String pattern) {
        if(StringUtils.isBlank(str)){
            return null;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.parse(str, df);
    }
    /**
     * 字符串转成LocalDate
     * @param str
     * @return
     */
    public static LocalDate stringToLocalDate(String str, String pattern) {
        if(StringUtils.isBlank(str)){
            return null;
        }
        DateTimeFormatter df = DateTimeFormatter.ofPattern(pattern);
        return LocalDate.parse(str, df);
    }

    /**
     * localDateTime转成Date
     * @param localDateTime
     * @return
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        if(null == localDateTime){
            return null;
        }
        return Date.from(localDateTime.atZone( ZoneId.systemDefault()).toInstant());
    }

    /**
     * localDate转成Date
     * @param localDate
     * @return
     */
    public static Date localDateToDate(LocalDate localDate) {
        if(null == localDate){
            return null;
        }
        return Date.from(localDate.atStartOfDay( ZoneId.systemDefault()).toInstant());
    }

    /**
     * date转成LocalDateTime
     * @param date
     * @return
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        if(null == date){
            return null;
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }


    /**
     * 时间戳转 LocalDateTime
     * @param timestamp 毫秒时间戳
     * @return
     */
    public static LocalDateTime timestampToLocalDateTime(Long timestamp) {
        if (null == timestamp) {
            return null;
        }
        if (timestamp.toString().length() == 10) {
            return timestampSecondToLocalDateTime(timestamp);
        }
        return Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneOffset.ofHours(8))
                .toLocalDateTime().withNano(0);
    }

    public static LocalDateTime timestampSecondToLocalDateTime(Long timestamp) {
        if (null == timestamp) {
            return null;
        }
        if (timestamp.toString().length() == 13) {
            return timestampToLocalDateTime(timestamp);
        }
        return Instant
                .ofEpochSecond(timestamp)
                .atZone(ZoneOffset.ofHours(8))
                .toLocalDateTime().withNano(0);
    }

    /**
     * 时间戳转 LocalDate
     * @param timestamp 毫秒时间戳
     * @return
     */
    public static LocalDate timestampToLocalDate(Long timestamp) {
        if (null == timestamp) {
            return null;
        }
        if (timestamp.toString().length() == 10) {
            return timestampSecondToLocalDate(timestamp);
        }
        return Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneOffset.ofHours(8))
                .toLocalDate();
    }

    public static LocalDate timestampSecondToLocalDate(Long timestamp) {
        if (null == timestamp) {
            return null;
        }
        if (timestamp.toString().length() == 13) {
            return timestampToLocalDate(timestamp);
        }
        return Instant
                .ofEpochSecond(timestamp)
                .atZone(ZoneOffset.ofHours(8))
                .toLocalDate();
    }


    /**
     * LocalDateTime 转时间戳
     * @param localDateTime
     * @return
     */
    public static Long localDateTimeToTimestamp(LocalDateTime localDateTime) {
        if (null == localDateTime) {
            return null;
        }
        return localDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
    }

    /**
     * LocalDate 转时间戳
     * @param localDate
     * @return
     */
    public static Long localDateToTimestamp(LocalDate localDate) {
        if (null == localDate) {
            return null;
        }
        return localDate.atStartOfDay(ZoneOffset.ofHours(8)).toInstant().toEpochMilli();
    }

    /**
     * 获取Unix时间戳（秒级）
     * @return
     */
    public static Long getCurrentUnixTimestamp() {
        return Instant.now().getEpochSecond();
    }

    /**
     * 获取Unix时间戳（秒级）
     * @return
     */
    public static String getCurrentUnixTimestampStr() {
        return String.valueOf(getCurrentUnixTimestamp());
    }

    /**
     * LocalDateTime加上分钟数
     * @param localDateTime
     * @param add
     * @return
     */
    public static LocalDateTime localDateTimeAddMinutes(LocalDateTime localDateTime, Long add) {
        if (null == localDateTime) {
            return null;
        }
        return localDateTime.plusMinutes(add);
    }

    /**
     * LocalDateTime转成RFC3339
     * @param localDateTime
     * @return
     */
    public static String toRFC3339Str(LocalDateTime localDateTime, String pattern) {
        if(Objects.isNull(localDateTime)){
            return "";
        }
        return getRFC3339SFormat(pattern).format(localDateTime);
    }

    /**
     * rfc3339转换为LocalDateTime
     * @param rfc3339DateTime
     * @param pattern
     * @return
     */
    public static LocalDateTime parseRfc3339(String rfc3339DateTime, String pattern) {
        if(StringUtils.isEmpty(rfc3339DateTime)){
            return null;
        }
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(rfc3339DateTime);
        LocalDateTime localDateTime = offsetDateTime.toLocalDateTime();
        return stringToLocalDateTime(localDateTime.format(DateTimeFormatter.ofPattern(pattern)), YYYY_MM_DD_HH_MM_SS);
    }

    /**
     * 获取之前某天某时刻
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public static LocalDateTime getBeforeLocalDateTime(Integer day, Integer hour, Integer minute, Integer second) {
        return LocalDate.now()
                .minusDays(day)
                .atTime(hour, minute, second);
    }

    /**
     * 获取之前某天某时刻
     * @param day
     * @param hour
     * @param minute
     * @param second
     * @return
     */
    public static Date getBeforeDateWithTime(Integer day, Integer hour, Integer minute, Integer second) {
        return localDateTimeToDate(getBeforeLocalDateTime(day, hour, minute, second));
    }

    /**
     * 计算两个时间月份差值 1.只计算年月（忽略天） 2.根据结束日期判断，如果日期大于15则差值+1
     * @param start
     * @param end
     * @return
     */
    public static Integer localDateIntervalMonth(LocalDate start, LocalDate end, Boolean flag){
        if(start == null || end == null){
            return null;
        }
        // 1. 计算基础年月差（忽略天数）
        YearMonth ymStart = YearMonth.from(start);
        YearMonth ymEnd = YearMonth.from(end);
        long baseMonths = ChronoUnit.MONTHS.between(ymStart, ymEnd);

        // 2. 如果结束日期大于15号，则月份差+1
        if (flag && end.getDayOfMonth() > 15) {
            baseMonths++;
        }

        return (int) baseMonths;
    }

    /**
     * 获取几天前的时间
     * @param ago
     * @return
     */
    public static LocalDate fewDaysAgoLocalDate(Integer ago){
        return LocalDate.now().minusDays(ago);
    }

    /**
     * 获取几天后的时间
     * @param after
     * @return
     */
    public static LocalDate fewDaysAfterLocalDate(Integer after){
        return LocalDate.now().plusDays(after);
    }

    /**
     * 获取几天前起始时间
     * @return
     */
    public static LocalDateTime fewDaysAgoLocalDateTimeStart(Integer ago){
        LocalDate localDate = fewDaysAgoLocalDate(ago);
        return localDate.atStartOfDay();
    }

    /**
     * 获取几天前结束时间
     * @return
     */
    public static LocalDateTime fewDaysAgoLocalDateTimeEnd(Integer ago){
        LocalDate localDate = fewDaysAgoLocalDate(ago);
        return localDate.atTime(23,59,59, 999);
    }

    /**
     * 获取几天前起始时间
     * @return
     */
    public static Date fewDaysAgoDateStart(Integer ago){
        return localDateTimeToDate(fewDaysAgoLocalDateTimeStart(ago));
    }

    /**
     * 获取几天前结束时间
     * @return
     */
    public static Date fewDaysAgoDateEnd(Integer ago){
        return localDateTimeToDate(fewDaysAgoLocalDateTimeEnd(ago));
    }

    /**
     * 获取几天后起始时间
     * @return
     */
    public static LocalDateTime fewDaysAfterLocalDateTimeStart(Integer after){
        LocalDate localDate = fewDaysAfterLocalDate(after);
        return localDate.atStartOfDay();
    }

    /**
     * 获取几天后结束时间
     * @return
     */
    public static LocalDateTime fewDaysAfterLocalDateTimeEen(Integer after){
        LocalDate localDate = fewDaysAfterLocalDate(after);
        return localDate.atTime(23,59,59, 999);
    }

    /**
     * 获取本月第一天
     */
    public static LocalDate getFirstDayOfCurrentMonth() {
        return LocalDate.now().withDayOfMonth(1);
    }

    /**
     * 获取下个月第一天
     */
    public static LocalDate getFirstDayOfNextMonth() {
        return getFirstDayOfCurrentMonth().plusMonths(1);
    }

    /**
     * 获取上个月第一天
     */
    public static LocalDate getFirstDayOfPreviousMonth() {
        return getFirstDayOfCurrentMonth().minusMonths(1);
    }

    /**
     * 获取季度第一天
     */
    public static LocalDate getFirstDayOfQuarter() {
        LocalDate current = LocalDate.now();
        int month = current.getMonthValue();
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
        return LocalDate.of(current.getYear(), quarterStartMonth, 1);
    }

    /**
     * 获取年度第一天
     */
    public static LocalDate getFirstDayOfYear() {
        LocalDate current = LocalDate.now();
        return LocalDate.of(current.getYear(), 1, 1);
    }

    /**
     * 获取本月第一天 00:00:00
     * @return
     */
    public static LocalDateTime getFirstDayOfMonthWithTime() {
        return getFirstDayOfCurrentMonth().atStartOfDay();
    }

    /**
     * 获取本月最后一天
     */
    public static LocalDate getLastDayOfCurrentMonth() {
        return YearMonth.from(LocalDate.now()).atEndOfMonth();
    }

    /**
     * 获取下个月最后一天
     */
    public static LocalDate getLastDayOfNextMonth() {
        return YearMonth.from(LocalDate.now().plusMonths(1)).atEndOfMonth();
    }

    /**
     * 获取上个月最后一天
     */
    public static LocalDate getLastDayOfPreviousMonth() {
        return YearMonth.from(LocalDate.now().minusMonths(1)).atEndOfMonth();
    }

    /**
     * 获取季度最后一天
     */
    public static LocalDate getLastDayOfQuarter() {
        LocalDate current = LocalDate.now();
        int month = current.getMonthValue();
        int quarterEndMonth = ((month - 1) / 3 + 1) * 3;
        YearMonth quarterEnd = YearMonth.of(current.getYear(), quarterEndMonth);
        return quarterEnd.atEndOfMonth();
    }

    /**
     * 获取年度最后一天
     */
    public static LocalDate getLastDayOfYear() {
        LocalDate current = LocalDate.now();
        return YearMonth.of(current.getYear(), 12).atEndOfMonth();
    }

    /**
     * 获取本月最后一天 23:59:59 999
     * @return
     */
    public static LocalDateTime getLastDayOfMonthWithTime() {
        return getLastDayOfCurrentMonth().atTime(23,59,59, 999);
    }

    /**
     * 计算两个时间相差天数
     * @param t1 开始的时间
     * @param t2 结束的时间
     * @return 相差的天数（绝对值）
     */
    public static Long getDaysDifference(LocalDateTime t1,LocalDateTime t2){
        if(Objects.isNull(t1) || Objects.isNull(t2)){
            return GlobalConstant.Number.ZERO_LONG;
        }
        Duration duration = Duration.between(t1,t2);
        //相差的天数
        return Math.abs(duration.toDays());
    }

    /**
     * 计算两个日期之间的天数差
     * @param d1
     * @param d2
     * @return 相差的天数（绝对值）
     */
    public static long getDaysDifference(LocalDate d1, LocalDate d2) {
        if(Objects.isNull(d1) || Objects.isNull(d2)){
            return GlobalConstant.Number.ZERO_LONG;
        }

        // 计算天数差（绝对值）
        return Math.abs(ChronoUnit.DAYS.between(d1, d2));
    }

    /**
     * 是否超时
     * @param targetDate
     * @param seconds
     * @return
     */
    public static boolean isTimeoutOfSeconds(Date targetDate, long seconds) {
        if (targetDate == null) {
            return false;
        }

        Instant targetInstant = targetDate.toInstant();
        Instant nowInstant = Instant.now();

        // 计算时间差
        Duration duration = Duration.between(targetInstant, nowInstant);

        return duration.getSeconds() > seconds;
    }
}