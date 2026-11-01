-- ===================================================================
-- 11_report_seed.sql: 阶段 12 - 运营报表、统计与异步导出任务种子数据
-- ===================================================================

-- 1. 异步报表导出任务 (report_export_task)
INSERT INTO report_export_task (id, org_id, task_no, report_type, filter_json, status, file_url, file_name, total_rows, error_message, created_by, started_at, finished_at, expire_at, created_at) VALUES
(1, 1, 'EXP20261101001', 'ADMISSION', '{"startDate": "2026-10-01", "endDate": "2026-10-31"}', 'SUCCESS', '/api/v1/reports/export-tasks/1/download', '2026年10月长者入住与退住统计报表.csv', 18, NULL, 1, '2026-11-01 10:00:01', '2026-11-01 10:00:03', '2026-11-08 10:00:00', '2026-11-01 10:00:00'),
(2, 1, 'EXP20261101002', 'FINANCE_BILLING', '{"billMonth": "2026-10"}', 'SUCCESS', '/api/v1/reports/export-tasks/2/download', '2026年10月机构财务账单与收款明细.csv', 12, NULL, 1, '2026-11-01 11:20:02', '2026-11-01 11:20:05', '2026-11-08 11:20:00', '2026-11-01 11:20:00'),
(3, 1, 'EXP20261101003', 'CARE_TASK', '{"careLevel": "LEVEL_3"}', 'SUCCESS', '/api/v1/reports/export-tasks/3/download', '重度护理照料任务执行与完成率汇总.csv', 35, NULL, 1, '2026-11-01 14:15:00', '2026-11-01 14:15:02', '2026-11-08 14:15:00', '2026-11-01 14:14:58'),
(4, 1, 'EXP20261101004', 'IOT_DEVICE', '{"deviceType": "SMART_WATCH"}', 'SUCCESS', '/api/v1/reports/export-tasks/4/download', '智能穿戴设备体征采集遥测数据报表.csv', 56, NULL, 1, '2026-11-01 16:00:01', '2026-11-01 16:00:04', '2026-11-08 16:00:00', '2026-11-01 16:00:00');
