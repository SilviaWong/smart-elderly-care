-- 智慧养老管理系统 阶段 9 餐饮管理种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 初始化菜品台账 (机构 1 与 机构 2)
INSERT INTO dish (
  id, org_id, dish_code, dish_name, category, price, allergens, description, status
) VALUES
(
  1, 1, 'DISH001', '小米养胃南瓜粥', 'STAPLE', 6.00,
  NULL, '精选高山黄小米搭配有机老南瓜，温补脾胃，软糯易消化', 'ACTIVE'
),
(
  2, 1, 'DISH002', '全麦紫薯馒头 (2个)', 'STAPLE', 5.00,
  '麸质谷物', '精制全麦面粉配合纯紫薯泥，富含膳食纤维，低糖低脂', 'ACTIVE'
),
(
  3, 1, 'DISH003', '清蒸鲜嫩鲈鱼片', 'MEAT', 28.00,
  '鱼类/海鲜', '去骨去刺纯净鱼肉，少盐清蒸配葱姜丝，富含优质蛋白', 'ACTIVE'
),
(
  4, 1, 'DISH004', '山药香菇炖土鸡块', 'MEAT', 26.00,
  NULL, '散养土鸡文火慢煨两小时，配河南焦作铁棍山药，滋补强身', 'ACTIVE'
),
(
  5, 1, 'DISH005', '清炒有机软烂时蔬', 'VEGETABLE', 12.00,
  NULL, '当季鲜嫩油菜心/菠菜，橄榄油低温快炒，软烂少油好咀嚼', 'ACTIVE'
),
(
  6, 1, 'DISH006', '番茄炒土鸡蛋', 'VEGETABLE', 15.00,
  '蛋类', '农家散养土鸡蛋配熟透沙瓤番茄，酸甜开胃，营养丰富', 'ACTIVE'
),
(
  7, 1, 'DISH007', '老火茯苓排骨汤', 'SOUP', 18.00,
  NULL, '茯苓配新鲜猪肋排，去油撇沫慢煲，健脾祛湿不上火', 'ACTIVE'
),
(
  8, 1, 'DISH008', '时令温和低糖水果拼盘', 'FRUIT', 10.00,
  NULL, '精选小苹果、猕猴桃切块，常温提供，不刺激肠胃', 'ACTIVE'
),
(
  9, 2, 'DISH901', '上海本帮红烧肉 (长者软烂版)', 'MEAT', 32.00,
  '大豆', '小火慢炖酥烂入味，上海机构特色菜品', 'ACTIVE'
)
ON DUPLICATE KEY UPDATE dish_name = VALUES(dish_name);

-- 2. 初始化每日餐次菜单 (机构 1)
INSERT INTO meal_menu (
  id, org_id, menu_date, meal_type, status, created_by, created_at
) VALUES
(
  1, 1, CURRENT_DATE, 'BREAKFAST', 'PUBLISHED', 1, CURRENT_TIMESTAMP
),
(
  2, 1, CURRENT_DATE, 'LUNCH', 'PUBLISHED', 1, CURRENT_TIMESTAMP
),
(
  3, 1, CURRENT_DATE, 'DINNER', 'PUBLISHED', 1, CURRENT_TIMESTAMP
),
(
  4, 1, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'LUNCH', 'PUBLISHED', 1, CURRENT_TIMESTAMP
),
(
  5, 2, CURRENT_DATE, 'LUNCH', 'PUBLISHED', 3, CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE status = VALUES(status);

-- 3. 关联菜单与菜品快照
INSERT INTO meal_menu_item (
  id, menu_id, dish_id, price_snapshot
) VALUES
-- 今日早餐 (粥 + 馒头 + 水果)
(1, 1, 1, 6.00),
(2, 1, 2, 5.00),
(3, 1, 8, 10.00),

-- 今日午餐 (鲈鱼 + 鸡块 + 软烂时蔬 + 番茄蛋 + 排骨汤)
(4, 2, 3, 28.00),
(5, 2, 4, 26.00),
(6, 2, 5, 12.00),
(7, 2, 6, 15.00),
(8, 2, 7, 18.00),

-- 今日晚餐 (南瓜粥 + 时蔬 + 排骨汤)
(9, 3, 1, 6.00),
(10, 3, 5, 12.00),
(11, 3, 7, 18.00),

-- 明日午餐
(12, 4, 4, 26.00),
(13, 4, 5, 12.00),
(14, 4, 6, 15.00),

-- 机构2午餐
(15, 5, 9, 32.00)
ON DUPLICATE KEY UPDATE price_snapshot = VALUES(price_snapshot);

-- 4. 初始化订餐订单 (保留快照)
INSERT INTO meal_order (
  id, org_id, order_no, elder_id, order_date, meal_type,
  total_amount, status, ordered_by, created_at
) VALUES
(
  1, 1, 'MLO202611010001', 1, CURRENT_DATE, 'LUNCH',
  40.00, 'CONFIRMED', 1, CURRENT_TIMESTAMP
),
(
  2, 1, 'MLO202611010002', 2, CURRENT_DATE, 'LUNCH',
  38.00, 'DELIVERED', 2, CURRENT_TIMESTAMP
),
(
  3, 1, 'MLO202611010003', 1, DATE_ADD(CURRENT_DATE, INTERVAL 1 DAY), 'LUNCH',
  38.00, 'CONFIRMED', 1, CURRENT_TIMESTAMP
),
(
  4, 2, 'MLO202611019001', 3, CURRENT_DATE, 'LUNCH',
  32.00, 'CONFIRMED', 3, CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE total_amount = VALUES(total_amount);

-- 5. 初始化订餐明细 (保存菜品名称快照与单价快照)
INSERT INTO meal_order_item (
  id, order_id, dish_id, dish_name_snapshot, quantity, unit_price, line_amount
) VALUES
-- 订单1明细 (长者1: 鲈鱼 28 + 清炒时蔬 12 = 40)
(1, 1, 3, '清蒸鲜嫩鲈鱼片', 1, 28.00, 28.00),
(2, 1, 5, '清炒有机软烂时蔬', 1, 12.00, 12.00),

-- 订单2明细 (长者2: 山药土鸡 26 + 清炒时蔬 12 = 38)
(3, 2, 4, '山药香菇炖土鸡块', 1, 26.00, 26.00),
(4, 2, 5, '清炒有机软烂时蔬', 1, 12.00, 12.00),

-- 订单3明细 (长者1明日午餐: 山药土鸡 26 + 清炒时蔬 12 = 38)
(5, 3, 4, '山药香菇炖土鸡块', 1, 26.00, 26.00),
(6, 3, 5, '清炒有机软烂时蔬', 1, 12.00, 12.00),

-- 机构2订单明细
(7, 4, 9, '上海本帮红烧肉 (长者软烂版)', 1, 32.00, 32.00)
ON DUPLICATE KEY UPDATE dish_name_snapshot = VALUES(dish_name_snapshot);
