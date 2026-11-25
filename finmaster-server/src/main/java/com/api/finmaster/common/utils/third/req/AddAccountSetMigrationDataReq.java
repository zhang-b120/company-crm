package com.qykj.finmaster.common.utils.third.req;

import com.beust.jcommander.internal.Maps;
import lombok.Data;
import org.springframework.cglib.beans.BeanMap;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@Data
public class AddAccountSetMigrationDataReq implements Serializable {
    private static final long serialVersionUID = -3760945648313472477L;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 实体类名
     */
    private String entityName;

    /**
     * 迁移数据
     */
    private String migrationData;
}