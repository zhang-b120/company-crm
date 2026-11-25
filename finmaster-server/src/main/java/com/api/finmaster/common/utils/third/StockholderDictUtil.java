package com.qykj.finmaster.common.utils.third;

import java.util.HashMap;
import java.util.Map;

/**
 * 股东信息字典翻译工具
 * 数据来源：云薪企客 API 文档附录（投资人类型 INVTYPE / 证照类型 BLICTYPE）
 */
public class StockholderDictUtil {

    /** 投资人类型（INVTYPE） */
    private static final Map<String, String> INV_TYPE = new HashMap<>();

    /** 证照类型（BLICTYPE） */
    private static final Map<String, String> BLIC_TYPE = new HashMap<>();

    static {
        // 投资人类型
        INV_TYPE.put("10",   "法人股东");
        INV_TYPE.put("11",   "企业法人");
        INV_TYPE.put("1110", "有限公司（国有独资）");
        INV_TYPE.put("1210", "股份有限公司（上市）");
        INV_TYPE.put("12",   "事业法人");
        INV_TYPE.put("13",   "社团法人");
        INV_TYPE.put("14",   "机关法人");
        INV_TYPE.put("20",   "自然人股东");
        INV_TYPE.put("21",   "农民自然人");
        INV_TYPE.put("22",   "非农民自然人");
        INV_TYPE.put("30",   "外国（地区）投资者");
        INV_TYPE.put("31",   "外国（地区）企业");
        INV_TYPE.put("3100", "全民所有制");
        INV_TYPE.put("32",   "外国（地区）经济组织");
        INV_TYPE.put("3200", "集体所有制");
        INV_TYPE.put("33",   "外商投资性公司");
        INV_TYPE.put("34",   "外商投资创业投资企业");
        INV_TYPE.put("35",   "外籍自然人");
        INV_TYPE.put("36",   "华侨");
        INV_TYPE.put("40",   "合伙企业");
        INV_TYPE.put("41",   "内资合伙企业");
        INV_TYPE.put("42",   "外商投资合伙企业");
        INV_TYPE.put("50",   "个人独资企业");
        INV_TYPE.put("5120", "有限责任公司（中外合资）");
        INV_TYPE.put("5130", "有限责任公司（中外合作）");
        INV_TYPE.put("5140", "有限责任公司（外资）");
        INV_TYPE.put("5150", "有限责任公司（外国自然人独资）");
        INV_TYPE.put("5160", "有限责任公司（外国法人经济组织独资）");
        INV_TYPE.put("5210", "股份有限公司（中外合资、未上市）");
        INV_TYPE.put("5220", "股份有限公司（上市）");
        INV_TYPE.put("5230", "股份有限公司（外商合资、未上市）");
        INV_TYPE.put("5310", "非公司外商投资企业（中外合作）");
        INV_TYPE.put("5400", "外商投资合伙企业");
        INV_TYPE.put("60",   "社会公共股");
        INV_TYPE.put("6110", "有限责任公司（台港澳与境内合资）");
        INV_TYPE.put("6120", "有限责任公司（台港澳合资）");
        INV_TYPE.put("6130", "有限责任公司（台港澳独资）");
        INV_TYPE.put("6140", "有限责任公司（台港澳自然人独资）");
        INV_TYPE.put("6100", "有限责任公司（非法人法人经济组织独资）");
        INV_TYPE.put("6170", "有限责任公司（与外国投资者合资）");
        INV_TYPE.put("6220", "股份有限公司（台港澳与境内合资、上市）");
        INV_TYPE.put("6250", "股份有限公司（台港澳与外国投资者合资、未上市）");
        INV_TYPE.put("63",   "全公司港、澳、港企（台湾港与境内合资）");
        INV_TYPE.put("90",   "其他投资者");
        INV_TYPE.put("9100", "农民专业合作社");
        INV_TYPE.put("9999", "个体工商户");
        INV_TYPE.put("A01",  "农民非企业单位");
        INV_TYPE.put("A04",  "其他自然人投资者");
        INV_TYPE.put("A05",  "其他法人单位");
        INV_TYPE.put("A05",  "其他自然人投资者");
        INV_TYPE.put("A06",  "国家授权投资的机构或部门");
        INV_TYPE.put("A07",  "国家授权投资机构");
        INV_TYPE.put("A08",  "国家授权部门");
        INV_TYPE.put("A09",  "股份有限公司（上市、自然人控股）");
        INV_TYPE.put("A10",  "股份有限公司（上市、外商合资）");
        INV_TYPE.put("A11",  "股份有限公司（上市、外商合资）");
        INV_TYPE.put("A12",  "股份有限公司（自然人控股）");
        INV_TYPE.put("A13",  "合伙企业（外资）");
        INV_TYPE.put("A14",  "集体所有制（股份合作制）");
        INV_TYPE.put("A15",  "集体所有制（股份合作制-股份制）");
        INV_TYPE.put("A16",  "集体所有制（合作制）");
        INV_TYPE.put("A17",  "母公司");
        INV_TYPE.put("A18",  "有公司");
        INV_TYPE.put("A19",  "有限公司（法独资）内资");
        INV_TYPE.put("A20",  "有限公司（法独资）私营");
        INV_TYPE.put("A21",  "有限公司（国有化）");
        INV_TYPE.put("A22",  "有限公司（国有化—国家授权投资主体）");
        INV_TYPE.put("A24",  "有限公司（自然人控股）");
        INV_TYPE.put("A25",  "有限合伙人");
        INV_TYPE.put("A26",  "有限责任");
        INV_TYPE.put("A27",  "有限责任公司（外资）");
        INV_TYPE.put("A28",  "港澳企业");
        INV_TYPE.put("A29",  "香港居民");
        INV_TYPE.put("A09",  "澳门企业");
        INV_TYPE.put("A31",  "澳门居民");
        INV_TYPE.put("A32",  "台湾企业");
        INV_TYPE.put("A33",  "台湾居民");
        INV_TYPE.put("A34",  "境内中国公民");
        INV_TYPE.put("A35",  "境外企业或经济组织");
        INV_TYPE.put("A36",  "境外社会公众股");
        INV_TYPE.put("A37",  "村民委员会");
        INV_TYPE.put("A38",  "法人");
        INV_TYPE.put("A39",  "非公司制");
        INV_TYPE.put("A40",  "非农民成员");
        INV_TYPE.put("A41",  "个人独资");
        INV_TYPE.put("A42",  "工会法人");
        INV_TYPE.put("A43",  "合伙人");
        INV_TYPE.put("A44",  "居委会");
        INV_TYPE.put("A45",  "民办非企业法人");
        INV_TYPE.put("A46",  "内资合伙企业");
        INV_TYPE.put("A47",  "普通合伙人");
        INV_TYPE.put("A48",  "商务单位");
        INV_TYPE.put("A49",  "社会公众股");
        INV_TYPE.put("A50",  "社会团体法人");
        INV_TYPE.put("A51",  "外方（承利方）");
        INV_TYPE.put("A52",  "外国（地区）自然人");
        INV_TYPE.put("A54",  "外国公民");
        INV_TYPE.put("A55",  "外商/港澳台投资企业");
        INV_TYPE.put("A56",  "外资合伙企业");
        INV_TYPE.put("A57",  "以个人财产出资");
        INV_TYPE.put("A58",  "中方（承利方）");
        INV_TYPE.put("A59",  "管委单位");
        INV_TYPE.put("A60",  "参股单位");
        INV_TYPE.put("A61",  "自然人");
        INV_TYPE.put("A62",  "参股公司");
        INV_TYPE.put("A63",  "子公司");
        INV_TYPE.put("A64",  "港澳台投资者");
        INV_TYPE.put("A65",  "特殊普通合伙人");
        INV_TYPE.put("A66",  "无限责任");
        INV_TYPE.put("A67",  "非农民");
        INV_TYPE.put("A68",  "企业");
        INV_TYPE.put("A69",  "有限责任公司（国有控股）");
        INV_TYPE.put("A70",  "有限责任公司（自然人控股）");
        INV_TYPE.put("A71",  "内资企业法人");
        INV_TYPE.put("A99",  "国有独资");
        INV_TYPE.put("T40",  "合伙人");
        INV_TYPE.put("XD",   "股份有限公司");
        INV_TYPE.put("XE",   "联盟");
        INV_TYPE.put("XF",   "普通合伙企业");
        INV_TYPE.put("XG",   "有限合伙企业");
        INV_TYPE.put("XX_01","有限责任公司（台湾合资、上市）");
        INV_TYPE.put("XX_10","港、澳、台投资有限合伙企业");
        INV_TYPE.put("XX_13","外商投资普通合伙企业");

        // 证照类型
        BLIC_TYPE.put("1",  "内资企业法人");
        BLIC_TYPE.put("11", "企业法人营业执照（公司）");
        BLIC_TYPE.put("12", "企业法人营业执照（非公司）");
        BLIC_TYPE.put("13", "企业集团登记证");
        BLIC_TYPE.put("14", "营业执照（分公司、营业单位）");
        BLIC_TYPE.put("2",  "外资企业法人");
        BLIC_TYPE.put("21", "企业法人营业执照（外资）");
        BLIC_TYPE.put("22", "营业执照（外资）");
        BLIC_TYPE.put("23", "外国（地区）企业常驻代表机构登记证");
        BLIC_TYPE.put("24", "外商投资企业办事机构注册证");
        BLIC_TYPE.put("3",  "非法人企业、个体工商户");
        BLIC_TYPE.put("31", "个人独资企业营业执照");
        BLIC_TYPE.put("32", "合伙企业营业执照");
        BLIC_TYPE.put("33", "个体工商户营业执照");
        BLIC_TYPE.put("36", "合伙企业分支机构营业执照");
        BLIC_TYPE.put("6",  "机关、事业、社团法人");
        BLIC_TYPE.put("61", "事业法人登记证");
        BLIC_TYPE.put("62", "社团法人登记证");
        BLIC_TYPE.put("63", "机关法人登记证");
        BLIC_TYPE.put("64", "特别法人登记证");
        BLIC_TYPE.put("66", "不需要登记的社团法人");
        BLIC_TYPE.put("8",  "农民专业合作社");
        BLIC_TYPE.put("81", "农民专业合作社法人营业执照");
        BLIC_TYPE.put("82", "农民专业合作社分支机构营业执照");
        BLIC_TYPE.put("9",  "其他");
        BLIC_TYPE.put("A2", "中华人民共和国军官证");
        BLIC_TYPE.put("A3", "外国（地区）护照");
    }

    public static String getInvTypeName(String code) {
        return code == null ? null : INV_TYPE.getOrDefault(code, code);
    }

    public static String getBlicTypeName(String code) {
        return code == null ? null : BLIC_TYPE.getOrDefault(code, code);
    }
}
