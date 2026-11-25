package com.qykj.finmaster.customer.lead.entity.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-30
 * @Description
 */
@Data
public class AssignLeadReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -3466270204068245583L;

    /** 批量操作的ID列表 */
    private List<Long> ids;

    /** 负责人用户名 */
    private String ownerUser;

    /** 负责人真实姓名 */
    private String ownerUserName;

}