-- 清空数据（注意顺序：先清空关联表，再清空主表）
DELETE FROM `sys_user_department`;
DELETE FROM `sys_role_menu`;
DELETE FROM `sys_user_role`;
DELETE FROM `sys_department`;
DELETE FROM `sys_menu`;
DELETE FROM `sys_role`;
DELETE FROM `sys_user`;
DELETE FROM `t_customer_follow`;
DELETE FROM `t_order`;
DELETE FROM `t_customer`;
DELETE FROM `sys_dict_data`;
DELETE FROM `sys_dict_type`;

-- 重置自增ID
ALTER TABLE `sys_user_department` AUTO_INCREMENT = 1;
ALTER TABLE `sys_role_menu` AUTO_INCREMENT = 1;
ALTER TABLE `sys_user_role` AUTO_INCREMENT = 1;
ALTER TABLE `sys_department` AUTO_INCREMENT = 1;
ALTER TABLE `sys_menu` AUTO_INCREMENT = 1;
ALTER TABLE `sys_role` AUTO_INCREMENT = 1;
ALTER TABLE `sys_user` AUTO_INCREMENT = 1;
ALTER TABLE `t_customer_follow` AUTO_INCREMENT = 1;
ALTER TABLE `t_order` AUTO_INCREMENT = 1;
ALTER TABLE `t_customer` AUTO_INCREMENT = 1;
ALTER TABLE `sys_dict_data` AUTO_INCREMENT = 1;
ALTER TABLE `sys_dict_type` AUTO_INCREMENT = 1;

-- 初始化管理员用户（密码：123456）
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `email`, `phone`, `enabled`, `create_time`, `update_time`, `deleted`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@example.com', '13800138000', 1, NOW(), NOW(), 0);

-- 初始化系统角色
INSERT INTO `sys_role` (`name`, `code`, `description`, `sort`, `enabled`, `create_time`, `update_time`, `deleted`)
VALUES
('超级管理员', 'ROLE_ADMIN', '系统超级管理员', 1, 1, NOW(), NOW(), 0),
('普通用户', 'ROLE_USER', '普通用户', 2, 1, NOW(), NOW(), 0);

