package com.qykj.finmaster.system.productType.domain.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class GetProductTypeResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -2633362667967856058L;
    /**
     * 类型ID
     */
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    private String createBy;

    private String createByName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新者 */
    private String updateBy;

    private String updateByName;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;
}
