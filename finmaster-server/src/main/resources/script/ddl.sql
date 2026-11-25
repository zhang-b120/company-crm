-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码',
    `real_name` varchar(50) DEFAULT NULL COMMENT '姓名',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
    `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `name` varchar(50) NOT NULL COMMENT '角色名称',
    `code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(255) DEFAULT NULL COMMENT '角色描述',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
    `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
                                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                          `parent_id` bigint DEFAULT NULL COMMENT '父菜单ID',
                                          `name` varchar(50) NOT NULL COMMENT '菜单名称',
    `path` varchar(200) DEFAULT NULL COMMENT '路由路径',
    `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
    `permission` varchar(100) DEFAULT NULL COMMENT '权限标识',
    `icon` varchar(100) DEFAULT NULL COMMENT '图标',
    `type` tinyint NOT NULL COMMENT '类型：1目录 2菜单 3按钮',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
    `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 部门表
CREATE TABLE IF NOT EXISTS `sys_department` (
                                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                `parent_id` bigint DEFAULT NULL COMMENT '父部门ID',
                                                `name` varchar(50) NOT NULL COMMENT '部门名称',
    `code` varchar(50) NOT NULL COMMENT '部门编码',
    `leader` varchar(50) DEFAULT NULL COMMENT '负责人',
    `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
    `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `user_id` bigint NOT NULL COMMENT '用户ID',
                                               `role_id` bigint NOT NULL COMMENT '角色ID',
                                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
                                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                               `role_id` bigint NOT NULL COMMENT '角色ID',
                                               `menu_id` bigint NOT NULL COMMENT '菜单ID',
                                               `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                               `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                               `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 用户部门关联表
