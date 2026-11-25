package com.qykj.finmaster.common.utils.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * 反射工具类
 */
public class ReflectUtils {
    /**
     * 获取类的所有字段
     */
    public static List<Object[]> getFields(Class<?> clazz, Class<? extends Annotation> annotation) {
        List<Object[]> fields = new ArrayList<Object[]>();
        for (Field field : clazz.getDeclaredFields()) {
            if (annotation != null) {
                if (field.isAnnotationPresent(annotation)) {
                    fields.add(new Object[] { field, field.getAnnotation(annotation) });
                }
            } else {
                fields.add(new Object[] { field, null });
            }
        }
        return fields;
    }

    /**
     * 获取对象字段值
     */
    public static Object getFieldValue(Object obj, String fieldName) {
        try {
            // 先尝试获取当前类的字段
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            // 如果当前类没有该字段，尝试获取父类的字段
            try {
                Field field = obj.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(obj);
            } catch (Exception ex) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置对象字段值
     */
    public static void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // ignore
        }
    }
} 