-- ===================================================================
-- 10_iot_seed.sql: 阶段 11 - 物联网设备、设备绑定、模拟采集数据与设备事件种子数据
-- 覆盖：智能手环、智能体征床垫、智能血压计、毫米波跌倒雷达、SOS 呼叫器
-- ===================================================================

-- 1. 设备台账表 (device)
INSERT INTO device (id, org_id, device_code, device_name, device_type, vendor, model, serial_no, install_location, status, last_online_at, created_at) VALUES
(1, 1, 'DEV_WATCH_001', '华为智能体征手环 (张爱国专用)', 'SMART_WATCH', 'HUAWEI', 'Band-B6-Pro', 'HW20261011001', '随身佩戴', 'ACTIVE', '2026-11-01 20:15:00', '2026-10-01 09:00:00'),
(2, 1, 'DEV_MATTRESS_101', '睡眠呼吸监测床垫 (1号楼101-1床)', 'SMART_MATTRESS', 'SleepAce', 'RestOn-M500', 'SA20261011002', '1号楼 101室 1床', 'ACTIVE', '2026-11-01 20:20:00', '2026-10-01 09:10:00'),
(3, 1, 'DEV_BP_001', '智能蓝牙语音血压计 (护理站专用)', 'BLOOD_PRESSURE', 'OMRON', 'HEM-7600T', 'OM20261011003', '1号楼 1楼护理站', 'ACTIVE', '2026-11-01 16:30:00', '2026-10-01 09:20:00'),
(4, 1, 'DEV_SOS_101', '床头一键紧急呼叫器 (101室)', 'SOS_BUTTON', 'TuyaSmart', 'SOS-Z3-Wall', 'TY20261011004', '1号楼 101室 床头墙面', 'ACTIVE', '2026-11-01 20:00:00', '2026-10-01 09:30:00'),
(5, 1, 'DEV_RADAR_101', '卫生间跌倒检测毫米波雷达 (101室)', 'RADAR_FALL', 'Hikvision', 'DS-PDC-Radar', 'HK20261011005', '1号楼 101室 独立卫浴顶装', 'ACTIVE', '2026-11-01 20:25:00', '2026-10-01 09:40:00'),
(6, 1, 'DEV_WATCH_002', '智能心率体温手环 (李玉华专用)', 'SMART_WATCH', 'HUAWEI', 'Band-B6-Pro', 'HW20261011006', '随身佩戴', 'ACTIVE', '2026-11-01 20:10:00', '2026-10-05 10:00:00'),
(7, 1, 'DEV_MATTRESS_102', '睡眠呼吸监测床垫 (1号楼101-2床)', 'SMART_MATTRESS', 'SleepAce', 'RestOn-M500', 'SA20261011007', '1号楼 101室 2床', 'ACTIVE', '2026-11-01 20:18:00', '2026-10-05 10:15:00'),
(8, 1, 'DEV_GLUCOSE_001', '智能蓝牙生化血糖仪', 'BLOOD_GLUCOSE', 'Sinocare', 'Safe-AQ-Air', 'SC20261011008', '2号楼 护理站药械柜', 'ACTIVE', '2026-11-01 17:15:00', '2026-10-08 11:00:00');

-- 2. 设备绑定历史表 (device_binding)
-- 包含已绑定与历史解绑记录
INSERT INTO device_binding (id, org_id, device_id, elder_id, bed_id, bind_type, bound_at, unbound_at, status) VALUES
(1, 1, 1, 1, NULL, 'ELDER', '2026-10-01 10:00:00', NULL, 'BOUND'),
(2, 1, 2, 1, 1, 'BED', '2026-10-01 10:30:00', NULL, 'BOUND'),
(3, 1, 4, 1, 1, 'SPACE', '2026-10-01 11:00:00', NULL, 'BOUND'),
(4, 1, 5, 1, 1, 'SPACE', '2026-10-01 11:30:00', NULL, 'BOUND'),
(5, 1, 6, 2, NULL, 'ELDER', '2026-10-06 09:30:00', NULL, 'BOUND'),
(6, 1, 7, 2, 2, 'BED', '2026-10-06 10:00:00', NULL, 'BOUND'),
(7, 1, 3, NULL, NULL, 'SPACE', '2026-10-01 12:00:00', '2026-10-15 18:00:00', 'UNBOUND');

