package com.qykj.finmaster.common.utils;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBrType;

import java.math.BigInteger;

import java.util.List;

/**
 * 把 XWPFDocument 转成简单 HTML，用于浏览器内预览。
 * 仅保留：段落、对齐、加粗、斜体、下划线、字号、表格（含简单边框）、空行。
 * 不处理：图片、复杂样式、页眉页脚、文本框等。
 */
public final class XwpfHtmlUtil {

    private XwpfHtmlUtil() {
    }

    public static String toHtml(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("<div class=\"docx-preview\">");
        List<IBodyElement> elements = doc.getBodyElements();
        for (IBodyElement el : elements) {
            switch (el.getElementType()) {
                case PARAGRAPH -> appendParagraph(sb, (XWPFParagraph) el);
                case TABLE -> appendTable(sb, (XWPFTable) el);
                default -> {
                    // 暂不处理其它类型
                }
            }
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static void appendParagraph(StringBuilder sb, XWPFParagraph p) {
        boolean pageBreak = paragraphHasPageBreak(p);

        // 段落是否完全无文本（图片不参与，预览不渲染图片）
        boolean anyText = false;
        for (XWPFRun r : p.getRuns()) {
            String t = r.text();
            if (t != null && !t.isEmpty()) { anyText = true; break; }
        }

        if (pageBreak && !anyText) {
            sb.append("<div class=\"page-break\"></div>");
            return;
        }
        if (pageBreak) {
            sb.append("<div class=\"page-break\"></div>");
        }

        String align = mapAlign(p.getAlignment());
        sb.append("<p");
        if (align != null) {
            sb.append(" style=\"text-align:").append(align).append(";\"");
        }
        sb.append('>');

        boolean rendered = false;
        for (XWPFRun r : p.getRuns()) {
            String text = r.text();
            if (text == null || text.isEmpty()) continue;
            rendered = true;
            appendRun(sb, r, text);
        }
        if (!rendered) {
            sb.append("&#160;");
        }
        sb.append("</p>");
    }

    /** 段落是否包含 docx 分页符（pPr.pageBreakBefore 或 run 内 <w:br type="page"/>）。 */
    private static boolean paragraphHasPageBreak(XWPFParagraph p) {
        try {
            CTPPr pPr = p.getCTP().getPPr();
            if (pPr != null && pPr.getPageBreakBefore() != null) return true;
        } catch (Exception ignored) { /* 容错 */ }
        for (XWPFRun r : p.getRuns()) {
            try {
                CTR ctr = r.getCTR();
                if (ctr == null) continue;
                List<CTBr> brs = ctr.getBrList();
                if (brs == null) continue;
                for (CTBr br : brs) {
                    STBrType.Enum type = br.getType();
                    if (type != null && type == STBrType.PAGE) return true;
                }
            } catch (Exception ignored) { /* 容错 */ }
        }
        return false;
    }

    private static void appendRun(StringBuilder sb, XWPFRun r, String text) {
        StringBuilder style = new StringBuilder();
        if (r.isBold()) style.append("font-weight:bold;");
        if (r.isItalic()) style.append("font-style:italic;");
        UnderlinePatterns u = r.getUnderline();
        if (u != null && u != UnderlinePatterns.NONE) style.append("text-decoration:underline;");
        int fs = r.getFontSize();
        if (fs > 0) style.append("font-size:").append(fs).append("pt;");
        String color = r.getColor();
        if (color != null && !color.isEmpty() && !"auto".equalsIgnoreCase(color)) {
            style.append("color:#").append(color).append(';');
        }
        sb.append("<span");
        if (style.length() > 0) {
            sb.append(" style=\"").append(style).append('"');
        }
        sb.append('>');
        if (text != null && !text.isEmpty()) {
            appendTextWithPlaceholders(sb, text);
        }
        sb.append("</span>");
    }

    /**
     * 把 run 文本里被哨兵字符包裹的占位符值切出来，输出为 contenteditable 的 .placeholder span，
     * 供前端就地编辑。其余文本按普通文本转义输出。
     */
    private static void appendTextWithPlaceholders(StringBuilder sb, String text) {
        if (text == null || text.isEmpty()) return;
        int i = 0;
        int len = text.length();
        while (i < len) {
            int start = text.indexOf(DocxTemplateUtil.MARKER_START, i);
            if (start < 0) {
                sb.append(escapeHtml(text.substring(i)));
                return;
            }
            int sep = text.indexOf(DocxTemplateUtil.MARKER_SEP, start + 1);
            int end = sep < 0 ? -1 : text.indexOf(DocxTemplateUtil.MARKER_END, sep + 1);
            if (sep < 0 || end < 0) {
                // 哨兵不完整，原样输出
                sb.append(escapeHtml(text.substring(i)));
                return;
            }
            // 输出哨兵之前的普通文本
            if (start > i) {
                sb.append(escapeHtml(text.substring(i, start)));
            }
            String key = text.substring(start + 1, sep);
            String val = text.substring(sep + 1, end);
            sb.append("<span class=\"placeholder\" data-key=\"")
                    .append(escapeHtml(key))
                    .append("\" contenteditable=\"true\" spellcheck=\"false\">")
                    .append(escapeHtml(val))
                    .append("</span>");
            i = end + 1;
        }
    }

    private static void appendTable(StringBuilder sb, XWPFTable table) {
        // 检查 tblBorders 是否全为 none/nil；是则给表格加 no-border 类，CSS 把 td 边框去掉
        boolean noBorder = isAllBordersNone(table);
        sb.append("<table class=\"docx-table");
        if (noBorder) sb.append(" no-border");
        sb.append("\">");
        for (XWPFTableRow row : table.getRows()) {
            sb.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                appendCell(sb, cell);
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
    }

    private static void appendCell(StringBuilder sb, XWPFTableCell cell) {
        sb.append("<td");
        try {
            CTTcPr tcPr = cell.getCTTc() == null ? null : cell.getCTTc().getTcPr();
            if (tcPr != null) {
                // <w:gridSpan val="N"/> → colspan="N"
                if (tcPr.getGridSpan() != null && tcPr.getGridSpan().getVal() != null) {
                    BigInteger v = tcPr.getGridSpan().getVal();
                    int colspan = v.intValue();
                    if (colspan > 1) sb.append(" colspan=\"").append(colspan).append("\"");
                }
                // <w:tcW w="N" type="dxa"/> → style="width:Npt"
                CTTblWidth tcW = tcPr.getTcW();
                if (tcW != null && tcW.getW() != null) {
                    String type = tcW.getType() == null ? "dxa" : tcW.getType().toString().toLowerCase();
                    String widthStyle = mapCellWidth(tcW.getW().toString(), type);
                    if (widthStyle != null) sb.append(" style=\"").append(widthStyle).append("\"");
                }
            }
        } catch (Exception ignored) { /* 容错：拿不到精确属性就退化成普通 <td> */ }
        sb.append('>');
        for (XWPFParagraph p : cell.getParagraphs()) {
            appendParagraph(sb, p);
        }
        sb.append("</td>");
    }

    /** docx tcW: dxa = twentieths of a point；pct = 50ths of a percent；auto/nil → 不输出宽度 */
    private static String mapCellWidth(String w, String type) {
        if (w == null || w.isEmpty()) return null;
        try {
            switch (type) {
                case "dxa":
                    double pt = Long.parseLong(w) / 20.0;
                    return "width:" + String.format("%.1fpt", pt);
                case "pct":
                    double pct = Long.parseLong(w) / 50.0;
                    return "width:" + String.format("%.1f%%", pct);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 整张表的 tblBorders 6 条边（top/left/bottom/right/insideH/insideV）是否全是 none/nil。 */
    private static boolean isAllBordersNone(XWPFTable table) {
        try {
            CTTblPr tblPr = table.getCTTbl() == null ? null : table.getCTTbl().getTblPr();
            if (tblPr == null) return false;
            CTTblBorders b = tblPr.getTblBorders();
            if (b == null) return false;
            return isNoneBorder(b.getTop())
                    && isNoneBorder(b.getLeft())
                    && isNoneBorder(b.getBottom())
                    && isNoneBorder(b.getRight())
                    && isNoneBorder(b.getInsideH())
                    && isNoneBorder(b.getInsideV());
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isNoneBorder(CTBorder border) {
        if (border == null) return true;
        Object val = border.getVal();
        if (val == null) return true;
        String s = val.toString().toLowerCase();
        return "none".equals(s) || "nil".equals(s);
    }

    private static String mapAlign(ParagraphAlignment a) {
        if (a == null) return null;
        return switch (a) {
            case CENTER -> "center";
            case RIGHT, END -> "right";
            case BOTH, DISTRIBUTE -> "justify";
            case LEFT, START -> null;
            default -> null;
        };
    }

    private static String escapeHtml(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&#39;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }
}
