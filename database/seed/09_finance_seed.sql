-- 智慧养老管理系统 阶段 10 财务管理种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 初始化费用项目标准库 (机构 1 与 机构 2)
INSERT INTO fee_item (
  id, org_id, fee_code, fee_name, fee_category, billing_unit, default_price, status
) VALUES
(
  1, 1, 'FEE_BED_STD', '标准双人间床位费 (月)', 'BED', '月', 2800.00, 'ACTIVE'
),
(
  2, 1, 'FEE_BED_VIP', '豪华单人间床位费 (月)', 'BED', '月', 4200.00, 'ACTIVE'
),
(
  3, 1, 'FEE_CARE_L2', '二级照护等级护理费 (月)', 'CARE', '月', 1800.00, 'ACTIVE'
),
(
  4, 1, 'FEE_CARE_L3', '三级照护等级护理费 (月)', 'CARE', '月', 2600.00, 'ACTIVE'
),
(
  5, 1, 'FEE_MEAL_STD', '长者营养膳食套餐包 (月)', 'MEAL', '月', 1200.00, 'ACTIVE'
),
(
  6, 1, 'FEE_SERVICE_ESCORT', '三甲医院专家门诊陪诊服务', 'SERVICE', '次', 150.00, 'ACTIVE'
),
(
  7, 1, 'FEE_DEPOSIT', '长者入住医疗急救与综合押金', 'DEPOSIT', '次', 10000.00, 'ACTIVE'
),
(
  8, 2, 'FEE_ORG2_BED', '上海园区标准床位费 (月)', 'BED', '月', 3200.00, 'ACTIVE'
)
ON DUPLICATE KEY UPDATE fee_name = VALUES(fee_name);

-- 2. 初始化月度账单 (机构 1 与 机构 2)
INSERT INTO billing (
  id, org_id, bill_no, elder_id, admission_id, bill_month, bill_date, due_date,
  total_amount, paid_amount, status, remark, created_by, created_at
) VALUES
(
  1, 1, 'BIL2026110001', 1, 1, '2026-11', '2026-11-01', '2026-11-15',
  5800.00, 5800.00, 'PAID', '张建国长者2026年11月份常规月度综合账单(床位+护理+餐饮)', 1, '2026-11-01 08:30:00'
),
(
  2, 1, 'BIL2026110002', 2, 2, '2026-11', '2026-11-01', '2026-11-15',
  5800.00, 2800.00, 'PARTIAL', '李淑芬长者11月份账单，家属已预付床位费2800元，剩余待缴清', 1, '2026-11-01 08:35:00'
),
(
  3, 1, 'BIL2026100001', 1, 1, '2026-10', '2026-10-01', '2026-10-15',
  5950.00, 0.00, 'OVERDUE', '10月份陪诊及床位账单，已逾期未结', 1, '2026-10-01 09:00:00'
),
(
  4, 2, 'BIL2026119001', 3, 3, '2026-11', '2026-11-01', '2026-11-20',
  3200.00, 0.00, 'UNPAID', '上海机构赵德全长者11月床位费账单', 3, '2026-11-01 09:10:00'
)
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- 3. 初始化账单明细快照 (包含计费周期、数量、单价快照与金额)
INSERT INTO billing_item (
  id, billing_id, fee_item_id, item_name_snapshot, period_start, period_end,
  quantity, unit_price, amount
) VALUES
-- 账单1明细 (张建国 11月: 床位 2800 + 二级护理 1800 + 膳食包 1200 = 5800)
(1, 1, 1, '标准双人间床位费 (月)', '2026-11-01', '2026-11-30', 1.00, 2800.00, 2800.00),
(2, 1, 3, '二级照护等级护理费 (月)', '2026-11-01', '2026-11-30', 1.00, 1800.00, 1800.00),
(3, 1, 5, '长者营养膳食套餐包 (月)', '2026-11-01', '2026-11-30', 1.00, 1200.00, 1200.00),

-- 账单2明细 (李淑芬 11月: 床位 2800 + 二级护理 1800 + 膳食包 1200 = 5800)
(4, 2, 1, '标准双人间床位费 (月)', '2026-11-01', '2026-11-30', 1.00, 2800.00, 2800.00),
(5, 2, 3, '二级照护等级护理费 (月)', '2026-11-01', '2026-11-30', 1.00, 1800.00, 1800.00),
(6, 2, 5, '长者营养膳食套餐包 (月)', '2026-11-01', '2026-11-30', 1.00, 1200.00, 1200.00),

-- 账单3明细 (张建国 10月: 床位 2800 + 二级护理 1800 + 膳食包 1200 + 专家陪诊 150 = 5950)
(7, 3, 1, '标准双人间床位费 (月)', '2026-10-01', '2026-10-31', 1.00, 2800.00, 2800.00),
(8, 3, 3, '二级照护等级护理费 (月)', '2026-10-01', '2026-10-31', 1.00, 1800.00, 1800.00),
(9, 3, 5, '长者营养膳食套餐包 (月)', '2026-10-01', '2026-10-31', 1.00, 1200.00, 1200.00),
(10, 3, 6, '三甲医院专家门诊陪诊服务', '2026-10-15', '2026-10-15', 1.00, 150.00, 150.00),

-- 账单4明细 (上海机构)
(11, 4, 8, '上海园区标准床位费 (月)', '2026-11-01', '2026-11-30', 1.00, 3200.00, 3200.00)
ON DUPLICATE KEY UPDATE amount = VALUES(amount);

-- 4. 初始化收款记录 (包含支付幂等键与外部交易流水)
INSERT INTO payment_record (
  id, org_id, payment_no, billing_id, amount, payment_method,
  transaction_no, idempotency_key, status, paid_at, operator_id, created_at
) VALUES
(
  1, 1, 'PMT202611010001', 1, 5800.00, 'BANK_TRANSFER',
  'TXN_CMB_20261101987654', 'IDEM_PAY_BIL0001_FULL', 'SUCCESS', '2026-11-01 10:00:00', 1, '2026-11-01 10:00:00'
),
(
  2, 1, 'PMT202611010002', 2, 2800.00, 'WECHAT_PAY',
  'TXN_WXP_20261101123456', 'IDEM_PAY_BIL0002_PART1', 'SUCCESS', '2026-11-01 11:30:00', 1, '2026-11-01 11:30:00'
),
(
  3, 1, 'PMT202610200001', 1, 500.00, 'ALIPAY',
  'TXN_ALI_20261020654321', 'IDEM_PAY_BIL0001_REFUND_DEMO', 'SUCCESS', '2026-10-20 14:00:00', 1, '2026-10-20 14:00:00'
)
ON DUPLICATE KEY UPDATE amount = VALUES(amount);

-- 5. 初始化退款记录 (关联原收款记录)
INSERT INTO refund_record (
  id, org_id, refund_no, payment_id, amount, reason,
  status, approved_by, refunded_at, created_by, created_at
) VALUES
(
  1, 1, 'RFD202610210001', 3, 500.00, '长者请假外出就医期间多收餐费，核对后全额退回原支付宝账户',
  'SUCCESS', 1, '2026-10-21 15:30:00', 1, '2026-10-21 14:00:00'
)
ON DUPLICATE KEY UPDATE reason = VALUES(reason);