-- 初始化系统菜单
INSERT INTO `sys_menu` (`id`, `name`, `parent_id`, `sort`, `path`, `component`, `type`, `permission`, `icon`, `enabled`, `create_time`, `update_time`, `deleted`)
VALUES
-- 系统管理
(1, '系统管理', 0, 1, 'system', NULL, 1, NULL, 'system', 1, NOW(), NOW(), 0),
-- 用户管理
(2, '用户管理', 1, 1, '/admin/system/user', 'system/user/index', 2, 'system:user:list', 'user', 1, NOW(), NOW(), 0),
(3, '用户查询', 2, 1, NULL, NULL, 3, 'system:user:query', NULL, 1, NOW(), NOW(), 0),
(4, '用户新增', 2, 2, NULL, NULL, 3, 'system:user:add', NULL, 1, NOW(), NOW(), 0),
(5, '用户修改', 2, 3, NULL, NULL, 3, 'system:user:edit', NULL, 1, NOW(), NOW(), 0),
(6, '用户删除', 2, 4, NULL, NULL, 3, 'system:user:delete', NULL, 1, NOW(), NOW(), 0),
(7, '用户导出', 2, 5, NULL, NULL, 3, 'system:user:export', NULL, 1, NOW(), NOW(), 0),
(8, '重置密码', 2, 6, NULL, NULL, 3, 'system:user:resetPwd', NULL, 1, NOW(), NOW(), 0),
-- 角色管理
(9, '角色管理', 1, 2, '/admin/system/role', 'system/role/index', 2, 'system:role:list', 'peoples', 1, NOW(), NOW(), 0),
(10, '角色查询', 9, 1, NULL, NULL, 3, 'system:role:query', NULL, 1, NOW(), NOW(), 0),
(11, '角色新增', 9, 2, NULL, NULL, 3, 'system:role:add', NULL, 1, NOW(), NOW(), 0),
(12, '角色修改', 9, 3, NULL, NULL, 3, 'system:role:edit', NULL, 1, NOW(), NOW(), 0),
(13, '角色删除', 9, 4, NULL, NULL, 3, 'system:role:delete', NULL, 1, NOW(), NOW(), 0),
(14, '角色导出', 9, 5, NULL, NULL, 3, 'system:role:export', NULL, 1, NOW(), NOW(), 0),
(15, '分配菜单权限', 9, 6, NULL, NULL, 3, 'system:role:assignMenu', NULL, 1, NOW(), NOW(), 0),
(16, '分配数据权限', 9, 7, NULL, NULL, 3, 'system:role:dataScope', NULL, 1, NOW(), NOW(), 0),
-- 菜单管理
(17, '菜单管理', 1, 3, '/admin/system/menu', 'system/menu/index', 2, 'system:menu:list', 'tree-table', 1, NOW(), NOW(), 0),
(18, '菜单查询', 17, 1, NULL, NULL, 3, 'system:menu:query', NULL, 1, NOW(), NOW(), 0),
(19, '菜单新增', 17, 2, NULL, NULL, 3, 'system:menu:add', NULL, 1, NOW(), NOW(), 0),
(20, '菜单修改', 17, 3, NULL, NULL, 3, 'system:menu:edit', NULL, 1, NOW(), NOW(), 0),
(21, '菜单删除', 17, 4, NULL, NULL, 3, 'system:menu:delete', NULL, 1, NOW(), NOW(), 0),
-- 部门管理
(22, '部门管理', 1, 4, '/admin/system/department', 'system/department/index', 2, 'system:department:list', 'tree', 1, NOW(), NOW(), 0),
(23, '部门查询', 22, 1, NULL, NULL, 3, 'system:department:query', NULL, 1, NOW(), NOW(), 0),
(24, '部门新增', 22, 2, NULL, NULL, 3, 'system:department:add', NULL, 1, NOW(), NOW(), 0),
(25, '部门修改', 22, 3, NULL, NULL, 3, 'system:department:edit', NULL, 1, NOW(), NOW(), 0),
(26, '部门删除', 22, 4, NULL, NULL, 3, 'system:department:delete', NULL, 1, NOW(), NOW(), 0),
-- 字典管理
(27, '字典管理', 1, 5, '/admin/system/dict', 'system/dict/index', 2, 'system:dict:list', 'dict', 1, NOW(), NOW(), 0),
(28, '字典查询', 27, 1, NULL, NULL, 3, 'system:dict:query', NULL, 1, NOW(), NOW(), 0),
(29, '字典新增', 27, 2, NULL, NULL, 3, 'system:dict:add', NULL, 1, NOW(), NOW(), 0),
(30, '字典修改', 27, 3, NULL, NULL, 3, 'system:dict:edit', NULL, 1, NOW(), NOW(), 0),
(31, '字典删除', 27, 4, NULL, NULL, 3, 'system:dict:delete', NULL, 1, NOW(), NOW(), 0),
(32, '字典导出', 27, 5, NULL, NULL, 3, 'system:dict:export', NULL, 1, NOW(), NOW(), 0),
-- 公海客户
(34, '公海客户', 33, 1, '/sale/public-sea', 'sale/public-sea/index', 2, 'sale:customer:public:list', 'peoples', 1, NOW(), NOW(), 0),
(35, '客户查询', 34, 1, NULL, NULL, 3, 'sale:customer:public:query', NULL, 1, NOW(), NOW(), 0),
(36, '客户认领', 34, 2, NULL, NULL, 3, 'sale:customer:claim', NULL, 1, NOW(), NOW(), 0),
-- 私海客户
(37, '私海客户', 33, 2, '/sale/private-sea', 'sale/private-sea/index', 2, 'sale:customer:private:list', 'user', 1, NOW(), NOW(), 0),
(38, '客户查询', 37, 1, NULL, NULL, 3, 'sale:customer:private:query', NULL, 1, NOW(), NOW(), 0),
(39, '客户新增', 37, 2, NULL, NULL, 3, 'sale:customer:add', NULL, 1, NOW(), NOW(), 0),
(40, '客户修改', 37, 3, NULL, NULL, 3, 'sale:customer:edit', NULL, 1, NOW(), NOW(), 0),
(41, '客户删除', 37, 4, NULL, NULL, 3, 'sale:customer:delete', NULL, 1, NOW(), NOW(), 0),
(42, '客户释放', 37, 5, NULL, NULL, 3, 'sale:customer:release', NULL, 1, NOW(), NOW(), 0),

-- 订单管理
(113, '订单管理', 0, 3, 'order', NULL, 1, NULL, 'shopping', 1, NOW(), NOW(), 0),
-- 订单列表
(114, '订单列表', 113, 1, '/order/list', 'order/OrderList', 2, 'order:list', 'list', 1, NOW(), NOW(), 0),
(115, '订单查询', 114, 1, NULL, NULL, 3, 'order:query', NULL, 1, NOW(), NOW(), 0),
(116, '订单新增', 114, 2, NULL, NULL, 3, 'order:add', NULL, 1, NOW(), NOW(), 0),
(117, '订单修改', 114, 3, NULL, NULL, 3, 'order:edit', NULL, 1, NOW(), NOW(), 0),
(118, '订单删除', 114, 4, NULL, NULL, 3, 'order:delete', NULL, 1, NOW(), NOW(), 0),
-- 回款管理
(119, '回款管理', 113, 2, '/order/payment', 'order/PaymentList', 2, 'order:payment:list', 'money', 1, NOW(), NOW(), 0),
(120, '回款查询', 119, 1, NULL, NULL, 3, 'order:payment:query', NULL, 1, NOW(), NOW(), 0),
(121, '回款新增', 119, 2, NULL, NULL, 3, 'order:payment:add', NULL, 1, NOW(), NOW(), 0),
(122, '回款修改', 119, 3, NULL, NULL, 3, 'order:payment:edit', NULL, 1, NOW(), NOW(), 0),
(123, '回款删除', 119, 4, NULL, NULL, 3, 'order:payment:delete', NULL, 1, NOW(), NOW(), 0),

