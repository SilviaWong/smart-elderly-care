-- 智慧养老管理系统 阶段 7 护理等级、护理计划与任务执行种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 护理等级初始化
INSERT INTO care_level (id, org_id, level_code, level_name, description, status)
VALUES
(1, 1, 'LEVEL_1', '一级全介助护理', '适用于完全失能或重度认知症长者，提供24小时全流程生活与医疗照护', 'ACTIVE'),
(2, 1, 'LEVEL_2', '二级半介助护理', '适用于部分失能长者，协助助餐、助浴、翻身与日常慢病依从性管理', 'ACTIVE'),
(3, 1, 'LEVEL_3', '三级介护照料', '适用于轻度功能障碍长者，提供晨晚间整理、用药提醒与定期安全巡视', 'ACTIVE'),
(4, 1, 'LEVEL_SPECIAL', '特级特需专护', '适用于临终关怀、重大术后康复或极高跌倒/窒息风险长者的一对一特护', 'ACTIVE'),
(5, 2, 'LEVEL_1', '一级全护理', '颐和长者之家一级护理', 'ACTIVE')
ON DUPLICATE KEY UPDATE level_name = VALUES(level_name);

-- 2. 护理项目字典初始化
INSERT INTO care_item (id, org_id, item_code, item_name, category, description, status)
VALUES
(1, 1, 'CI_001', '晨间洗漱与口腔清洁', 'DAILY_CARE', '协助长者洗漱、漱口、假牙清洁及面部擦拭', 'ACTIVE'),
(2, 1, 'CI_002', '防褥疮定时体位翻身', 'BODY_NURSING', '每2小时协助卧床长者体位翻转、受压部位减压按摩与皮肤查验', 'ACTIVE'),
(3, 1, 'CI_003', '慢病用药按时核对送服', 'HEALTH_CARE', '核对医嘱用药、确认剂量、口服温水送服并留观长者咽下', 'ACTIVE'),
(4, 1, 'CI_004', '肢体关节被动被辅运动', 'REHAB_CARE', '按康复指导协助各主要大关节被动活动15分钟，预防肌肉挛缩', 'ACTIVE'),
(5, 1, 'CI_005', '居室便器消毒与环境整理', 'ENV_CLEAN', '清扫居室、开窗通风30分钟、倾倒排泄物并含氯消毒液擦拭消杀', 'ACTIVE'),
(6, 1, 'CI_006', '晚间就寝前足浴与更衣', 'DAILY_CARE', '40度温水协助长者足浴15分钟，协助更换睡衣并检查床栏固定', 'ACTIVE'),
(7, 2, 'CI_001', '晨间洗漱', 'DAILY_CARE', '颐和长者晨间护理', 'ACTIVE')
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name);

-- 3. 护理计划初始化
INSERT INTO care_plan (id, org_id, plan_no, elder_id, care_level_id, start_date, end_date, status, creator_id, remark, created_at)
VALUES
(1, 1, 'CP20261101001', 1, 2, '2026-11-01', '2027-10-31', 'ACTIVE', 1, '张建国长者二级半介助日常照护计划', '2026-11-01 08:00:00'),
(2, 1, 'CP20261101002', 2, 3, '2026-11-01', '2027-10-31', 'ACTIVE', 1, '李淑芬长者三级介护照料计划', '2026-11-01 08:00:00')
ON DUPLICATE KEY UPDATE plan_no = VALUES(plan_no);

-- 4. 护理计划明细配置 (执行频率与时间窗口)
INSERT INTO care_plan_item (id, org_id, plan_id, care_item_id, frequency_type, frequency_value, time_window_start, time_window_end, assignee_id, enabled, created_at)
VALUES
(1, 1, 1, 1, 'DAILY', '1', '07:30:00', '08:30:00', 2, 1, '2026-11-01 08:00:00'),
(2, 1, 1, 2, 'DAILY', '1', '09:00:00', '10:00:00', 2, 1, '2026-11-01 08:00:00'),
(3, 1, 1, 3, 'DAILY', '1', '12:00:00', '13:00:00', 2, 1, '2026-11-01 08:00:00'),
(4, 1, 1, 6, 'DAILY', '1', '20:00:00', '21:00:00', 2, 1, '2026-11-01 08:00:00'),
(5, 1, 2, 1, 'DAILY', '1', '08:00:00', '09:00:00', 2, 1, '2026-11-01 08:00:00'),
(6, 1, 2, 3, 'DAILY', '1', '12:30:00', '13:30:00', 2, 1, '2026-11-01 08:00:00')
ON DUPLICATE KEY UPDATE enabled = VALUES(enabled);

-- 5. 示范护理任务数据 (具备严密的 idempotency_key 幂等键)
INSERT INTO care_task (id, org_id, task_no, plan_id, plan_item_id, elder_id, task_date, scheduled_at, assignee_id, status, idempotency_key, created_at)
VALUES
(1, 1, 'CT202611010001', 1, 1, 1, '2026-11-01', '2026-11-01 07:30:00', 2, 'COMPLETED', 'CPI_1_2026-11-01_07:30:00', '2026-11-01 06:00:00'),
(2, 1, 'CT202611010002', 1, 2, 1, '2026-11-01', '2026-11-01 09:00:00', 2, 'IN_PROGRESS', 'CPI_2_2026-11-01_09:00:00', '2026-11-01 06:00:00'),
(3, 1, 'CT202611010003', 1, 3, 1, '2026-11-01', '2026-11-01 12:00:00', 2, 'PENDING', 'CPI_3_2026-11-01_12:00:00', '2026-11-01 06:00:00'),
(4, 1, 'CT202611010004', 2, 5, 2, '2026-11-01', '2026-11-01 08:00:00', 2, 'COMPLETED', 'CPI_5_2026-11-01_08:00:00', '2026-11-01 06:00:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 6. 任务执行记录 (真实执行人、时间、结果)
INSERT INTO care_task_record (id, org_id, task_id, executor_id, executed_at, result_code, result_desc, created_at)
VALUES
(1, 1, 1, 2, '2026-11-01 08:05:00', 'NORMAL', '长者晨起洗漱与口腔假牙清洁完成，面色精神佳。', '2026-11-01 08:05:30'),
(2, 1, 4, 2, '2026-11-01 08:20:00', 'NORMAL', '李奶奶洗漱完毕，协助更衣整理床铺。', '2026-11-01 08:20:30')
ON DUPLICATE KEY UPDATE result_code = VALUES(result_code);
