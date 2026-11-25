package com.qykj.finmaster.migration.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 财务数据迁移实体类
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@Data
@TableName("financial_data_migration")
@EqualsAndHashCode(callSuper = true)
public class FinancialDataMigration extends BaseEntity {

    private static final long serialVersionUID = 1003037191078060170L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField(value = "company_id")
    private Long companyId;

    /**
     * 正式账套ID
     */
    @TableField(value = "account_set_id")
    private Long accountSetId;

    /**
     * 体验账套ID
     */
    @TableField(value = "experience_account_set_id")
    private Long experienceAccountSetId;

    /**
     * 实体类名
     */
    @TableField(value = "entity_name")
    private String entityName;

    /**
     * 迁移数据
     */
    @TableField(value = "migration_data")
    private String migrationData;

    /**
     * 迁移状态 0-未迁移 1-已迁移
     */
    @TableField(value = "migration_status")
    private Integer migrationStatus;
}