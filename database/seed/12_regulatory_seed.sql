-- ===================================================================
-- 12_regulatory_seed.sql: 阶段 13 - 监管报送模板、字段映射与报送批次种子数据
-- ===================================================================

USE smart_elderly_care;

-- 1. 监管报送模板定义 (包含完整字段映射、校验规则与字典转换配置)
INSERT INTO regulatory_template (id, org_id, template_code, template_name, version_no, field_mapping_json, status, created_at) VALUES
(1, 1, 'CIVIL_ELDER_RECORD', '民政养老服务平台在住长者备案台账', 'v1.0', '{
  "targetAuthority": "民政局老龄工作与养老服务处",
  "businessDomain": "ELDER",
  "fileFormat": "CSV",
  "description": "民政养老服务监管平台长者基本信息、床位分布与紧急联系人备案规范模板",
  "fields": [
    {"sourceField": "name", "targetField": "长者姓名", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 1},
    {"sourceField": "idCard", "targetField": "公民身份号码", "fieldType": "STRING", "required": true, "validationRule": "ID_CARD", "transformRule": null, "defaultValue": null, "sortOrder": 2},
    {"sourceField": "gender", "targetField": "性别", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "MALE:1-男,FEMALE:2-女", "defaultValue": "1-男", "sortOrder": 3},
    {"sourceField": "birthDate", "targetField": "出生日期", "fieldType": "DATE", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 4},
    {"sourceField": "phone", "targetField": "联系电话", "fieldType": "STRING", "required": false, "validationRule": "PHONE", "transformRule": null, "defaultValue": null, "sortOrder": 5},
    {"sourceField": "bedNo", "targetField": "床位编号", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 6},
    {"sourceField": "roomNo", "targetField": "房间编号", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 7},
    {"sourceField": "checkInDate", "targetField": "入住登记日期", "fieldType": "DATE", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 8},
    {"sourceField": "careLevelName", "targetField": "评估照护等级", "fieldType": "STRING", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "自理老人", "sortOrder": 9},
    {"sourceField": "emergencyContact", "targetField": "紧急联系人姓名", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 10},
    {"sourceField": "emergencyPhone", "targetField": "紧急联系人电话", "fieldType": "STRING", "required": true, "validationRule": "PHONE", "transformRule": null, "defaultValue": null, "sortOrder": 11}
  ]
}', 'ACTIVE', CURRENT_TIMESTAMP),

(2, 1, 'CIVIL_BED_RECORD', '民政机构床位与物理空间备案表', 'v1.0', '{
  "targetAuthority": "民政局社会福利与慈善事业促进处",
  "businessDomain": "BED",
  "fileFormat": "CSV",
  "description": "养老机构床位总数、在用状态及空间分布备案",
  "fields": [
    {"sourceField": "buildingName", "targetField": "所属楼栋", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 1},
    {"sourceField": "floorNo", "targetField": "所在楼层", "fieldType": "INTEGER", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 2},
    {"sourceField": "roomNo", "targetField": "房间门牌号", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 3},
    {"sourceField": "bedNo", "targetField": "床位物理编码", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 4},
    {"sourceField": "bedType", "targetField": "床位功能类型", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "STANDARD:普通单人间,CARE:专业护理床,DOUBLE:双人标间", "defaultValue": "普通单人间", "sortOrder": 5},
    {"sourceField": "bedStatus", "targetField": "当前使用状态", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "VACANT:空闲中,OCCUPIED:已入住占用,MAINTENANCE:维护暂封", "defaultValue": "空闲中", "sortOrder": 6},
    {"sourceField": "currentElderName", "targetField": "现住老人姓名", "fieldType": "STRING", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "-", "sortOrder": 7}
  ]
}', 'ACTIVE', CURRENT_TIMESTAMP),

(3, 1, 'HEALTH_CARE_RECORD', '卫健委医养结合照护服务规范报送', 'v1.0', '{
  "targetAuthority": "市卫生健康委员会老龄健康司",
  "businessDomain": "CARE",
  "fileFormat": "CSV",
  "description": "医养结合机构生活照料与健康巡查执行台账",
  "fields": [
    {"sourceField": "elderName", "targetField": "长者姓名", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 1},
    {"sourceField": "idCard", "targetField": "身份证号", "fieldType": "STRING", "required": true, "validationRule": "ID_CARD", "transformRule": null, "defaultValue": null, "sortOrder": 2},
    {"sourceField": "itemName", "targetField": "护理服务项目", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 3},
    {"sourceField": "planDate", "targetField": "计划服务日期", "fieldType": "DATE", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 4},
    {"sourceField": "status", "targetField": "执行状态", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "COMPLETED:已完成履约,PENDING:待执行,SKIPPED:特殊原因免执", "defaultValue": "待执行", "sortOrder": 5},
    {"sourceField": "executorName", "targetField": "责任护理员", "fieldType": "STRING", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "-", "sortOrder": 6},
    {"sourceField": "executedAt", "targetField": "实际完成时间", "fieldType": "DATETIME", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "-", "sortOrder": 7}
  ]
}', 'ACTIVE', CURRENT_TIMESTAMP),

