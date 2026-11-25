package com.qykj.finmaster.common.core.domain.common;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.security.LoginUser;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Entity基类
 */
@Data
public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 创建者 */
    @TableField(value = "create_by")
    private String createBy;

    @TableField(value = "create_by_name")
    private String createByName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by")
    private String updateBy;

    @TableField(value = "update_by_name")
    private String updateByName;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time")
    private Date updateTime;

    /** 备注 */
    @TableField(value = "remark")
    private String remark;

}
