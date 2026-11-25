package com.qykj.finmaster.common.utils.copy;

import cn.hutool.core.bean.BeanUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Slf4j
public class CopierCopyUtils {

	/**
	 * 对象转换
	 *
	 * @param source 源对象
	 * @param targetClass 目标类
	 * @return 目标对象
	 */
	public static <T> T convertTo(Object source, Class<T> targetClass) {
		if (source == null) {
			return null;
		}
		return BeanUtil.toBean(source, targetClass);
	}

	/**
	 * 对象转换（带自定义转换器）
	 *
	 * @param source 源对象
	 * @param targetClass 目标类
	 * @param converter 转换器
	 * @return 目标对象
	 */
	public static <T> T convertTo(Object source, Class<T> targetClass, Function<T, T> converter) {
		T target = convertTo(source, targetClass);
		if (target != null && converter != null) {
			return converter.apply(target);
		}
		return target;
	}

	/**
	 * 集合对象转换
	 *
	 * @param sourceList 源对象集合
	 * @param targetClass 目标类
	 * @return 目标对象集合
	 */
	public static <T> List<T> convertTo(List<?> sourceList, Class<T> targetClass) {
		if (CollectionUtils.isEmpty(sourceList)) {
			return Collections.emptyList();
		}
		return sourceList.stream()
				.map(source -> convertTo(source, targetClass))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * 集合对象转换（带自定义转换器）
	 *
	 * @param sourceList 源对象集合
	 * @param targetClass 目标类
	 * @param converter 转换器
	 * @return 目标对象集合
	 */
	public static <T> List<T> convertTo(List<?> sourceList, Class<T> targetClass, Function<T, T> converter) {
		if (CollectionUtils.isEmpty(sourceList)) {
			return Collections.emptyList();
		}
		return sourceList.stream()
				.map(source -> convertTo(source, targetClass, converter))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	/**
	 * 对象属性拷贝
	 *
	 * @param source 源对象
	 * @param target 目标对象
	 */
	public static void copyProperties(Object source, Object target) {
		if (source == null || target == null) {
			return;
		}
		BeanUtil.copyProperties(source, target);
	}

	/**
	 * 对象属性拷贝（忽略空值）
	 *
	 * @param source 源对象
	 * @param target 目标对象
	 */
	public static void copyPropertiesIgnoreNull(Object source, Object target) {
		if (source == null || target == null) {
			return;
		}
		BeanUtil.copyProperties(source, target, true);
	}

	/**
	 * 比较两个对象的差异并返回差异字符串
	 * @param oldObj 旧对象
	 * @param newObj 新对象
	 * @return 差异字符串，格式：字段名: 旧值 -> 新值
	 */
	public static String diffChange(Object oldObj, Object newObj) {
		if (oldObj == null || newObj == null) {
			return "";
		}

		List<String> diffList = new ArrayList<>();
		Field[] fields = oldObj.getClass().getDeclaredFields();

		for (Field field : fields) {
			field.setAccessible(true);
			try {
				Object oldValue = field.get(oldObj);
				Object newValue = field.get(newObj);

				// 如果两个值不相等，则记录差异
				if (!equals(oldValue, newValue)) {
					String fieldName = field.getName();
					String oldValueStr = formatValue(oldValue);
					String newValueStr = formatValue(newValue);
					diffList.add(String.format("%s: %s -> %s", fieldName, oldValueStr, newValueStr));
				}
			} catch (IllegalAccessException e) {
				// 忽略访问异常
			}
		}

		return String.join("; ", diffList);
	}

	/**
	 * 格式化值
	 * @param value 值
	 * @return 格式化后的字符串
	 */
	private static String formatValue(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Boolean) {
			return (Boolean) value ? "是" : "否";
		}
		return value.toString();
	}

	/**
	 * 比较两个值是否相等
	 * @param value1 值1
	 * @param value2 值2
	 * @return 是否相等
	 */
	private static boolean equals(Object value1, Object value2) {
		if (value1 == value2) {
			return true;
		}
		if (value1 == null || value2 == null) {
			return false;
		}
		return value1.equals(value2);
	}
}