-- 初始化部门
INSERT INTO `sys_department` (`parent_id`, `name`, `code`, `leader`, `phone`, `email`, `sort`, `enabled`, `create_time`, `update_time`, `deleted`)
VALUES
(NULL, '总公司', 'HQ', '张三', '13800138001', 'hq@example.com', 1, 1, NOW(), NOW(), 0),
(1, '研发部', 'DEV', '李四', '13800138002', 'dev@example.com', 1, 1, NOW(), NOW(), 0),
(1, '市场部', 'MKT', '王五', '13800138003', 'mkt@example.com', 2, 1, NOW(), NOW(), 0),
(1, '财务部', 'FIN', '赵六', '13800138004', 'fin@example.com', 3, 1, NOW(), NOW(), 0);

-- 初始化用户角色关联
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `create_time`, `update_time`, `deleted`)
VALUES (1, 1, NOW(), NOW(), 0);

-- 初始化角色菜单关联（超级管理员拥有所有菜单权限）
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`, `create_time`, `update_time`, `deleted`)
SELECT 1, id, NOW(), NOW(), 0 FROM sys_menu;

-- 初始化用户部门关联
INSERT INTO `sys_user_department` (`user_id`, `department_id`, `create_time`, `update_time`, `deleted`)
VALUES (1, 1, NOW(), NOW(), 0);

-- 插入测试客户数据
INSERT INTO `t_customer` (
    `customer_name`,
    `customer_type`,
    `industry`,
    `customer_level`,
    `customer_tags`,
    `intention_level`,
    `payment_status`,
    `register_time`,
    `register_address`,
    `business_license`,
    `legal_person`,
    `registered_capital`,
    `status`,
    `owner_id`,
    `department_id`,
    `create_by`,
    `create_time`,
    `update_by`,
    `update_time`,
    `remark`
) VALUES
('阿里巴巴科技有限公司', 1, '互联网', 5, '重点客户,高价值', 5, 1, '1999-01-01 00:00:00', '杭州市西湖区', '91330100716105437B', '马云', 1000000000.00, 1, NULL, 2, 'admin', NOW(), 'admin', NOW(), '互联网巨头'),
('腾讯科技（深圳）有限公司', 1, '互联网', 5, '重点客户,高价值', 5, 1, '1998-11-11 00:00:00', '深圳市南山区', '91440300708461136T', '马化腾', 2000000000.00, 1, NULL, 2, 'admin', NOW(), 'admin', NOW(), '互联网巨头'),
('百度在线网络技术（北京）有限公司', 1, '互联网', 4, '重点客户', 4, 1, '2000-01-01 00:00:00', '北京市海淀区', '91110000802100433B', '李彦宏', 500000000.00, 2, 1, 2, 'admin', NOW(), 'admin', NOW(), '互联网巨头'),
('小米科技有限责任公司', 1, '智能硬件', 4, '重点客户,高价值', 4, 1, '2010-04-06 00:00:00', '北京市海淀区', '9111000059967708XG', '雷军', 100000000.00, 2, 1, 2, 'admin', NOW(), 'admin', NOW(), '智能硬件厂商'),
('华为技术有限公司', 1, '通信设备', 5, '重点客户,高价值', 5, 1, '1987-09-15 00:00:00', '深圳市龙岗区', '914403001922038216', '任正非', 5000000000.00, 2, 1, 2, 'admin', NOW(), 'admin', NOW(), '通信设备巨头');

-- 插入测试订单数据
INSERT INTO `t_order` (`order_no`, `customer_id`, `order_amount`, `order_date`, `status`, `salesman_id`, `remark`) VALUES
('ORD202403010001', 1, 10000.00, '2024-03-01 10:00:00', 1, 1, '测试订单1'),
('ORD202403010002', 2, 20000.00, '2024-03-01 11:00:00', 2, 1, '测试订单2'),
('ORD202403010003', 3, 30000.00, '2024-03-01 12:00:00', 3, 1, '测试订单3');

-- 插入测试客户跟进记录
INSERT INTO `t_customer_follow` (`customer_id`, `follow_type`, `follow_content`, `follow_result`, `next_follow_time`, `operator_id`) VALUES
(1, 1, '电话沟通产品需求', '客户表示感兴趣，需要进一步了解', '2024-03-08 10:00:00', 1),
(2, 2, '发送产品介绍邮件', '客户已收到邮件，等待回复', '2024-03-09 14:00:00', 1),
(3, 3, '上门拜访', '客户对产品很满意，准备下单', '2024-03-10 15:00:00', 1);

-- ----------------------------
-- 初始化字典类型数据
-- ----------------------------
INSERT INTO `sys_dict_type` (`name`, `code`, `status`, `remark`, `create_time`) VALUES
('行业字典', 'industry', 1, '客户所属行业', NOW()),
('客户等级字典', 'customer_level', 1, '客户等级', NOW()),
('客户意向字典', 'customer_intention', 1, '客户意向度', NOW()),
('客户标签字典', 'customer_tag', 1, '客户标签', NOW());

-- ----------------------------
-- 初始化行业字典数据
-- ----------------------------
INSERT INTO `sys_dict_data` (`type_id`, `label`, `value`, `sort`, `status`, `create_time`) VALUES
(1, '互联网', 'internet', 1, 1, NOW()),
(1, '金融', 'finance', 2, 1, NOW()),
(1, '教育', 'education', 3, 1, NOW()),
(1, '医疗', 'medical', 4, 1, NOW()),
(1, '制造业', 'manufacturing', 5, 1, NOW());

-- ----------------------------
-- 初始化客户等级字典数据
-- ----------------------------
INSERT INTO `sys_dict_data` (`type_id`, `label`, `value`, `sort`, `status`, `create_time`) VALUES
(2, 'A级', 'A', 1, 1, NOW()),
(2, 'B级', 'B', 2, 1, NOW()),
(2, 'C级', 'C', 3, 1, NOW()),
(2, 'D级', 'D', 4, 1, NOW());

-- ----------------------------
-- 初始化客户意向字典数据
-- ----------------------------
INSERT INTO `sys_dict_data` (`type_id`, `label`, `value`, `sort`, `status`, `create_time`) VALUES
(3, '高', 'high', 1, 1, NOW()),
(3, '中', 'medium', 2, 1, NOW()),
(3, '低', 'low', 3, 1, NOW());

-- ----------------------------
-- 初始化客户标签字典数据
-- ----------------------------
INSERT INTO `sys_dict_data` (`type_id`, `label`, `value`, `sort`, `status`, `create_time`) VALUES
(4, '重要客户', 'important', 1, 1, NOW()),
(4, '潜在客户', 'potential', 2, 1, NOW()),
(4, '老客户', 'old', 3, 1, NOW()),
(4, '新客户', 'new', 4, 1, NOW());

-- 添加客户来源字典类型
INSERT INTO sys_dict_type (id, name, code, status, remark, create_by, create_time, update_by, update_time)
VALUES (100, '客户来源', 'customer_source', 1, '客户来源字典', 'admin', NOW(), 'admin', NOW());

-- 添加客户来源字典数据
INSERT INTO sys_dict_data (id, type_id, label, value, sort, status, remark, create_by, create_time, update_by, update_time) VALUES
(1001, 100, '小程序注册', '1', 1, 1, '小程序注册的客户', 'admin', NOW(), 'admin', NOW()),
(1002, 100, '引流', '2', 2, 1, '引流渠道的客户', 'admin', NOW(), 'admin', NOW()),
(1003, 100, '抖音', '3', 3, 1, '抖音平台的客户', 'admin', NOW(), 'admin', NOW()),
(1004, 100, '小红书', '4', 4, 1, '小红书平台的客户', 'admin', NOW(), 'admin', NOW()),
(1005, 100, '官网', '5', 5, 1, '官网注册的客户', 'admin', NOW(), 'admin', NOW()),
(1006, 100, '客户介绍', '6', 6, 1, '老客户介绍的客户', 'admin', NOW(), 'admin', NOW());

-- 添加跟进方式字典类型
INSERT INTO sys_dict_type (id, name, code, status, remark, create_by, create_time, update_by, update_time)
VALUES (101, '跟进方式', 'follow_up_method', 1, '客户跟进方式字典', 'admin', NOW(), 'admin', NOW());

-- 添加跟进方式字典数据
INSERT INTO sys_dict_data (id, type_id, label, value, sort, status, remark, create_by, create_time, update_by, update_time) VALUES
(1101, 101, '电话', '1', 1, 1, '电话跟进', 'admin', NOW(), 'admin', NOW()),
(1102, 101, '微信', '2', 2, 1, '微信跟进', 'admin', NOW(), 'admin', NOW());
