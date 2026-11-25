package com.qykj.finmaster.system.role.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RoleDataScopeDTO {
    
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
    
    @NotNull(message = "数据范围不能为空")
    private Integer dataScope;
    
    private List<Long> deptIds;
} 