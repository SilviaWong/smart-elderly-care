-- 智慧养老管理系统 阶段 2 基础数据与系统管理初始化种子脚本
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 初始化机构
INSERT INTO sys_org (id, org_code, org_name, org_type, contact_name, contact_phone, address, status)
VALUES
(1, 'ORG001', '阳光馨园康养中心', 'NURSING_HOME', '张院长', '13800138001', '北京市海淀区康养路 88 号', 'ACTIVE'),
(2, 'ORG002', '颐和长者之家', 'NURSING_HOME', '李副院长', '13900139002', '上海市长宁区夕阳红大道 66 号', 'ACTIVE')
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name);

-- 2. 初始化权限资源树
INSERT INTO sys_permission (id, parent_id, permission_code, permission_name, permission_type, path)
VALUES
(1, NULL, 'dashboard:view', '运营工作台', 'MENU', '/dashboard'),
(2, NULL, 'system', '系统管理', 'MENU', '/system'),
(3, 2, 'system:user:view', '用户查询', 'BUTTON', '/api/v1/system/users'),
(4, 2, 'system:user:edit', '用户编辑', 'BUTTON', '/api/v1/system/users/*'),
(5, 2, 'system:role:view', '角色查询', 'BUTTON', '/api/v1/system/roles'),
(6, 2, 'system:role:edit', '角色编辑', 'BUTTON', '/api/v1/system/roles/*'),
(7, NULL, 'elder', '老人档案', 'MENU', '/elder'),
(8, 7, 'elder:view', '老人查询', 'BUTTON', '/api/v1/elders'),
(9, 7, 'elder:edit', '老人建档编辑', 'BUTTON', '/api/v1/elders/*'),
(10, NULL, 'admission', '入住与床位', 'MENU', '/admission'),
(11, 10, 'admission:view', '床位入住查询', 'BUTTON', '/api/v1/admissions'),
(12, 10, 'admission:operate', '入住调床退住办理', 'BUTTON', '/api/v1/admissions/*'),
(13, NULL, 'serviceorder', '服务工单', 'MENU', '/serviceorder'),
(14, NULL, 'health', '健康管理', 'MENU', '/health'),
(15, NULL, 'care', '护理管理', 'MENU', '/care'),
(16, NULL, 'dining', '餐饮管理', 'MENU', '/dining'),
(17, NULL, 'finance', '财务管理', 'MENU', '/finance'),
(18, NULL, 'risk', '安全风险', 'MENU', '/risk'),
(19, NULL, 'report', '统计报表', 'MENU', '/report')
ON DUPLICATE KEY UPDATE permission_name = VALUES(permission_name);

-- 3. 初始化角色
-- BCrypt 密码统一为: 123456 -> $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
INSERT INTO sys_role (id, org_id, role_code, role_name, description)
VALUES
(1, 1, 'ORG_ADMIN', '机构管理员', '具备机构全部业务与配置管理权限'),
(2, 1, 'NURSE_DIRECTOR', '护理主管', '负责护理计划、任务调度与质量审核'),
(3, 1, 'CAREGIVER', '一线护理员', '负责执行日常护理与工单任务'),
(4, 2, 'ORG_ADMIN', '机构管理员', '颐和长者之家管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 4. 角色权限关联
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19),
(2, 1), (2, 7), (2, 8), (2, 10), (2, 11), (2, 14), (2, 15),
(3, 1), (3, 7), (3, 8), (3, 13), (3, 15),
(4, 1), (4, 2), (4, 3), (4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15), (4, 16), (4, 17), (4, 18), (4, 19)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

-- 5. 初始化用户 (密码均为 123456)
INSERT INTO sys_user (id, org_id, username, password_hash, real_name, phone, email, status)
VALUES
(1, 1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '阳光张主管', '13800000001', 'admin@org1.com', 'ACTIVE'),
(2, 1, 'caregiver1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '王护理员', '13800000002', 'caregiver1@org1.com', 'ACTIVE'),
(3, 2, 'org2_admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '颐和李主管', '13900000001', 'admin@org2.com', 'ACTIVE')
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name);

-- 6. 用户角色关联
INSERT INTO sys_user_role (user_id, role_id)
VALUES
(1, 1),
(2, 3),
(3, 4)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);