-- 3. 设备采集数据表 (device_data)
INSERT INTO device_data (id, org_id, device_id, metric_code, metric_value, unit, raw_payload, collected_at, received_at) VALUES
(1, 1, 1, 'HEART_RATE', 72.0000, 'bpm', '{"rawHeartRate": 72, "signalQuality": "GOOD", "battery": 88}', '2026-11-01 19:30:00', '2026-11-01 19:30:02'),
(2, 1, 1, 'SPO2', 98.0000, '%', '{"rawSpo2": 98, "signalQuality": "GOOD"}', '2026-11-01 19:30:00', '2026-11-01 19:30:02'),
(3, 1, 1, 'STEP_COUNT', 3820.0000, 'steps', '{"stepCount": 3820, "distanceMeters": 2150}', '2026-11-01 20:00:00', '2026-11-01 20:00:03'),
(4, 1, 2, 'RESPIRATORY_RATE', 16.0000, '次/分', '{"respiration": 16, "inBed": true, "motionLevel": 0}', '2026-11-01 20:15:00', '2026-11-01 20:15:05'),
(5, 1, 2, 'IN_BED_STATUS', 1.0000, '状态', '{"inBed": true, "durationMinutes": 45}', '2026-11-01 20:15:00', '2026-11-01 20:15:05'),
(6, 1, 3, 'SBP', 128.0000, 'mmHg', '{"systolic": 128, "diastolic": 82, "pulse": 70}', '2026-11-01 16:30:00', '2026-11-01 16:30:04'),
(7, 1, 3, 'DBP', 82.0000, 'mmHg', '{"systolic": 128, "diastolic": 82, "pulse": 70}', '2026-11-01 16:30:00', '2026-11-01 16:30:04'),
(8, 1, 6, 'HEART_RATE', 68.0000, 'bpm', '{"rawHeartRate": 68, "battery": 75}', '2026-11-01 20:10:00', '2026-11-01 20:10:03'),
(9, 1, 1, 'BATTERY', 12.0000, '%', '{"batteryPercent": 12, "chargeStatus": "DISCHARGING"}', '2026-11-01 20:15:00', '2026-11-01 20:15:02');

-- 4. 设备事件表 (device_event)
INSERT INTO device_event (id, org_id, device_id, event_type, severity, event_payload, event_status, occurred_at, handled_by, handled_at, handle_result, created_at) VALUES
(1, 1, 4, 'SOS', 'CRITICAL', '{"trigger": "BUTTON_PRESS_3S", "location": "1号楼 101室 床头", "elderId": 1, "elderName": "张爱国"}', 'CLOSED', '2026-10-25 14:15:00', 1, '2026-10-25 14:18:00', '值班护士已现场赶赴，老人误触床头按钮，长者神志清楚且无身体不适', '2026-10-25 14:15:02'),
(2, 1, 1, 'LOW_BATTERY', 'LOW', '{"currentBattery": 12, "threshold": 15, "deviceCode": "DEV_WATCH_001"}', 'OPEN', '2026-11-01 20:15:00', NULL, NULL, NULL, '2026-11-01 20:15:03'),
(3, 1, 5, 'FALL_DETECTED', 'CRITICAL', '{"confidence": 0.92, "sensorType": "MMWAVE_RADAR", "location": "101室独立卫浴"}', 'PROCESSING', '2026-11-01 19:40:00', 2, NULL, '护士长陈晓华已前往复核现场雷达状态', '2026-11-01 19:40:05'),
(4, 1, 2, 'LEAVE_BED_OVERTIME', 'HIGH', '{"leaveDurationMinutes": 65, "nightTime": true, "elderName": "张爱国"}', 'OPEN', '2026-11-01 02:30:00', NULL, NULL, NULL, '2026-11-01 02:30:02');
