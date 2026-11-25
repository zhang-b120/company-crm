package com.qykj.finmaster.common.core.domain.common;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-01
 * @Description
 */
@Data
public class CodeEntity implements Serializable {
    private static final long serialVersionUID = -4791439462807797928L;

    @NotNull
    private String code;
}