(4, 1, 'SAFETY_RISK_RECORD', '应急与民政重大安全风险隐患闭环报备', 'v1.0', '{
  "targetAuthority": "区应急管理局与民政执法监察大队",
  "businessDomain": "RISK",
  "fileFormat": "CSV",
  "description": "机构跌倒、意外伤害、火情、走失等安全风险闭环处置追踪报备",
  "fields": [
    {"sourceField": "eventNo", "targetField": "隐患事件单号", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 1},
    {"sourceField": "eventType", "targetField": "风险类型", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "FALL:跌倒意外,ESCAPE:走失风险,FIRE:消防火警,DEVICE:设备断电,FOOD:食品安全", "defaultValue": "其他安全事件", "sortOrder": 2},
    {"sourceField": "riskLevel", "targetField": "风险等级", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "CRITICAL:极高危,HIGH:高风险,MEDIUM:中度隐患,LOW:轻微提示", "defaultValue": "中度隐患", "sortOrder": 3},
    {"sourceField": "elderName", "targetField": "涉及长者姓名", "fieldType": "STRING", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "公用区域无特定长者", "sortOrder": 4},
    {"sourceField": "location", "targetField": "事发物理区域", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 5},
    {"sourceField": "occurredAt", "targetField": "发生时间", "fieldType": "DATETIME", "required": true, "validationRule": "NOT_BLANK", "transformRule": null, "defaultValue": null, "sortOrder": 6},
    {"sourceField": "status", "targetField": "闭环状态", "fieldType": "STRING", "required": true, "validationRule": "NOT_BLANK", "transformRule": "CLOSED:已处置闭环,PROCESSING:处置干预中,PENDING:待分派处置", "defaultValue": "待分派处置", "sortOrder": 7},
    {"sourceField": "handlerName", "targetField": "处置责任人", "fieldType": "STRING", "required": false, "validationRule": null, "transformRule": null, "defaultValue": "-", "sortOrder": 8}
  ]
}', 'ACTIVE', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE template_name = VALUES(template_name);

-- 2. 报送批次示例数据 (覆盖已完成、已提交、有校验错误各种真实测试场景)
INSERT INTO regulatory_batch (id, org_id, batch_no, template_id, period_start, period_end, status, file_url, validation_result_json, submitted_at, response_desc, created_by, created_at) VALUES
(1, 1, 'REG202610-001', 1, '2026-10-01', '2026-10-31', 'SUBMITTED', '/api/v1/regulatory/batches/1/download', '{
  "totalRows": 2,
  "passedRows": 2,
  "failedRows": 0,
  "validationStatus": "ALL_PASSED",
  "validatedAt": "2026-10-31 18:00:00",
  "errors": []
}', '2026-10-31 18:30:00', '回执单号: GOV-CIVIL-202610-880192 | 民政养老监管平台已受理备案，数据校验全部合格', 1, '2026-10-31 17:50:00'),

(2, 1, 'REG202611-002', 2, '2026-11-01', '2026-11-30', 'VALIDATED', '/api/v1/regulatory/batches/2/download', '{
  "totalRows": 6,
  "passedRows": 6,
  "failedRows": 0,
  "validationStatus": "ALL_PASSED",
  "validatedAt": "2026-11-01 09:10:00",
  "errors": []
}', NULL, '数据校验完毕，等待机构主管确认并正式提交', 1, '2026-11-01 09:00:00'),

(3, 1, 'REG202611-003', 3, '2026-10-01', '2026-10-31', 'VALIDATED', '/api/v1/regulatory/batches/3/download', '{
  "totalRows": 5,
  "passedRows": 4,
  "failedRows": 1,
  "validationStatus": "CONTAINS_ERRORS",
  "validatedAt": "2026-11-01 11:30:00",
  "errors": [
    {
      "rowNum": 2,
      "recordId": 102,
      "fieldName": "idCard",
      "fieldLabel": "身份证号",
      "rejectedValue": "11010119500307",
      "errorType": "FORMAT_INVALID",
      "errorMessage": "身份证号长度不足18位，未能通过国家标准校验"
    }
  ]
}', NULL, '数据包含 1 处不合规校验错误，请核对修正后再行提交', 1, '2026-11-01 11:20:00')
ON DUPLICATE KEY UPDATE status = VALUES(status);