CREATE TABLE IF NOT EXISTS `sys_user_department` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                                     `department_id` bigint NOT NULL COMMENT '部门ID',
                                                     `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                                     `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                                     `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
                                                     PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_department_id` (`department_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户部门关联表';
CREATE TABLE IF NOT EXISTS `sys_role_data_scope` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
                                                     `role_id` bigint NOT NULL COMMENT '角色ID',
                                                     `data_scope` tinyint NOT NULL COMMENT '数据范围（1：全部数据权限 2：自定义数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
                                                     `dept_ids` varchar(500) DEFAULT NULL COMMENT '部门ID列表',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint DEFAULT NULL COMMENT '创建人',
    `update_by` bigint DEFAULT NULL COMMENT '更新人',
    `deleted` tinyint NOT NULL DEFAULT '0' COMMENT '是否删除（0：否 1：是）',
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_role_data_scope_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色数据权限表';

-- 客户表

CREATE TABLE `t_customer` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
    `customer_name` varchar(100) NOT NULL COMMENT '客户名称',
    `customer_type` tinyint NOT NULL DEFAULT '1' COMMENT '客户类型（1：企业 2：个人）',
    `industry` varchar(50) DEFAULT NULL COMMENT '所属行业',
    `customer_level` tinyint DEFAULT '0' COMMENT '客户等级（0-5）',
    `customer_tags` varchar(200) DEFAULT NULL COMMENT '客户标签，多个标签用逗号分隔',
    `intention_level` tinyint DEFAULT '0' COMMENT '意向度（0-5）',
    `payment_status` tinyint DEFAULT '0' COMMENT '付费状态（0：未付费 1：已付费）',
    `register_time` datetime DEFAULT NULL COMMENT '企业注册时间',
    `register_address` varchar(200) DEFAULT NULL COMMENT '企业注册地址',
    `business_license` varchar(200) DEFAULT NULL COMMENT '营业执照号',
    `legal_person` varchar(50) DEFAULT NULL COMMENT '法人代表',
    `registered_capital` decimal(20,2) DEFAULT NULL COMMENT '注册资本',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1：公海 2：私海）',
    `owner_id` bigint DEFAULT NULL COMMENT '负责人ID',
    `department_id` bigint DEFAULT NULL COMMENT '所属部门ID',
    `last_follow_time` datetime DEFAULT NULL COMMENT '最近跟进时间',
    `last_follow_content` varchar(500) DEFAULT NULL COMMENT '最近跟进内容',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_owner_id` (`owner_id`),
    KEY `idx_department_id` (`department_id`),
    KEY `idx_status` (`status`),
    KEY `idx_customer_level` (`customer_level`),
    KEY `idx_intention_level` (`intention_level`),
    KEY `idx_payment_status` (`payment_status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 客户联系人表
CREATE TABLE IF NOT EXISTS `t_customer_contact` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '联系人ID',
    `customer_id` bigint NOT NULL COMMENT '客户ID',
    `name` varchar(50) NOT NULL COMMENT '联系人姓名',
    `position` varchar(50) DEFAULT NULL COMMENT '职务',
    `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
    `mobile` varchar(20) DEFAULT NULL COMMENT '手机号码',
    `email` varchar(100) DEFAULT NULL COMMENT '电子邮箱',
    `is_primary` tinyint(1) DEFAULT '0' COMMENT '是否主要联系人',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='客户联系人表';

-- 客户跟进记录表
CREATE TABLE IF NOT EXISTS `t_customer_follow` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '跟进记录ID',
    `customer_id` bigint NOT NULL COMMENT '客户ID',
    `follow_type` tinyint NOT NULL COMMENT '跟进类型（1：电话 2：邮件 3：拜访 4：其他）',
    `follow_content` varchar(500) NOT NULL COMMENT '跟进内容',
    `follow_result` varchar(500) DEFAULT NULL COMMENT '跟进结果',
    `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='客户跟进记录表';

-- 客户操作日志表
CREATE TABLE IF NOT EXISTS `t_customer_operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `customer_id` bigint NOT NULL COMMENT '客户ID',
    `operation_type` varchar(50) NOT NULL COMMENT '操作类型',
    `operation_content` varchar(500) NOT NULL COMMENT '操作内容',
    `operator_id` bigint NOT NULL COMMENT '操作人ID',
    `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='客户操作日志表';

-- 客户订单表
CREATE TABLE IF NOT EXISTS `t_customer_order` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no` varchar(50) NOT NULL COMMENT '订单编号',
    `customer_id` bigint NOT NULL COMMENT '客户ID',
    `product_id` bigint NOT NULL COMMENT '产品ID',
    `product_name` varchar(100) NOT NULL COMMENT '产品名称',
    `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `payment_status` tinyint NOT NULL DEFAULT '0' COMMENT '收款状态（0：未收款 1：已收款）',
    `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态（0：待审核 1：已通过 2：已拒绝）',
    `service_area` varchar(200) DEFAULT NULL COMMENT '服务地区',
    `order_date` datetime NOT NULL COMMENT '下单日期',
    `salesman_id` bigint NOT NULL COMMENT '销售人员ID',
    `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
    `create_time` datetime DEFAULT NULL COMMENT '创建时间',
    `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
    `update_time` datetime DEFAULT NULL COMMENT '更新时间',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_salesman_id` (`salesman_id`),
    KEY `idx_payment_status` (`payment_status`),
    KEY `idx_audit_status` (`audit_status`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COMMENT='客户订单表';

-- 订单表
CREATE TABLE IF NOT EXISTS `t_order` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` varchar(50) NOT NULL COMMENT '订单编号',
    `customer_id` bigint(20) NOT NULL COMMENT '客户ID',
    `order_amount` decimal(10,2) NOT NULL COMMENT '订单金额',
    `order_date` datetime NOT NULL COMMENT '下单日期',
    `status` tinyint(4) NOT NULL DEFAULT '1' COMMENT '订单状态：1-待处理 2-处理中 3-已完成 4-已取消',
    `salesman_id` bigint(20) NOT NULL COMMENT '销售人员ID',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_customer_id` (`customer_id`),
    KEY `idx_salesman_id` (`salesman_id`),
    KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ----------------------------
-- 字典类型表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典类型ID',
  `name` varchar(100) NOT NULL COMMENT '字典名称',
  `code` varchar(100) NOT NULL COMMENT '字典编码',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `dict_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';

-- ----------------------------
-- 字典数据表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_dict_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `type_id` bigint(20) NOT NULL COMMENT '字典类型ID',
  `label` varchar(100) NOT NULL COMMENT '字典标签',
  `value` varchar(100) NOT NULL COMMENT '字典键值',
  `sort` int(4) NOT NULL DEFAULT '0' COMMENT '字典排序',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `dict_type_id` (`type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';



