-- 智慧养老管理系统 阶段 3 老人主档案与联系人、能力评估种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 初始化老人主档案 (机构 1 与 机构 2)
-- 注意：证件号采用 AES 加密密文存储，并建立 SHA-256 哈希索引
INSERT INTO elder (
  id, org_id, elder_no, name, gender, birth_date, id_type, id_no_ciphertext, id_no_hash, phone,
  address, blood_type, allergy_desc, chronic_disease_desc, medication_desc, diet_taboo,
  mobility_level, status, remark, created_by, created_at, deleted
) VALUES
(
  1, 1, 'ELD-202611-001', '张建国', 'MALE', '1948-05-12', 'ID_CARD',
  '9qfM8N1l+J7cOa6y7wQ9zA==', 'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3', '13811110001',
  '北京市海淀区中关村南大街 1 号院 2 号楼 301', 'A', '青霉素过敏', '高血压2级、冠心病',
  '降压药每日清晨1片，阿司匹林肠溶片晚1片', '低盐低脂饮食、禁食辛辣',
  'PARTIAL', 'PENDING', '老人意识清醒，听力略有下降，需要日常慢病关照', 1, CURRENT_TIMESTAMP, 0
),
(
  2, 1, 'ELD-202611-002', '李淑芬', 'FEMALE', '1952-08-20', 'ID_CARD',
  'd9vK3P5m+K8bPa7z8xR0wB==', 'b3a8e0e1f9ab1bfe3a35f231db3d0f3b55c2460cf6324b0a2dcd6114f636e0a7', '13811110002',
  '北京市海淀区知春路 88 号 5 单元 102', 'O', '磺胺类药物过敏', '骨质疏松、轻度关节炎',
  '钙片每日咀嚼1片', '少糖少油',
  'SELF_CARE', 'PENDING', '爱好书法与园艺，性格开朗', 1, CURRENT_TIMESTAMP, 0
),
(
  3, 2, 'ELD-ORG2-001', '赵德全', 'MALE', '1945-03-15', 'ID_CARD',
  'k8vM1N3l+P9cQa2y1wT8zA==', 'c7b9e1e2f0ac2cfe4b46f342ec4e1f4c66d3571df7435c1b3ede7225f747f1b8', '13922220001',
  '上海市长宁区天山路 99 号', 'B', '无已知过敏', '高脂血症',
  '他汀类药物晚1片', '常规饮食',
  'SELF_CARE', 'PENDING', '上海机构长者档案', 3, CURRENT_TIMESTAMP, 0
)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. 初始化老人紧急联系人 (带主要联系人属性)
INSERT INTO elder_contact (
  id, org_id, elder_id, contact_name, relationship, phone, is_primary, is_emergency, address, created_at
) VALUES
(1, 1, 1, '张志强', 'SON', '13900001111', 1, 1, '北京市海淀区中关村南大街 1 号院', CURRENT_TIMESTAMP),
(2, 1, 1, '王秀英', 'SPOUSE', '13900001112', 0, 1, '北京市海淀区中关村南大街 1 号院', CURRENT_TIMESTAMP),
(3, 1, 2, '李立新', 'DAUGHTER', '13900002222', 1, 1, '北京市海淀区知春路 88 号', CURRENT_TIMESTAMP),
(4, 2, 3, '赵海波', 'SON', '13900003333', 1, 1, '上海市长宁区天山路 99 号', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE contact_name = VALUES(contact_name);

-- 3. 初始化老人能力与风险评估记录
INSERT INTO elder_assessment (
  id, org_id, elder_id, assessment_type, assessment_date, score, result_level, detail_json, assessor_id, remark, created_at
) VALUES
(
  1, 1, 1, 'ADL', '2026-11-01', 75.00, '轻度失能',
  '{"dressing": 10, "bathing": 5, "eating": 10, "walking": 15, "toileting": 10}', 1,
  '进食与穿衣可自理，洗澡需少量协助', CURRENT_TIMESTAMP
),
(
  2, 1, 2, 'ADL', '2026-11-01', 95.00, '能力完好',
  '{"dressing": 10, "bathing": 10, "eating": 10, "walking": 15, "toileting": 10}', 1,
  '日常生活各项能力完好自理', CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE score = VALUES(score);
