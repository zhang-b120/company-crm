package com.qykj.finmaster.common.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @Author zhb
 * @Date 2025-05-13
 * @Description
 */
@Slf4j
public class WebUtils {

    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String CHARSET_PRE = "charset=";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    public static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

    private static class DefaultTrustManager implements X509TrustManager {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static InputStream doPostReStream(String url, Map<String, Object> params,int connectTimeout,int readTimeout)
            throws IOException {
        return doPostReStream(url, CONTENT_TYPE_JSON, DEFAULT_CHARSET, params, connectTimeout, readTimeout);
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static InputStream doPostReStream(String url, String contentType, String charset, Map<String, Object> params,int connectTimeout,int readTimeout)
            throws IOException {
        HttpURLConnection httpURLConnection = null;
        Writer printWriter = null;
        InputStream inputStream =null;

        String str = JSON.toJSONString(params);
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        try {
            try {
                try{
                    httpURLConnection = getConnection(new URL(url), METHOD_POST, ctype);
                    httpURLConnection.setRequestMethod(METHOD_POST);// 提交模式
                    httpURLConnection.setConnectTimeout(connectTimeout);//连接超时 单位毫秒
                    httpURLConnection.setReadTimeout(readTimeout);//读取超时 单位毫秒
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    printWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), charset);
                    printWriter.write(str);
                    printWriter.flush();// flush输出流的缓冲
                    inputStream = httpURLConnection.getInputStream();
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return inputStream;
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static String doPostReString(String url, Map<String, Object> params,int connectTimeout,int readTimeout)
            throws IOException {
        return doPostReString(url, CONTENT_TYPE_JSON, DEFAULT_CHARSET, params, connectTimeout, readTimeout);
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串 stream
     * @throws IOException
     */
    /**
     * POST 原始 JSON 字符串 body（不经 Map 包装），用于发送 JSON 数组等非对象 body。
     * @param url 目标 URL
     * @param jsonBody 已序列化好的 JSON 字符串（如 "[{...},{...}]"）
     * @return 响应字符串
     */
    public static String doPostJsonReString(String url, String jsonBody) throws IOException {
        HttpURLConnection httpURLConnection = null;
        Writer printWriter = null;
        String resp = null;
        String ctype = CONTENT_TYPE_JSON + ";" + CHARSET_PRE + DEFAULT_CHARSET;
        try {
            try {
                httpURLConnection = getConnection(new URL(url), METHOD_POST, ctype);
                httpURLConnection.setRequestMethod(METHOD_POST);
                httpURLConnection.setConnectTimeout(15000);
                httpURLConnection.setReadTimeout(15000);
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);

                printWriter = new PrintWriter(new OutputStreamWriter(httpURLConnection.getOutputStream(), DEFAULT_CHARSET));
                printWriter.write(jsonBody == null ? "" : jsonBody);
                printWriter.flush();
                resp = getResponseAsString(httpURLConnection);
            } finally {
                if (printWriter != null) {
                    try { printWriter.close(); } catch (Exception ignored) {}
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
            }
        } catch (IOException e) {
            throw e;
        }
        return resp;
    }

    public static String doPostReString(String url, String contentType, String charset, Map<String, Object> params,int connectTimeout,int readTimeout)
            throws IOException {
        HttpURLConnection httpURLConnection = null;
        Writer printWriter = null;
        String resp =null;

        String str = JSON.toJSONString(params);
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        try {
            try {
                try{
                    httpURLConnection = getConnection(new URL(url), METHOD_POST, ctype);
                    httpURLConnection.setRequestMethod(METHOD_POST);// 提交模式
                    httpURLConnection.setConnectTimeout(connectTimeout);//连接超时 单位毫秒
                    httpURLConnection.setReadTimeout(readTimeout);//读取超时 单位毫秒
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    printWriter = new PrintWriter(httpURLConnection.getOutputStream());
                    printWriter.write(str);
                    printWriter.flush();// flush输出流的缓冲
                    resp = getResponseAsString(httpURLConnection);
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return resp;
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static InputStream doPostReStream(String url, Map<String, Object> params) throws IOException {
        return doPostReStream(url, CONTENT_TYPE_JSON, DEFAULT_CHARSET, params);
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static InputStream doPostReStream(String url, String contentType, String charset, Map<String, Object> params) throws IOException {
        HttpURLConnection httpURLConnection = null;
        Writer printWriter = null;
        String str = JSON.toJSONString(params);
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        InputStream inputStream = null;
        try {
            try {
                try{
                    httpURLConnection = getConnection(new URL(url), METHOD_POST, ctype);
                    httpURLConnection.setRequestMethod(METHOD_POST);// 提交模式
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    printWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), charset);
                    printWriter.write(str);
                    printWriter.flush();// flush输出流的缓冲
                    inputStream = httpURLConnection.getInputStream();
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return inputStream;
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static String doPostReString(String url, Map<String, Object> params) throws IOException {
        return doPostReString(url, CONTENT_TYPE_JSON, DEFAULT_CHARSET, params);
    }

    /**
     * 执行HTTP POST请求 JSON。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串 stream
     * @throws IOException
     */
    public static String doPostReString(String url, String contentType, String charset, Map<String, Object> params) throws IOException {
        HttpURLConnection httpURLConnection = null;
        Writer printWriter = null;
        String str = JSON.toJSONString(params);
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        String resp = null;
        try {
            try {
                try{
                    httpURLConnection = getConnection(new URL(url), METHOD_POST, ctype);
                    httpURLConnection.setRequestMethod(METHOD_POST);// 提交模式
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    printWriter = new OutputStreamWriter(httpURLConnection.getOutputStream(), charset);
                    printWriter.write(str);
                    printWriter.flush();// flush输出流的缓冲
                    resp = getResponseAsString(httpURLConnection);
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (printWriter != null) {
                    printWriter.close();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return resp;
    }

    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param contentType 数据提交方式 例如：application/json;application/x-www-form-urlencoded
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException
     */
    public static InputStream doPostReStream(String url, Map<String, Object> params, String contentType, String charset,int connectTimeout,int readTimeout)
            throws IOException {
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        String query = buildQuery(params, charset);
        byte[] content={};
        if(query!=null){
            content=query.getBytes(charset);
        }
        return doPostWithByteReStream(url, ctype, content, connectTimeout, readTimeout);
    }

    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param ctype 请求类型
     * @param content 请求字节数组
     * @return 响应字符串
     * @throws IOException
     */
    public static InputStream doPostWithByteReStream(String url, String ctype, byte[] content,int connectTimeout,int readTimeout) throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        InputStream inputStream = null;
        try {
            try {
                try{
                    conn = getConnection(new URL(url), METHOD_POST, ctype);
                    conn.setConnectTimeout(connectTimeout);
                    conn.setReadTimeout(readTimeout);
                }catch(IOException e){
                    throw e;
                }
                try{
                    out = conn.getOutputStream();
                    out.write(content);
                    inputStream = conn.getInputStream();
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (out != null) {
                    out.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return inputStream;
    }

    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param contentType 数据提交方式 例如：application/json;application/x-www-form-urlencoded
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException
     */
    public static String doPostReString(String url, Map<String, Object> params, String contentType, String charset,int connectTimeout,int readTimeout)
            throws IOException {
        String ctype = contentType + ";" + CHARSET_PRE + charset;
        String query = buildQuery(params, charset);
        byte[] content={};
        if(query!=null){
            content=query.getBytes(charset);
        }
        return doPostWithByteReString(url, ctype, content, connectTimeout, readTimeout);
    }

    /**
     * 执行HTTP POST请求。
     *
     * @param url 请求地址
     * @param ctype 请求类型
     * @param content 请求字节数组
     * @return 响应字符串
     * @throws IOException
     */
    public static String doPostWithByteReString(String url, String ctype, byte[] content,int connectTimeout,int readTimeout) throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        String rsp = null;
        try {
            try {
                try{
                    conn = getConnection(new URL(url), METHOD_POST, ctype);
                    conn.setConnectTimeout(connectTimeout);
                    conn.setReadTimeout(readTimeout);
                }catch(IOException e){
                    throw e;
                }
                try{
                    out = conn.getOutputStream();
                    out.write(content);
                    rsp = getResponseAsString(conn);
                }catch(IOException e){
                    throw e;
                }

            }finally {
                if (out != null) {
                    out.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }

        return rsp;
    }


    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @return 响应字符串
     * @throws IOException
     */
    public static String doGet(String url, Map<String, Object> params) throws IOException {
        return doGet(url, params, DEFAULT_CHARSET);
    }
    public static InputStream doGetReInputStream(String url, Map<String, Object> params) throws IOException {
        return doGetReInputStream(url, params, DEFAULT_CHARSET);
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException
     */
    public static InputStream doGetReInputStream(String url, Map<String, Object> params, String charset)
            throws IOException {
        long startTime = System.currentTimeMillis();
        HttpURLConnection conn = null;
        InputStream inputStream = null;
        PrintWriter printWriter = null;
        String str = JSON.toJSONString(params);
        String ctype = CONTENT_TYPE_FORM + ";"  + CHARSET_PRE + charset;
        String query = buildQuery(params, charset);
        try {
            try {
                try{
                    conn = getConnection(buildGetUrl(url, query), METHOD_GET, ctype);
                }catch(IOException e){
                    throw e;
                }

                try{
                    printWriter = new PrintWriter(conn.getOutputStream());
                    printWriter.write(str);
                    printWriter.flush();
                    inputStream = conn.getInputStream();
                }catch(IOException e){
                    throw e;
                }

            } finally {
                if (printWriter != null) {
                    printWriter.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
        }

        return inputStream;
    }

    /**
     * 执行HTTP GET请求。
     *
     * @param url 请求地址
     * @param params 请求参数
     * @param charset 字符集，如UTF-8, GBK, GB2312
     * @return 响应字符串
     * @throws IOException
     */
    public static String doGet(String url, Map<String, Object> params, String charset)
            throws IOException {
        long startTime = System.currentTimeMillis();
        HttpURLConnection conn = null;
        String rsp = null;
        String ctype = CONTENT_TYPE_FORM + ";"  + CHARSET_PRE + charset;
        String query = buildQuery(params, charset);
        try {
            try {
                try{
                    conn = getConnection(buildGetUrl(url, query), METHOD_GET, ctype);
                }catch(IOException e){
                    throw e;
                }

                try{
                    rsp = getResponseAsString(conn);
                }catch(IOException e){
                    throw e;
                }

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
        }

        return rsp;
    }

    /**
     * 上传文件（通用multipart/form-data上传方法）
     * @param url
     * @param file
     * @param fileFieldName
     * @return
     * @throws IOException
     */
    public static String uploadFile(String url, File file, String fileFieldName) throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;
        FileInputStream fileIn = null;

        // 生成边界字符串
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String contentType = CONTENT_TYPE_MULTIPART + ";" + "boundary=" + boundary;

        try {
            // 获取连接
            conn = getConnection(new URL(url), METHOD_POST, contentType);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = conn.getOutputStream();
            // 写入文件字段
            writeFilePart(out, boundary, fileFieldName, file);
            // 写入结束边界
            writeBoundaryEnd(out, boundary);
            out.flush();
            // 获取响应
            return getResponseAsString(conn);

        } finally {
            // 关闭资源
            if (out != null) {
                out.close();
            }
            if (fileIn != null) {
                fileIn.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 通过InputStream上传文件（适用于内存中的文件数据）
     * @param url
     * @param inputStream
     * @param fileName
     * @param fileFieldName
     * @param fileLength
     * @return
     * @throws IOException
     */
    public static String uploadFile(String url, InputStream inputStream, String fileName, String fileFieldName, Long fileLength) throws IOException {
        HttpURLConnection conn = null;
        OutputStream out = null;

        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        String contentType = CONTENT_TYPE_MULTIPART + ";" + "boundary=" + boundary;

        try {
            conn = getConnection(new URL(url), METHOD_POST, contentType);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            out = conn.getOutputStream();
            // 写入文件部分
            writeFilePart(out, boundary, fileFieldName, inputStream, fileName, fileLength);
            // 写入结束边界
            writeBoundaryEnd(out, boundary);
            out.flush();
            return getResponseAsString(conn);

        } finally {
            if (out != null) {
                out.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 写入文件部分到输出流
     */
    private static void writeFilePart(OutputStream out, String boundary,
                                      String fieldName, File file) throws IOException {

        // 构建Content-Disposition头
        String contentDisposition = String.format(
                "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"; filelength=%d\r\n",
                fieldName, file.getName(), file.length()
        );
        // 构建Content-Type头
        String contentType = String.format("Content-Type: %s\r\n\r\n", CONTENT_TYPE_OCTET_STREAM);
        // 写入边界
        out.write(("--" + boundary + "\r\n").getBytes());
        // 写入Content-Disposition
        out.write(contentDisposition.getBytes());
        // 写入Content-Type
        out.write(contentType.getBytes());

        // 写入文件内容
        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        // 写入换行
        out.write(("\r\n").getBytes());
    }

    /**
     * 写入文件部分（使用InputStream）
     */
    private static void writeFilePart(OutputStream out, String boundary,
                                      String fieldName, InputStream inputStream,
                                      String fileName, long fileLength) throws IOException {
        // 构建Content-Disposition头
        String contentDisposition = String.format(
                "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"; filelength=%d\r\n",
                fieldName, fileName, fileLength
        );
        // 构建Content-Type头
        String contentType = String.format("Content-Type: %s\r\n\r\n", CONTENT_TYPE_OCTET_STREAM);
        // 写入边界
        out.write(("--" + boundary + "\r\n").getBytes());
        // 写入Content-Disposition
        out.write(contentDisposition.getBytes());
        // 写入Content-Type
        out.write(contentType.getBytes());

        // 写入文件内容
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        // 写入换行
        out.write(("\r\n").getBytes());
    }

    /**
     * 写入边界结束标记
     */
    private static void writeBoundaryEnd(OutputStream out, String boundary) throws IOException {
        out.write(("--" + boundary + "--\r\n").getBytes());
    }

    private static HttpURLConnection getConnection(URL url, String method, String ctype) throws IOException {
        HttpURLConnection conn = null;
        if ("https".equals(url.getProtocol())) {
            SSLContext ctx = null;
            try {
                ctx = SSLContext.getInstance("TLS");
                ctx.init(new KeyManager[0], new TrustManager[] { new DefaultTrustManager() }, new SecureRandom());
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
            HttpsURLConnection connHttps = (HttpsURLConnection) url.openConnection();
            connHttps.setSSLSocketFactory(ctx.getSocketFactory());
            connHttps.setHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    return true;// 默认都认证通过
                }
            });
            conn = connHttps;
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }

        conn.setRequestMethod(method);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", ctype);
        return conn;
    }

    public static URL buildGetUrl(String strUrl, String query) throws IOException {
        URL url = new URL(strUrl);
        if (StringUtils.isEmpty(query)) {
            return url;
        }

        if (StringUtils.isEmpty(url.getQuery())) {
            if (strUrl.endsWith("?")) {
                strUrl = strUrl + query;
            } else {
                strUrl = strUrl + "?" + query;
            }
        } else {
            if (strUrl.endsWith("&")) {
                strUrl = strUrl + query;
            } else {
                strUrl = strUrl + "&" + query;
            }
        }

        return new URL(strUrl);
    }

    public static String buildQuery(Map<String, Object> params, String charset) throws IOException {
        if (params == null || params.isEmpty()) {
            return null;
        }

        StringBuilder query = new StringBuilder();
        Set<Entry<String, Object>> entries = params.entrySet();
        boolean hasParam = false;

        for (Entry<String, Object> entry : entries) {
            String name = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue().toString():"";
            // 忽略参数名或参数值为空的参数
            if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value)) {
                if (hasParam) {
                    query.append("&");
                } else {
                    hasParam = true;
                }

                query.append(name).append("=").append(URLEncoder.encode(value, charset));
            }
        }

        return query.toString();
    }

    protected static String getResponseAsString(HttpURLConnection conn) throws IOException {
        String charset = getResponseCharset(conn.getContentType());
        InputStream es = conn.getErrorStream();
        if (es == null) {
            return getStreamAsString(conn.getInputStream(), charset);
        } else {
            String msg = getStreamAsString(es, charset);
            if (StringUtils.isEmpty(msg)) {
                throw new IOException(conn.getResponseCode() + ":" + conn.getResponseMessage());
            } else {
                throw new IOException(msg);
            }
        }
    }

    public static String getStreamAsString(InputStream stream, String charset) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
            StringWriter writer = new StringWriter();

            char[] chars = new char[256];
            int count = 0;
            while ((count = reader.read(chars)) > 0) {
                writer.write(chars, 0, count);
            }

            return writer.toString();
        } finally {
            if (stream != null) {
                stream.close();
            }
        }
    }

    private static String getResponseCharset(String ctype) {
        String charset = DEFAULT_CHARSET;

        if (!StringUtils.isEmpty(ctype)) {
            String[] params = ctype.split(";");
            for (String param : params) {
                param = param.trim();
                if (param.startsWith("charset")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        if (!StringUtils.isEmpty(pair[1])) {
                            charset = pair[1].trim();
                        }
                    }
                    break;
                }
            }
        }

        return charset;
    }


    /**
     * 使用默认的UTF-8字符集编码请求参数值。
     *
     * @param value 参数值
     * @return 编码后的参数值
     */
    public static String encode(String value) {
        return encode(value, DEFAULT_CHARSET);
    }

    /**
     * 使用指定的字符集编码请求参数值。
     *
     * @param value 参数值
     * @param charset 字符集
     * @return 编码后的参数值
     */
    public static String encode(String value, String charset) {
        String result = null;
        if (!StringUtils.isEmpty(value)) {
            try {
                result = URLEncoder.encode(value, charset);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}