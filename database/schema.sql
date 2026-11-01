-- 智慧养老管理系统 MySQL 8.0 DDL
-- 版本：V1.0 | 引擎：InnoDB | 字符集：utf8mb4
-- 说明：这是本次复刻方案的初始数据库结构，不是原始源码数据库导出。
CREATE DATABASE IF NOT EXISTS smart_elderly_care
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE smart_elderly_care;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE sys_org (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_code VARCHAR(40) NOT NULL,
  org_name VARCHAR(120) NOT NULL,
  org_type VARCHAR(30) NOT NULL DEFAULT 'NURSING_HOME',
  contact_name VARCHAR(60),
  contact_phone VARCHAR(30),
  address VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_org_code (org_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='养老机构';

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT,
  username VARCHAR(60) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(60) NOT NULL,
  phone VARCHAR(30),
  email VARCHAR(120),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_login_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_name (username),
  KEY idx_user_org_status (org_id, status),
  CONSTRAINT fk_user_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户';

CREATE TABLE sys_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT,
  role_code VARCHAR(60) NOT NULL,
  role_name VARCHAR(80) NOT NULL,
  description VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_org_code (org_id, role_code),
  CONSTRAINT fk_role_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT,
  permission_code VARCHAR(100) NOT NULL,
  permission_name VARCHAR(100) NOT NULL,
  permission_type VARCHAR(20) NOT NULL DEFAULT 'MENU',
  path VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_permission_code (permission_code),
  KEY idx_permission_parent (parent_id),
  CONSTRAINT fk_permission_parent FOREIGN KEY (parent_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限资源';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系';

CREATE TABLE sys_role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES sys_permission(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关系';

CREATE TABLE sys_operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT,
  user_id BIGINT,
  module VARCHAR(60) NOT NULL,
  action VARCHAR(80) NOT NULL,
  biz_type VARCHAR(60),
  biz_id BIGINT,
  request_uri VARCHAR(255),
  request_ip VARCHAR(64),
  detail_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_op_org_time (org_id, created_at),
  KEY idx_op_biz (biz_type, biz_id),
  CONSTRAINT fk_op_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_op_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志';

CREATE TABLE building (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  building_name VARCHAR(80) NOT NULL,
  building_code VARCHAR(40) NOT NULL,
  address VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_building_code (org_id, building_code),
  CONSTRAINT fk_building_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='楼栋';

CREATE TABLE room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  building_id BIGINT NOT NULL,
  floor_no VARCHAR(20),
  room_no VARCHAR(40) NOT NULL,
  room_type VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_no (org_id, building_id, room_no),
  KEY idx_room_building (building_id),
  CONSTRAINT fk_room_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_room_building FOREIGN KEY (building_id) REFERENCES building(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='房间';

CREATE TABLE bed (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  bed_no VARCHAR(40) NOT NULL,
  bed_type VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_bed_no (org_id, room_id, bed_no),
  KEY idx_bed_status (org_id, status),
  CONSTRAINT fk_bed_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_bed_room FOREIGN KEY (room_id) REFERENCES room(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床位';

CREATE TABLE elder (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  elder_no VARCHAR(40) NOT NULL,
  name VARCHAR(80) NOT NULL,
  gender VARCHAR(10),
  birth_date DATE,
  id_type VARCHAR(30),
  id_no_ciphertext VARCHAR(512),
  id_no_hash CHAR(64),
  phone VARCHAR(30),
  address VARCHAR(255),
  photo_url VARCHAR(500),
  blood_type VARCHAR(10),
  allergy_desc TEXT,
  chronic_disease_desc TEXT,
  medication_desc TEXT,
  diet_taboo TEXT,
  mobility_level VARCHAR(30),
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(500),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_elder_no (org_id, elder_no),
  KEY idx_elder_name (org_id, name),
  KEY idx_elder_id_hash (org_id, id_no_hash),
  KEY idx_elder_status (org_id, status),
  CONSTRAINT fk_elder_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_elder_creator FOREIGN KEY (created_by) REFERENCES sys_user(id),
  CONSTRAINT fk_elder_updater FOREIGN KEY (updated_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人档案';

CREATE TABLE elder_contact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  contact_name VARCHAR(80) NOT NULL,
  relationship VARCHAR(40),
  phone VARCHAR(30) NOT NULL,
  is_primary TINYINT NOT NULL DEFAULT 0,
  is_emergency TINYINT NOT NULL DEFAULT 0,
  address VARCHAR(255),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_contact_elder (org_id, elder_id),
  CONSTRAINT fk_contact_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_contact_elder FOREIGN KEY (elder_id) REFERENCES elder(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人联系人';

CREATE TABLE elder_assessment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  assessment_type VARCHAR(40) NOT NULL,
  assessment_date DATE NOT NULL,
  score DECIMAL(8,2),
  result_level VARCHAR(40),
  detail_json JSON,
  assessor_id BIGINT,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_assessment_elder_date (org_id, elder_id, assessment_date),
  CONSTRAINT fk_assess_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_assess_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_assess_user FOREIGN KEY (assessor_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='老人评估记录';

CREATE TABLE admission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  admission_no VARCHAR(50) NOT NULL,
  elder_id BIGINT NOT NULL,
  bed_id BIGINT NOT NULL,
  contract_no VARCHAR(60),
  admission_date DATE NOT NULL,
  planned_discharge_date DATE,
  actual_discharge_date DATE,
  care_level_code VARCHAR(40),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  deposit_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  discharge_reason VARCHAR(500),
  operator_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_admission_no (org_id, admission_no),
  KEY idx_admission_elder_status (org_id, elder_id, status),
  KEY idx_admission_bed_status (org_id, bed_id, status),
  CONSTRAINT fk_admission_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_admission_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_admission_bed FOREIGN KEY (bed_id) REFERENCES bed(id),
  CONSTRAINT fk_admission_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入住记录';

CREATE TABLE bed_change_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  admission_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  from_bed_id BIGINT,
  to_bed_id BIGINT,
  change_type VARCHAR(20) NOT NULL,
  change_reason VARCHAR(500),
  changed_by BIGINT,
  changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_bed_change_admission (org_id, admission_id, changed_at),
  CONSTRAINT fk_bcl_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_bcl_admission FOREIGN KEY (admission_id) REFERENCES admission(id),
  CONSTRAINT fk_bcl_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_bcl_from_bed FOREIGN KEY (from_bed_id) REFERENCES bed(id),
  CONSTRAINT fk_bcl_to_bed FOREIGN KEY (to_bed_id) REFERENCES bed(id),
  CONSTRAINT fk_bcl_user FOREIGN KEY (changed_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='床位变更记录';

CREATE TABLE service_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  item_code VARCHAR(40) NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  category VARCHAR(40),
  unit VARCHAR(20),
  standard_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  need_confirm TINYINT NOT NULL DEFAULT 0,
  description VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_item (org_id, item_code),
  CONSTRAINT fk_service_item_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务项目';

CREATE TABLE service_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  elder_id BIGINT NOT NULL,
  admission_id BIGINT,
  service_item_id BIGINT NOT NULL,
  source_type VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
  priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
  expected_start_at DATETIME,
  assignee_id BIGINT,
  creator_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ASSIGN',
  request_desc VARCHAR(1000),
  execution_result TEXT,
  completed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_service_order_no (org_id, order_no),
  KEY idx_service_status_time (org_id, status, created_at),
  KEY idx_service_assignee (org_id, assignee_id, status),
  CONSTRAINT fk_so_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_so_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_so_admission FOREIGN KEY (admission_id) REFERENCES admission(id),
  CONSTRAINT fk_so_item FOREIGN KEY (service_item_id) REFERENCES service_item(id),
  CONSTRAINT fk_so_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id),
  CONSTRAINT fk_so_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务工单';

CREATE TABLE service_order_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30) NOT NULL,
  action VARCHAR(40) NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(1000),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sol_order_time (org_id, order_id, created_at),
  CONSTRAINT fk_sol_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_sol_order FOREIGN KEY (order_id) REFERENCES service_order(id),
  CONSTRAINT fk_sol_user FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流转记录';

CREATE TABLE health_metric (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  metric_code VARCHAR(40) NOT NULL,
  metric_name VARCHAR(80) NOT NULL,
  unit VARCHAR(20),
  value_type VARCHAR(20) NOT NULL DEFAULT 'NUMBER',
  description VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_health_metric (org_id, metric_code),
  CONSTRAINT fk_metric_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康指标定义';

CREATE TABLE health_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  metric_id BIGINT NOT NULL,
  metric_value DECIMAL(10,3) NOT NULL,
  unit_snapshot VARCHAR(20),
  measured_at DATETIME NOT NULL,
  source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
  source_device_id BIGINT,
  recorder_id BIGINT,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_health_elder_metric_time (org_id, elder_id, metric_id, measured_at),
  CONSTRAINT fk_hr_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_hr_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_hr_metric FOREIGN KEY (metric_id) REFERENCES health_metric(id),
  CONSTRAINT fk_hr_recorder FOREIGN KEY (recorder_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康记录';

CREATE TABLE health_alert_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  metric_id BIGINT NOT NULL,
  rule_name VARCHAR(100) NOT NULL,
  condition_type VARCHAR(20) NOT NULL,
  min_value DECIMAL(10,3),
  max_value DECIMAL(10,3),
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  dedup_minutes INT NOT NULL DEFAULT 30,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_alert_rule_metric (org_id, metric_id, enabled),
  CONSTRAINT fk_har_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_har_metric FOREIGN KEY (metric_id) REFERENCES health_metric(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康预警规则';

CREATE TABLE health_alert (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  health_record_id BIGINT NOT NULL,
  rule_id BIGINT NOT NULL,
  alert_no VARCHAR(50) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  alert_value DECIMAL(10,3) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  assignee_id BIGINT,
  handled_at DATETIME,
  handle_result TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_health_alert_no (org_id, alert_no),
  KEY idx_health_alert_status (org_id, status, created_at),
  CONSTRAINT fk_ha_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_ha_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_ha_record FOREIGN KEY (health_record_id) REFERENCES health_record(id),
  CONSTRAINT fk_ha_rule FOREIGN KEY (rule_id) REFERENCES health_alert_rule(id),
  CONSTRAINT fk_ha_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康预警';

CREATE TABLE care_level (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  level_code VARCHAR(40) NOT NULL,
  level_name VARCHAR(80) NOT NULL,
  description VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_care_level (org_id, level_code),
  CONSTRAINT fk_care_level_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理等级';

CREATE TABLE care_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  item_code VARCHAR(40) NOT NULL,
  item_name VARCHAR(100) NOT NULL,
  category VARCHAR(40),
  description VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_care_item (org_id, item_code),
  CONSTRAINT fk_care_item_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理项目';

CREATE TABLE care_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  plan_no VARCHAR(50) NOT NULL,
  elder_id BIGINT NOT NULL,
  care_level_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  creator_id BIGINT NOT NULL,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_care_plan_no (org_id, plan_no),
  KEY idx_care_plan_elder (org_id, elder_id, status),
  CONSTRAINT fk_cp_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_cp_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_cp_level FOREIGN KEY (care_level_id) REFERENCES care_level(id),
  CONSTRAINT fk_cp_creator FOREIGN KEY (creator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理计划';

CREATE TABLE care_plan_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  plan_id BIGINT NOT NULL,
  care_item_id BIGINT NOT NULL,
  frequency_type VARCHAR(20) NOT NULL,
  frequency_value VARCHAR(100),
  time_window_start TIME,
  time_window_end TIME,
  assignee_id BIGINT,
  enabled TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_cpi_plan (org_id, plan_id),
  CONSTRAINT fk_cpi_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_cpi_plan FOREIGN KEY (plan_id) REFERENCES care_plan(id),
  CONSTRAINT fk_cpi_item FOREIGN KEY (care_item_id) REFERENCES care_item(id),
  CONSTRAINT fk_cpi_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理计划明细';

CREATE TABLE care_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  task_no VARCHAR(50) NOT NULL,
  plan_id BIGINT NOT NULL,
  plan_item_id BIGINT NOT NULL,
  elder_id BIGINT NOT NULL,
  task_date DATE NOT NULL,
  scheduled_at DATETIME,
  assignee_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  idempotency_key VARCHAR(120) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_care_task_no (org_id, task_no),
  UNIQUE KEY uk_care_task_idem (org_id, idempotency_key),
  KEY idx_care_task_assignee (org_id, assignee_id, task_date, status),
  CONSTRAINT fk_ct_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_ct_plan FOREIGN KEY (plan_id) REFERENCES care_plan(id),
  CONSTRAINT fk_ct_plan_item FOREIGN KEY (plan_item_id) REFERENCES care_plan_item(id),
  CONSTRAINT fk_ct_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_ct_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理任务实例';

CREATE TABLE care_task_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  executor_id BIGINT NOT NULL,
  executed_at DATETIME NOT NULL,
  result_code VARCHAR(30) NOT NULL,
  result_desc TEXT,
  attachment_json JSON,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ctr_task (org_id, task_id, executed_at),
  CONSTRAINT fk_ctr_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_ctr_task FOREIGN KEY (task_id) REFERENCES care_task(id),
  CONSTRAINT fk_ctr_executor FOREIGN KEY (executor_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='护理任务执行记录';

CREATE TABLE dish (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  dish_code VARCHAR(40) NOT NULL,
  dish_name VARCHAR(100) NOT NULL,
  category VARCHAR(40),
  price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  allergens VARCHAR(500),
  description VARCHAR(500),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_dish_code (org_id, dish_code),
  CONSTRAINT fk_dish_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品';

CREATE TABLE meal_menu (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  menu_date DATE NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_meal_menu (org_id, menu_date, meal_type),
  CONSTRAINT fk_menu_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_menu_user FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日菜单';

CREATE TABLE meal_menu_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  menu_id BIGINT NOT NULL,
  dish_id BIGINT NOT NULL,
  price_snapshot DECIMAL(12,2) NOT NULL,
  KEY idx_menu_item_menu (menu_id),
  CONSTRAINT fk_mmi_menu FOREIGN KEY (menu_id) REFERENCES meal_menu(id),
  CONSTRAINT fk_mmi_dish FOREIGN KEY (dish_id) REFERENCES dish(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单菜品';

CREATE TABLE meal_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  order_no VARCHAR(50) NOT NULL,
  elder_id BIGINT NOT NULL,
  order_date DATE NOT NULL,
  meal_type VARCHAR(20) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
  ordered_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_meal_order_no (org_id, order_no),
  KEY idx_meal_order_elder_date (org_id, elder_id, order_date),
  CONSTRAINT fk_mo_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_mo_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_mo_user FOREIGN KEY (ordered_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订餐订单';

CREATE TABLE meal_order_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  dish_id BIGINT,
  dish_name_snapshot VARCHAR(100) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  unit_price DECIMAL(12,2) NOT NULL,
  line_amount DECIMAL(12,2) NOT NULL,
  KEY idx_meal_order_item (order_id),
  CONSTRAINT fk_moi_order FOREIGN KEY (order_id) REFERENCES meal_order(id),
  CONSTRAINT fk_moi_dish FOREIGN KEY (dish_id) REFERENCES dish(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订餐明细';

CREATE TABLE fee_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  fee_code VARCHAR(40) NOT NULL,
  fee_name VARCHAR(100) NOT NULL,
  fee_category VARCHAR(40),
  billing_unit VARCHAR(20),
  default_price DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  UNIQUE KEY uk_fee_item (org_id, fee_code),
  CONSTRAINT fk_fee_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用项目';

CREATE TABLE billing (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  bill_no VARCHAR(50) NOT NULL,
  elder_id BIGINT NOT NULL,
  admission_id BIGINT,
  bill_month CHAR(7) NOT NULL,
  bill_date DATE NOT NULL,
  due_date DATE,
  total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  paid_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  remark VARCHAR(500),
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_billing_no (org_id, bill_no),
  KEY idx_billing_elder_month (org_id, elder_id, bill_month),
  KEY idx_billing_status_due (org_id, status, due_date),
  CONSTRAINT fk_billing_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_billing_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_billing_admission FOREIGN KEY (admission_id) REFERENCES admission(id),
  CONSTRAINT fk_billing_user FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单';

CREATE TABLE billing_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  billing_id BIGINT NOT NULL,
  fee_item_id BIGINT,
  item_name_snapshot VARCHAR(100) NOT NULL,
  period_start DATE,
  period_end DATE,
  quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  KEY idx_billing_item (billing_id),
  CONSTRAINT fk_bi_billing FOREIGN KEY (billing_id) REFERENCES billing(id),
  CONSTRAINT fk_bi_fee FOREIGN KEY (fee_item_id) REFERENCES fee_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细';

CREATE TABLE payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  payment_no VARCHAR(60) NOT NULL,
  billing_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(30) NOT NULL,
  transaction_no VARCHAR(100),
  idempotency_key VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  paid_at DATETIME NOT NULL,
  operator_id BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_payment_no (org_id, payment_no),
  UNIQUE KEY uk_payment_idem (org_id, idempotency_key),
  KEY idx_payment_billing (billing_id, status),
  CONSTRAINT fk_payment_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_payment_billing FOREIGN KEY (billing_id) REFERENCES billing(id),
  CONSTRAINT fk_payment_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款记录';

CREATE TABLE refund_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  refund_no VARCHAR(60) NOT NULL,
  payment_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  approved_by BIGINT,
  refunded_at DATETIME,
  created_by BIGINT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refund_no (org_id, refund_no),
  KEY idx_refund_payment (payment_id, status),
  CONSTRAINT fk_refund_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment_record(id),
  CONSTRAINT fk_refund_approver FOREIGN KEY (approved_by) REFERENCES sys_user(id),
  CONSTRAINT fk_refund_creator FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款记录';

CREATE TABLE device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  device_code VARCHAR(60) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(40) NOT NULL,
  vendor VARCHAR(100),
  model VARCHAR(100),
  serial_no VARCHAR(100),
  install_location VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  last_online_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_code (org_id, device_code),
  KEY idx_device_status (org_id, status),
  CONSTRAINT fk_device_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备台账';

CREATE TABLE device_binding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  elder_id BIGINT,
  bed_id BIGINT,
  bind_type VARCHAR(20) NOT NULL,
  bound_at DATETIME NOT NULL,
  unbound_at DATETIME,
  status VARCHAR(20) NOT NULL DEFAULT 'BOUND',
  KEY idx_device_binding (org_id, device_id, status),
  KEY idx_binding_elder (org_id, elder_id, status),
  CONSTRAINT fk_db_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_db_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_db_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_db_bed FOREIGN KEY (bed_id) REFERENCES bed(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备绑定历史';

CREATE TABLE device_data (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  metric_code VARCHAR(60) NOT NULL,
  metric_value DECIMAL(12,4),
  unit VARCHAR(20),
  raw_payload JSON,
  collected_at DATETIME NOT NULL,
  received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_device_data_time (device_id, collected_at),
  KEY idx_device_data_org_time (org_id, received_at),
  CONSTRAINT fk_dd_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_dd_device FOREIGN KEY (device_id) REFERENCES device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备采集数据';

CREATE TABLE device_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  event_payload JSON,
  event_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  occurred_at DATETIME NOT NULL,
  handled_by BIGINT,
  handled_at DATETIME,
  handle_result TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_device_event_status (org_id, event_status, created_at),
  CONSTRAINT fk_de_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_de_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_de_user FOREIGN KEY (handled_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备事件';

CREATE TABLE risk_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  event_no VARCHAR(60) NOT NULL,
  elder_id BIGINT,
  event_type VARCHAR(40) NOT NULL,
  severity VARCHAR(20) NOT NULL,
  occurred_at DATETIME NOT NULL,
  location_desc VARCHAR(255),
  description TEXT NOT NULL,
  reporter_id BIGINT,
  assignee_id BIGINT,
  status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
  conclusion TEXT,
  closed_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_risk_event_no (org_id, event_no),
  KEY idx_risk_status (org_id, status, severity, occurred_at),
  CONSTRAINT fk_risk_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_risk_elder FOREIGN KEY (elder_id) REFERENCES elder(id),
  CONSTRAINT fk_risk_reporter FOREIGN KEY (reporter_id) REFERENCES sys_user(id),
  CONSTRAINT fk_risk_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全风险事件';

CREATE TABLE risk_event_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  risk_event_id BIGINT NOT NULL,
  action VARCHAR(40) NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30),
  operator_id BIGINT NOT NULL,
  content TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_risk_log (org_id, risk_event_id, created_at),
  CONSTRAINT fk_rel_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_rel_event FOREIGN KEY (risk_event_id) REFERENCES risk_event(id),
  CONSTRAINT fk_rel_user FOREIGN KEY (operator_id) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风险事件处理记录';

CREATE TABLE report_export_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  task_no VARCHAR(60) NOT NULL,
  report_type VARCHAR(60) NOT NULL,
  filter_json JSON,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  file_url VARCHAR(500),
  file_name VARCHAR(255),
  total_rows BIGINT,
  error_message TEXT,
  created_by BIGINT NOT NULL,
  started_at DATETIME,
  finished_at DATETIME,
  expire_at DATETIME,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_export_task_no (org_id, task_no),
  KEY idx_export_user_status (org_id, created_by, status, created_at),
  CONSTRAINT fk_export_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_export_user FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步报表导出任务';

CREATE TABLE regulatory_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  template_code VARCHAR(60) NOT NULL,
  template_name VARCHAR(120) NOT NULL,
  version_no VARCHAR(30) NOT NULL,
  field_mapping_json JSON NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reg_template (org_id, template_code, version_no),
  CONSTRAINT fk_rt_org FOREIGN KEY (org_id) REFERENCES sys_org(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管报送模板';

CREATE TABLE regulatory_batch (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  org_id BIGINT NOT NULL,
  batch_no VARCHAR(60) NOT NULL,
  template_id BIGINT NOT NULL,
  period_start DATE,
  period_end DATE,
  status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
  file_url VARCHAR(500),
  validation_result_json JSON,
  submitted_at DATETIME,
  response_desc TEXT,
  created_by BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_reg_batch (org_id, batch_no),
  KEY idx_reg_batch_status (org_id, status, created_at),
  CONSTRAINT fk_rb_org FOREIGN KEY (org_id) REFERENCES sys_org(id),
  CONSTRAINT fk_rb_template FOREIGN KEY (template_id) REFERENCES regulatory_template(id),
  CONSTRAINT fk_rb_user FOREIGN KEY (created_by) REFERENCES sys_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监管报送批次';

SET FOREIGN_KEY_CHECKS = 1;

-- 实施注意：
-- 1) admission 同一床位有效期不能重叠，需事务 SELECT bed FOR UPDATE + 校验。
-- 2) elder 同一时刻只能有一条 ACTIVE admission，需应用层事务校验。
-- 3) payment/refund 金额边界、billing 汇总金额需在事务中校验并更新。
-- 4) 生产环境需增加数据脱敏/加密、备份、审计、分区和数据保留策略。
