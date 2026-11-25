package com.qykj.finmaster.common.core.domain.constansts;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
public class GlobalConstant {

	public static final long FILE_MAX_SIZE = 10 * 1024 * 1024;
	public static final String UNKNOWN = "unknown";

	public static final String X_FORWARDED_FOR = "X-Forwarded-For";
	public static final String X_REAL_IP = "X-Real-IP";
	public static final String PROXY_CLIENT_IP = "Proxy-Client-IP";
	public static final String WL_PROXY_CLIENT_IP = "WL-Proxy-Client-IP";
	public static final String HTTP_CLIENT_IP = "HTTP_CLIENT_IP";
	public static final String HTTP_X_FORWARDED_FOR = "HTTP_X_FORWARDED_FOR";
	public static final String REMOTE_ADDR          = "remote-addr";
	public static final String X_REQUEST_GRAY       = "X-Request-Gray";

	public static final String LOCALHOST_IP = "127.0.0.1";
	public static final String LOCALHOST_IP_16 = "0:0:0:0:0:0:0:1";
	public static final int MAX_IP_LENGTH = 15;
	public static final String UTF8 = "UTF-8";
	public static final String SIGN_RSA = "RSA";
	public static final String SIGN_RSA_NONE = "NONE";
	public static final String SIGN_RSA_1 = "RSA1";
	public static final String SIGN_RSA_256 = "RSA256";

	public static final String BASE_URL = "qingyicloud.com";
	public static final String IMG_URL = "/common/file/imgs/";
	public static final String EXCEL_URL = "/common/file/excel/";
	// 默认租户ID
	public static final Long DEFAULT_TENANT_ID = 1L;
	public static final String DEFAULT_USER_PASSWORD = "123456";

	public static final long MILLI_SCALE  = 1L;
	public static final long SECOND_SCALE = 1000L * MILLI_SCALE;
	public static final long MINUTE_SCALE = 60L * SECOND_SCALE;
	public static final long SIX_MINUTE_SCALE = 6L * MINUTE_SCALE;
	public static final long HOUR_SCALE   = 60L * MINUTE_SCALE;
	public static final long DAY_SCALE    = 24L * HOUR_SCALE;
	public static final long TEN_DAY_SCALE    = 10L * DAY_SCALE;
	//超级管理员和管理员
	public static final List<Long> adminRoleIds = Arrays.asList(1L, 3L);

	//所有管理员
	public static final List<Long> allAdminRoleIds = Arrays.asList(1L, 3L, 9L, 12L, 13L, 15L);

	public interface Number {
		int THOUSAND_INT = 1000;
		int TWOTHOUSAND_INT = 2000;
		int HUNDRED_INT = 100;
		int TWOHUNDRED_INT = 200;
		int FIVEHUNDRED_INT = 500;
		int ZERO_INT = 0;
		int ONE_INT = 1;
		int TWO_INT = 2;
		int THREE_INT = 3;
		int FOUR_INT = 4;
		int FIVE_INT = 5;
		int SIX_INT = 6;
		int SEVEN_INT = 7;
		int EIGHT_INT = 8;
		int NINE_INT = 9;
		int TEN_INT = 10;
		int SEVENTEEN_INT = 17;
		int EIGHTEEN_INT = 18;

		long ZERO_LONG = 0L;
		long ONE_LONG = 1L;
		long TWO_LONG = 2L;
		long THREE_LONG = 3L;
		long FOUR_LONG = 4L;
		long FIVE_LONG = 5L;
		long SIX_LONG = 6L;
		long SEVEN_LONG = 7L;
		long EIGHT_LONG = 8L;
		long NINE_LONG= 9L;
		long TEN_LONG = 10L;
		long TWELVE_LONG = 12L;
		long FIFTY_LONG = 50L;
		long HUNDRED_LONG = 100L;
		long TWO_HUNDRED_LONG = 200L;
		long FIVE_HUNDRED_LONG = 500L;
		long THOUSAND_LONG = 1000L;
		long TEN_THOUSAND_LONG = 10000L;

	}

	public interface Str{
		public final String ZERO = "0";
		public final String ONE = "1";
		public final String TWO = "2";
		public final String THREE = "3";
		public final String FOUR = "4";
	}

	public static final class Symbol {
		private Symbol() {
		}
		public static final String COMMA = ",";
		public static final String SPOT = ".";
		public static final String SPOT_REGULAR = "\\.";
		public final static String UNDER_LINE = "_";
		public final static String UNDER_LINE_ZERO = "_0";
		public final static String PER_CENT = "%";
		public final static String AT = "@";
		public final static String PIPE = "||";
		public final static String SHORT_LINE = "-";
		public final static String SPACE = " ";
		public final static String SLASH = "/";
		public final static String MH = ":";
		public final static String CHINESE_MH = "：";
		public final static String BAR = "|";
		public final static String COMMA_BAR = COMMA + BAR;
		public final static String COMMA_BAR_REGULAR = COMMA + "\\" + BAR;
		public final static String STAR = "*";
		public final static String TILDE = "~";
		public final static String ENTER = "\n";
	}

	public static final class Domain {
		private Domain() {
		}
		public static final String HTTP = "http://";
		public static final String HTTPS = "https://";
		public static final String WWW = "www.";
		public static final String HTTP_WWW = HTTP + WWW;
		public static final String HTTPS_WWW = HTTPS + WWW;
	}

	public static final class StaticUrl{
		private StaticUrl() {
		}
		public static final String IMG = Domain.HTTPS_WWW + BASE_URL + IMG_URL;
		public static final String EXCEL = Domain.HTTPS_WWW + BASE_URL + EXCEL_URL;
	}

	public static final class Suffix{
		private Suffix() {
		}
		public static final String LOWER_PDF = ".pdf";
		public static final String CAP_PDF = ".PDF";
	}
}

