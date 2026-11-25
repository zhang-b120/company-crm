package com.qykj.finmaster.common.utils.poi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.qykj.finmaster.common.annotation.Excel;
import com.qykj.finmaster.common.annotation.Excel.Type;
import com.qykj.finmaster.common.annotation.Excel.ColumnType;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.reflect.ReflectUtils;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Excel工具类
 */
public class ExcelUtil<T> {
    private static final Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    /**
     * Excel sheet最大行数，默认65536
     */
    public static final int SHEET_SIZE = 65536;

    /**
     * 工作表名称
     */
    private String sheetName;

    /**
     * 导出类型（EXPORT:导出数据；IMPORT:导入模板）
     */
    private Type type;

    /**
     * 工作薄对象
     */
    private Workbook wb;

    /**
     * 工作表对象
     */
    private Sheet sheet;

    /**
     * 样式列表
     */
    private Map<String, CellStyle> styles;

    /**
     * 导入导出数据列表
     */
    private List<T> list;

    /**
     * 注解列表
     */
    private List<Object[]> fields;

    /**
     * 实体对象
     */
    public Class<T> clazz;

    public ExcelUtil(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void init(List<T> list, String sheetName, Type type) {
        if (list == null) {
            list = new ArrayList<T>();
        }
        this.list = list;
        this.sheetName = sheetName;
        this.type = type;
        createExcelField();
        createWorkbook();
    }

    /**
     * 创建excel第一行标题
     */
    public void createTitle() {
        if (fields == null) {
            return;
        }
        int index = 0;
        for (Object[] os : fields) {
            Excel excel = (Excel) os[1];
            this.createCell(index, 0, excel.title(), null);
            index++;
        }
    }

    /**
     * 创建excel数据行
     */
    public void createData() {
        if (fields == null) {
            log.warn("Excel字段列表为空，无法创建数据行");
            return;
        }
        log.info("开始创建Excel数据行，数据条数: {}, 字段数量: {}", list.size(), fields.size());
        
        int index = 1;
        for (T t : list) {
            log.debug("处理第{}行数据: {}", index, t);
            int colIndex = 0;
            for (Object[] os : fields) {
                Excel excel = (Excel) os[1];
                String cellValue = this.getValue(t, excel);
                log.debug("第{}行第{}列，字段: {}, 值: {}", index, colIndex, excel.title(), cellValue);
                this.createCell(colIndex, index, cellValue, null);
                colIndex++;
            }
            index++;
        }
        log.info("Excel数据行创建完成，共处理{}行数据", list.size());
    }

    /**
     * 创建单元格
     */
    public Cell createCell(int column, int row, Object val, CellStyle style) {
        Row r = sheet.getRow(row);
        if (r == null) {
            r = sheet.createRow(row);
        }
        Cell cell = r.createCell(column);
        if (val == null) {
            cell.setCellValue("");
        } else if (val instanceof String) {
            cell.setCellValue((String) val);
        } else if (val instanceof Integer) {
            cell.setCellValue((Integer) val);
        } else if (val instanceof Long) {
            cell.setCellValue((Long) val);
        } else if (val instanceof Double) {
            cell.setCellValue((Double) val);
        } else if (val instanceof Float) {
            cell.setCellValue((Float) val);
        } else if (val instanceof Boolean) {
            cell.setCellValue((Boolean) val);
        } else {
            cell.setCellValue(val.toString());
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
        
        // 设置列宽，确保时间字段有足够宽度
        if (val != null && val.toString().contains("CST")) {
            sheet.setColumnWidth(column, 25 * 256); // 设置列宽为25个字符
        }
        
        return cell;
    }

    /**
     * 获取对象值
     */
    public String getValue(T t, Excel excel) {
        String value = "";
        if (excel != null) {
            value = excel.value();
        }
        if (StringUtils.isEmpty(value)) {
            String fieldName = excel.field();
            Object fieldValue = ReflectUtils.getFieldValue(t, fieldName);
            if (fieldValue != null) {
                value = fieldValue.toString();
                // 处理字段值转换
                value = convertByExp(value, excel.readConverterExp());
                // 处理时间格式
                value = formatTimeValue(value, excel.dateFormat());
            } else {
                log.debug("字段值获取失败 - 字段名: {}, 对象: {}", fieldName, t);
            }
        }
        return value;
    }

    /**
     * 格式化时间值
     */
    private String formatTimeValue(String value, String dateFormat) {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(dateFormat)) {
            return value;
        }
        
        try {
            // 如果值包含CST，说明是Date.toString()格式，需要重新格式化
            if (value.contains("CST")) {
                // 尝试解析Date格式
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", java.util.Locale.ENGLISH);
                java.util.Date date = inputFormat.parse(value);
                
                // 使用指定的格式重新格式化
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat(dateFormat);
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            log.warn("时间格式化失败: value={}, format={}, error={}", value, dateFormat, e.getMessage());
        }
        
        return value;
    }

    /**
     * 根据转换表达式转换值
     */
    private String convertByExp(String value, String readConverterExp) {
        if (StringUtils.isEmpty(value) || StringUtils.isEmpty(readConverterExp)) {
            return value;
        }
        
        try {
            String[] expArr = readConverterExp.split(",");
            for (String exp : expArr) {
                if (exp.contains("=")) {
                    String[] keyValue = exp.split("=");
                    if (keyValue.length == 2 && value.equals(keyValue[0].trim())) {
                        return keyValue[1].trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("字段值转换失败: value={}, exp={}, error={}", value, readConverterExp, e.getMessage());
        }
        
        return value;
    }

    /**
     * 创建excel字段
     */
    public void createExcelField() {
        this.fields = new ArrayList<Object[]>();
        List<Object[]> fields = new ArrayList<Object[]>();
        List<Object[]> superFields = new ArrayList<Object[]>();
        
        log.info("开始创建Excel字段，实体类: {}", this.clazz.getName());
        
        fields.addAll(ReflectUtils.getFields(this.clazz, Excel.class));
        log.info("当前类Excel字段数量: {}", fields.size());
        
        superFields.addAll(ReflectUtils.getFields(this.clazz.getSuperclass(), Excel.class));
        log.info("父类Excel字段数量: {}", superFields.size());
        
        fields.addAll(superFields);
        
        for (Object[] field : fields) {
            Excel attr = (Excel) field[1];
            if (attr != null && (attr.type() == Type.ALL || attr.type() == type)) {
                this.fields.add(field);
                log.debug("添加Excel字段: {} -> {}", field[0], attr.title());
            }
        }
        
        log.info("最终Excel字段数量: {}", this.fields.size());
    }

    /**
     * 创建工作簿
     */
    public void createWorkbook() {
        this.wb = new XSSFWorkbook();
        this.sheet = wb.createSheet();
        wb.setSheetName(0, sheetName);
    }

    /**
     * 导出Excel
     */
    public void exportExcel(HttpServletResponse response, List<T> list, String sheetName) {
        init(list, sheetName, Type.EXPORT);
        createTitle();
        createData();
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = sheetName + ".xlsx";
            
            // 处理中文文件名编码问题
            String encodedFileName;
            try {
                // 使用ISO-8859-1编码，这是HTTP标准
                encodedFileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            } catch (Exception e) {
                log.warn("文件名编码失败，使用原始文件名: {}", e.getMessage());
                encodedFileName = fileName;
            }
            
            response.setHeader("Content-disposition", "attachment;filename=\"" + encodedFileName + "\"");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            
            log.info("开始写入Excel文件，数据条数: {}", list.size());
            wb.write(response.getOutputStream());
            log.info("Excel文件写入完成");
        } catch (IOException e) {
            log.error("导出Excel异常{}", e.getMessage());
        }
    }
} 