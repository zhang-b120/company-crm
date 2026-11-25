package com.qykj.finmaster.common.utils;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * @Author zhb
 * @Date 2025-08-20
 * @Description
 */
public class FileUtils {

    /**
     * 上传文件
     * @param file 原始文件
     * @param savePath 文件保存路径
     * @param saveFileName 保存文件名（无后缀）
     * @return
     */
    public static Boolean uploadFile(MultipartFile file, String savePath, String saveFileName) {
        // 文件保存路径
        if(StringUtils.isEmpty(savePath)){
            throw new BusinessException(ResultMsg.FILE_UPLOAD_NOT_SAVE_PATH);
        }
        try {
            File dir = new File(savePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File saveFile = new File(dir, saveFileName);
            // 保存文件
            file.transferTo(saveFile);
        } catch (Exception e) {
            throw new BusinessException(ResultMsg.FILE_UPLOAD_FAIL);
        }
        return Boolean.TRUE;
    }

    /**
     * 根据上传类型 获取保存
     * @param uploadType
     * @return
     */
    public static String getSaveFileName(String uploadType, String fileSuffix) {
        StringBuffer saveFileName = new StringBuffer();
        if (FileConstants.FILE_TYPE_ORD_ACCOUNT.equals(uploadType)) {
            saveFileName.append(FileConstants.FILE_PRE_ORD_ACCOUNT);
        }else if(FileConstants.FILE_TYPE_COMPANY_RECORD.equals(uploadType)){
            saveFileName.append(FileConstants.FILE_PRE_COMPANY_RECORD);
        }
        saveFileName.append(System.currentTimeMillis()).append(GlobalConstant.Symbol.SPOT).append(fileSuffix);
        return saveFileName.toString();
    }

    /**
     * 根据上传类型 获取保存
     * @param uploadType
     * @return
     */
    public static String getSavePath(String uploadType) {
        String path = null;
        if (FileConstants.FILE_TYPE_ORD_ACCOUNT.equals(uploadType)) {
            path = FileConstants.FILE_URL_ORD_ACCOUNT;;
        }else if(FileConstants.FILE_TYPE_COMPANY_RECORD.equals(uploadType)){
            path = FileConstants.FILE_URL_COMPANY_RECORD;;
        }else if(FileConstants.FILE_TYPE_ORDER_REFUND.equals(uploadType)){
            path = FileConstants.FILE_URL_ORDER_REFUND;
        }else if(FileConstants.FILE_TYPE_CONTRACT.equals(uploadType)){
            path = FileConstants.FILE_URL_CONTRACT;
        }
        return path;
    }

    /**
     * 获取文件base64编码
     *
     * @param path      文件路径
     * @param urlEncode
     * @return base64编码信息，不带文件头
     * @throws Exception IO异常
     */
    public static String getFileContentAsBase64(String path, boolean urlEncode) {
        byte[] b = readFileByBytes(path);
        String base64 = Base64.getEncoder().encodeToString(b);
        if (urlEncode) {
            try {
                base64 = URLEncoder.encode(base64, GlobalConstant.UTF8);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return base64;
    }

    /**
     * 读取文件内容，作为字符串返回
     * @param filePath
     * @return
     * @throws IOException
     */
    public static String readFileAsString(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_NOT_EXISTS, filePath));
        }
        if (file.length() > GlobalConstant.FILE_MAX_SIZE) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_TOO_LARGE, filePath, GlobalConstant.FILE_MAX_SIZE));
        }

        StringBuilder sb = new StringBuilder((int) (file.length()));
        // 创建字节输入流
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            // 创建一个长度为10240的Buffer
            byte[] bbuf = new byte[10240];
            // 用于保存实际读取的字节数
            int len = 0;
            while ( (len = fis.read(bbuf)) > 0 ) {
                sb.append(new String(bbuf, 0, len));
            }
            fis.close();
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_ERROR, filePath, e.getMessage()));
        }
    }

    /**
     * 根据文件路径读取byte[] 数组
     * @param filePath
     * @return
     */
    public static byte[] readFileByBytes(String filePath)  {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[10240];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    bos.write(buffer, 0, len);
                }
                byte[] byteArray = bos.toByteArray();
                return byteArray;
            } catch (IOException e) {
                throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_ERROR, filePath, e.getMessage()));
            } finally {
                try {
                    in.close();
                    bos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * 分块读取大文件（避免内存溢出）
     * @param filePath
     * @return
     * @throws IOException
     */
    public static byte[] readLargeFileByBytes(String filePath)  {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_NOT_EXISTS, filePath));
        }
        if (file.length() > Integer.MAX_VALUE) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_TOO_LARGE, filePath, Integer.MAX_VALUE));
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length());
            byte[] buffer = new byte[10240];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(MessageFormat.format(ResultMsg.FILE_READ_ERROR, filePath, e.getMessage()));
        }
    }
}