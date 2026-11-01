-- 智慧养老管理系统 阶段 4 空间资源与床位入住种子数据
-- 字符集：utf8mb4

USE smart_elderly_care;

-- 1. 楼栋初始化 (机构 1 与 机构 2)
INSERT INTO building (id, org_id, building_name, building_code, address, status, created_at)
VALUES
(1, 1, '1号颐养楼', 'BLD-01', '园区东侧花园旁', 'ACTIVE', CURRENT_TIMESTAMP),
(2, 1, '2号康护楼', 'BLD-02', '园区西侧医疗区旁', 'ACTIVE', CURRENT_TIMESTAMP),
(3, 2, '海派长者楼', 'BLD-SH01', '园区主楼', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE building_name = VALUES(building_name);

-- 2. 房间初始化
INSERT INTO room (id, org_id, building_id, floor_no, room_no, room_type, status, created_at)
VALUES
(1, 1, 1, '1F', '101', 'DOUBLE', 'ACTIVE', CURRENT_TIMESTAMP),
(2, 1, 1, '1F', '102', 'SINGLE', 'ACTIVE', CURRENT_TIMESTAMP),
(3, 1, 1, '2F', '201', 'DOUBLE', 'ACTIVE', CURRENT_TIMESTAMP),
(4, 1, 2, '1F', '101', 'CARE_ROOM', 'ACTIVE', CURRENT_TIMESTAMP),
(5, 2, 3, '1F', '101', 'DOUBLE', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE room_no = VALUES(room_no);

-- 3. 床位初始化 (初始状态均为 AVAILABLE 空闲，version = 0)
INSERT INTO bed (id, org_id, room_id, bed_no, bed_type, status, version, created_at)
VALUES
(1, 1, 1, '101-01', 'STANDARD', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(2, 1, 1, '101-02', 'STANDARD', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(3, 1, 2, '102-01', 'SINGLE', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(4, 1, 3, '201-01', 'STANDARD', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(5, 1, 3, '201-02', 'STANDARD', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(6, 1, 4, 'CARE-01', 'ELECTRIC_NURSING', 'AVAILABLE', 0, CURRENT_TIMESTAMP),
(7, 2, 5, 'SH-101-01', 'STANDARD', 'AVAILABLE', 0, CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE bed_no = VALUES(bed_no);
