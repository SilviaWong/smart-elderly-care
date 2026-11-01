-- 智慧养老管理系统 阶段 6 健康指标、健康记录与预警规则种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 健康指标字典初始化 (涵盖收缩压、舒张压、心率、体温、血氧饱和度、空腹血糖、体重)
INSERT INTO health_metric (id, org_id, metric_code, metric_name, unit, value_type, description, status)
VALUES
(1, 1, 'SBP', '收缩压', 'mmHg', 'NUMBER', '心脏收缩时动脉血管内的压力，正常参考值 90-139 mmHg', 'ACTIVE'),
(2, 1, 'DBP', '舒张压', 'mmHg', 'NUMBER', '心脏舒张时动脉血管内的压力，正常参考值 60-89 mmHg', 'ACTIVE'),
(3, 1, 'HR', '心率/脉搏', 'bpm', 'NUMBER', '静息状态下每分钟心跳次数，正常参考值 60-100 bpm', 'ACTIVE'),
(4, 1, 'TEMP', '体温', '℃', 'NUMBER', '长者耳温/额温/腋温，正常参考值 36.0-37.2 ℃', 'ACTIVE'),
(5, 1, 'SPO2', '血氧饱和度', '%', 'NUMBER', '血液中氧合血红蛋白容量占全部血红蛋白容量的百分比，正常值 95%-100%', 'ACTIVE'),
(6, 1, 'GLU_FASTING', '空腹血糖', 'mmol/L', 'NUMBER', '禁食8-10小时后静脉或指尖血糖，正常参考值 3.9-6.1 mmol/L', 'ACTIVE'),
(7, 1, 'WEIGHT', '体重', 'kg', 'NUMBER', '长者晨起空腹静重，用于评估营养状况与水肿监测', 'ACTIVE'),
(8, 2, 'SBP', '收缩压', 'mmHg', 'NUMBER', '颐和长者之家收缩压监测指标', 'ACTIVE')
ON DUPLICATE KEY UPDATE metric_name = VALUES(metric_name);

-- 2. 健康预警规则初始化 (针对收缩压、舒张压、心率、体温、血氧等关键体征)
INSERT INTO health_alert_rule (id, org_id, metric_id, rule_name, condition_type, min_value, max_value, severity, dedup_minutes, enabled, created_at)
VALUES
(1, 1, 1, '高血压二级危急预警', 'GT', NULL, 160.000, 'HIGH', 30, 1, CURRENT_TIMESTAMP),
(2, 1, 1, '低血压风险预警', 'LT', 90.000, NULL, 'HIGH', 30, 1, CURRENT_TIMESTAMP),
(3, 1, 3, '心动过速预警', 'GT', NULL, 100.000, 'MEDIUM', 30, 1, CURRENT_TIMESTAMP),
(4, 1, 3, '心动过缓预警', 'LT', 55.000, NULL, 'HIGH', 30, 1, CURRENT_TIMESTAMP),
(5, 1, 4, '发热体温预警', 'GT', NULL, 37.300, 'HIGH', 30, 1, CURRENT_TIMESTAMP),
(6, 1, 5, '低血氧危急警报', 'LT', 93.000, NULL, 'CRITICAL', 15, 1, CURRENT_TIMESTAMP),
(7, 1, 6, '高血糖超标预警', 'GT', NULL, 7.800, 'MEDIUM', 60, 1, CURRENT_TIMESTAMP),
(8, 2, 8, '收缩压超标预警', 'GT', NULL, 150.000, 'HIGH', 30, 1, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE rule_name = VALUES(rule_name);

-- 3. 健康记录示范数据 (展示典型长者的多维度生命体征)
INSERT INTO health_record (id, org_id, elder_id, metric_id, metric_value, unit_snapshot, measured_at, source_type, recorder_id, remark, created_at)
VALUES
(1, 1, 1, 1, 168.000, 'mmHg', '2026-11-01 08:30:00', 'MANUAL', 2, '晨起晨检测量，长者自述略有头昏', '2026-11-01 08:31:00'),
(2, 1, 1, 2, 95.000, 'mmHg', '2026-11-01 08:30:00', 'MANUAL', 2, '晨起舒张压', '2026-11-01 08:31:00'),
(3, 1, 1, 3, 76.000, 'bpm', '2026-11-01 08:30:00', 'MANUAL', 2, '脉搏律齐', '2026-11-01 08:31:00'),
(4, 1, 1, 4, 36.600, '℃', '2026-11-01 08:30:00', 'MANUAL', 2, '体温正常', '2026-11-01 08:31:00'),
(5, 1, 1, 5, 91.000, '%', '2026-11-01 09:15:00', 'MANUAL', 2, '长者活动后自述微喘，指尖血氧偏低', '2026-11-01 09:16:00'),
(6, 1, 2, 1, 125.000, 'mmHg', '2026-11-01 08:40:00', 'MANUAL', 2, '李奶奶血压平稳', '2026-11-01 08:41:00'),
(7, 1, 2, 4, 38.200, '℃', '2026-11-01 10:20:00', 'MANUAL', 2, '长者面色发红，耳温测得38.2度', '2026-11-01 10:21:00')
ON DUPLICATE KEY UPDATE metric_value = VALUES(metric_value);

-- 4. 示范健康预警数据 (展示未处理与处理中的告警单)
INSERT INTO health_alert (id, org_id, elder_id, health_record_id, rule_id, alert_no, severity, alert_value, status, assignee_id, handled_at, handle_result, created_at)
VALUES
(1, 1, 1, 1, 1, 'ALT202611010001', 'HIGH', 168.000, 'PROCESSING', 2, NULL, NULL, '2026-11-01 08:31:05'),
(2, 1, 1, 5, 6, 'ALT202611010002', 'CRITICAL', 91.000, 'OPEN', NULL, NULL, NULL, '2026-11-01 09:16:05'),
(3, 1, 2, 7, 5, 'ALT202611010003', 'HIGH', 38.200, 'RESOLVED', 2, '2026-11-01 11:30:00', '已协助物理降温并遵医嘱口服退热药，半小时后复测体温降至37.1℃，精神状态好转。', '2026-11-01 10:21:05')
ON DUPLICATE KEY UPDATE alert_no = VALUES(alert_no);
