-- 智慧养老管理系统 阶段 5 服务项目与工单全流程种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 服务项目字典初始化 (涵盖生活照护、医疗健康、康复护理、心理慰藉、居室清洁)
INSERT INTO service_item (id, org_id, item_code, item_name, category, unit, standard_price, need_confirm, description, status, created_at)
VALUES
(1, 1, 'SRV-001', '晨间整理与翻身拍背', 'DAILY_CARE', '次', 15.00, 0, '协助卧床或半失能长者进行晨间整理、叩背排痰与防褥疮体位翻转', 'ACTIVE', CURRENT_TIMESTAMP),
(2, 1, 'SRV-002', '协助助浴与洗发', 'DAILY_CARE', '次', 50.00, 1, '专业助浴设备辅助长者洗浴洗发，预防烫伤与滑跌，需护理主管确认验收', 'ACTIVE', CURRENT_TIMESTAMP),
(3, 1, 'SRV-003', '生命体征专项监测', 'HEALTH_NURSING', '次', 20.00, 0, '每日定时测量并记录长者血压、血糖、体温、脉搏与血氧饱和度', 'ACTIVE', CURRENT_TIMESTAMP),
(4, 1, 'SRV-004', '慢病用药依从性提醒与核对', 'HEALTH_NURSING', '次', 10.00, 0, '严格按医嘱核对长者早/中/晚药品、剂量并协助口服温水送服', 'ACTIVE', CURRENT_TIMESTAMP),
(5, 1, 'SRV-005', '肢体康复理疗训练', 'REHAB', '小时', 80.00, 1, '康复师或主管护理员指导并协助开展关节被动活动与下肢步态平衡训练', 'ACTIVE', CURRENT_TIMESTAMP),
(6, 1, 'SRV-006', '心理疏导与情感陪伴', 'PSYCHOLOGY', '小时', 40.00, 0, '倾听长者心声，进行情绪疏解、认知刺激游戏与怀旧疗法互动', 'ACTIVE', CURRENT_TIMESTAMP),
(7, 1, 'SRV-007', '居室深度紫外消杀与除螨', 'CLEANING', '次', 60.00, 1, '长者房间、床品紫外线消毒机深度杀菌，床褥除螨清洁', 'ACTIVE', CURRENT_TIMESTAMP),
(8, 2, 'SRV-SH01', '特需海派助餐配膳', 'DAILY_CARE', '次', 35.00, 0, '个性化低盐低脂营养膳食送餐入室与协助进食', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE item_name = VALUES(item_name);

-- 2. 初始示范工单数据 (展示各种典型工单生命周期状态)
INSERT INTO service_order (id, org_id, order_no, elder_id, admission_id, service_item_id, source_type, priority, expected_start_at, assignee_id, creator_id, status, request_desc, execution_result, completed_at, created_at)
VALUES
(1, 1, 'SO20261101001', 1, 1, 1, 'MANUAL', 'NORMAL', '2026-11-01 08:30:00', 3, 2, 'COMPLETED', '长者早起需协助叩背排痰', '已完成翻身排背，呼吸顺畅，长者精神状态平稳良好。', '2026-11-01 09:10:00', '2026-11-01 08:00:00'),
(2, 1, 'SO20261101002', 2, 2, 2, 'MANUAL', 'HIGH', '2026-11-01 10:00:00', 3, 2, 'IN_PROGRESS', '例行周助浴，长者右侧肢体轻度偏瘫，需特别注意防滑防护', NULL, NULL, '2026-11-01 09:30:00'),
(3, 1, 'SO20261101003', 1, 1, 5, 'MANUAL', 'URGENT', '2026-11-01 14:00:00', 3, 1, 'PENDING_CONFIRM', '术后下肢肌力康复训练40分钟', '已协助完成下肢关节主被动屈伸训练4组，长者配合度高，无明显酸痛。', NULL, '2026-11-01 11:00:00'),
(4, 1, 'SO20261101004', 3, NULL, 6, 'MANUAL', 'NORMAL', '2026-11-01 16:00:00', NULL, 2, 'PENDING_ASSIGN', '长者家属近日出差，长者略感孤单，安排社工/护理员午后陪聊互动', NULL, NULL, '2026-11-01 14:20:00')
ON DUPLICATE KEY UPDATE order_no = VALUES(order_no);

-- 3. 工单流转审计日志初始化
INSERT INTO service_order_log (id, org_id, order_id, from_status, to_status, action, operator_id, remark, created_at)
VALUES
(1, 1, 1, NULL, 'PENDING_ASSIGN', 'CREATE', 2, '主管创建晨间护理工单', '2026-11-01 08:00:00'),
(2, 1, 1, 'PENDING_ASSIGN', 'ASSIGNED', 'ASSIGN', 2, '派发给李护理员', '2026-11-01 08:05:00'),
(3, 1, 1, 'ASSIGNED', 'ACCEPTED', 'ACCEPT', 3, '李护理员接单', '2026-11-01 08:15:00'),
(4, 1, 1, 'ACCEPTED', 'IN_PROGRESS', 'START', 3, '到达101房间开始执行叩背', '2026-11-01 08:30:00'),
(5, 1, 1, 'IN_PROGRESS', 'COMPLETED', 'COMPLETE', 3, '晨间翻身拍背执行完毕', '2026-11-01 09:10:00'),
(6, 1, 2, NULL, 'PENDING_ASSIGN', 'CREATE', 2, '主管创建助浴工单', '2026-11-01 09:30:00'),
(7, 1, 2, 'PENDING_ASSIGN', 'ASSIGNED', 'ASSIGN', 2, '指派李护理员', '2026-11-01 09:32:00'),
(8, 1, 2, 'ASSIGNED', 'ACCEPTED', 'ACCEPT', 3, '李护理员已接单准备助浴用品', '2026-11-01 09:40:00'),
(9, 1, 2, 'ACCEPTED', 'IN_PROGRESS', 'START', 3, '进入助浴室开始助浴', '2026-11-01 10:00:00'),
(10, 1, 3, NULL, 'PENDING_ASSIGN', 'CREATE', 1, '管理员创建康复理疗工单', '2026-11-01 11:00:00'),
(11, 1, 3, 'PENDING_ASSIGN', 'ASSIGNED', 'ASSIGN', 1, '指派李护理员', '2026-11-01 11:05:00'),
(12, 1, 3, 'ASSIGNED', 'ACCEPTED', 'ACCEPT', 3, '接单', '2026-11-01 11:10:00'),
(13, 1, 3, 'ACCEPTED', 'IN_PROGRESS', 'START', 3, '开始训练', '2026-11-01 14:00:00'),
(14, 1, 3, 'IN_PROGRESS', 'PENDING_CONFIRM', 'COMPLETE', 3, '训练完成，该项目需要主管验收', '2026-11-01 14:45:00'),
(15, 1, 4, NULL, 'PENDING_ASSIGN', 'CREATE', 2, '创建待指派陪伴工单', '2026-11-01 14:20:00')
ON DUPLICATE KEY UPDATE action = VALUES(action);
