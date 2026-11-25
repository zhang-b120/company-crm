package com.qykj.finmaster.system.role.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class GetRoleDataScopeResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -6781315739678438307L;

    private Long id;

    /**
     * 角色ID
     */
    private Long roleId;
    
    /**
     * 数据权限类型：1-全部数据，2-本人数据，3-本部门数据，4-指定部门数据
     */
    private Integer dataScope;
    
    /**
     * 指定部门ID列表，多个用逗号分隔
     */
    private String deptIds;

    /** 创建者 */
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