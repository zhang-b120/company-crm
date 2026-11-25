package com.qykj.finmaster.common.utils;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * docx 模板渲染：替换 ${key} 占位符。
 * Word 常常会把同一个占位符拆到多个 run，所以这里采用"把段落里所有 run 的文本拼起来再替换、
 * 然后把结果写回第一个 run、清空其余 run"的策略，能正确处理跨 run 占位符。
 * 注意：会丢弃同一段落内不同 run 的样式差异，但模板里占位符所在 run 通常样式一致，可接受。
 */
public final class DocxTemplateUtil {

    /** 占位符值的哨兵字符（Unicode 私有区），applyValuesWithMarkers 使用，XwpfHtmlUtil 识别。 */
    public static final char MARKER_START = '';
    public static final char MARKER_SEP = '';
    public static final char MARKER_END = '';

    private DocxTemplateUtil() {
    }

    public static void render(String templatePath, Map<String, String> values, String outputPath) throws IOException {
        try (FileInputStream in = new FileInputStream(templatePath);
             XWPFDocument doc = new XWPFDocument(in)) {
            applyValues(doc, values);
            try (FileOutputStream out = new FileOutputStream(outputPath)) {
                doc.write(out);
            }
        }
    }

    /**
     * 把模板加载到内存并替换占位符，但不落盘。调用方负责关闭返回的 XWPFDocument。
     * 用于预览场景：渲染后立即转 HTML 给前端展示，不产生持久文件。
     */
    public static XWPFDocument renderInMemory(String templatePath, Map<String, String> values) throws IOException {
        try (FileInputStream in = new FileInputStream(templatePath)) {
            XWPFDocument doc = new XWPFDocument(in);
            applyValues(doc, values);
            return doc;
        }
    }

    /**
     * 在已打开的 XWPFDocument 上原地替换 ${key} 占位符。
     */
    public static void applyValues(XWPFDocument doc, Map<String, String> values) {
        applyValuesInternal(doc, values, false);
    }

    /**
     * 与 applyValues 类似，但占位符值前后会包裹 PUA 哨兵字符（U+E000..U+E002），
     * 供 XwpfHtmlUtil 在转 HTML 时识别并输出 contenteditable 的红框 span。
     * 仅用于浏览器内可视化预览，不要用于生成最终落盘的 docx。
     */
    public static void applyValuesWithMarkers(XWPFDocument doc, Map<String, String> values) {
        applyValuesInternal(doc, values, true);
    }

    private static void applyValuesInternal(XWPFDocument doc, Map<String, String> values, boolean withMarkers) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            replaceInParagraph(p, values, withMarkers);
        }
        for (XWPFTable t : doc.getTables()) {
            for (XWPFTableRow row : t.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph p : cell.getParagraphs()) {
                        replaceInParagraph(p, values, withMarkers);
                    }
                }
            }
        }
    }

    private static void replaceInParagraph(XWPFParagraph paragraph, Map<String, String> values, boolean withMarkers) {
        List<XWPFRun> runs = paragraph.getRuns();
        if (runs == null || runs.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (XWPFRun r : runs) {
            String t = r.text();
            if (t != null) sb.append(t);
        }
        String original = sb.toString();
        if (!original.contains("${")) {
            return;
        }
        String replaced = applyReplacements(original, values, withMarkers);
        if (replaced.equals(original)) {
            return;
        }
        // 将结果写回第一个 run，清空其余
        XWPFRun first = runs.get(0);
        first.setText(replaced, 0);
        for (int i = 1; i < runs.size(); i++) {
            runs.get(i).setText("", 0);
        }
    }

    private static String applyReplacements(String text, Map<String, String> values, boolean withMarkers) {
        String result = text;
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = "${" + e.getKey() + "}";
            String val = e.getValue() == null ? "" : e.getValue();
            String injected;
            if (withMarkers) {
                injected = "" + MARKER_START + e.getKey() + MARKER_SEP + val + MARKER_END;
            } else {
                injected = val;
            }
            result = result.replace(key, injected);
        }
        return result;
    }
}
