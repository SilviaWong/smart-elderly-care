package com.example.elderlycare.modules.regulatory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.SecurityCryptoUtil;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.entity.ElderContact;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.regulatory.adapter.RegulatorySubmissionAdapter;
import com.example.elderlycare.modules.regulatory.dto.FieldMappingDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchGenerateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchQueryDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryTemplate;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryBatchMapper;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryTemplateMapper;
import com.example.elderlycare.modules.regulatory.service.RegulatoryBatchService;
import com.example.elderlycare.modules.regulatory.vo.BatchValidationResultVO;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryBatchVO;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegulatoryBatchServiceImpl implements RegulatoryBatchService {

    private final RegulatoryBatchMapper batchMapper;
    private final RegulatoryTemplateMapper templateMapper;
    private final ElderMapper elderMapper;
    private final ElderContactMapper elderContactMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final CareTaskMapper careTaskMapper;
    private final RiskEventMapper riskEventMapper;
    private final SysUserMapper sysUserMapper;
    private final SecurityCryptoUtil securityCryptoUtil;
    private final RegulatorySubmissionAdapter submissionAdapter;
    private final ObjectMapper objectMapper;

    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[1-2]\\d|3[0-1])\\d{3}[\\dXx]$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private Long getCurrentOrgId() {
        Long orgId = OrgContextHolder.getOrgId();
        return orgId != null ? orgId : 1L;
    }

    private Long getCurrentUserId() {
        Long userId = OrgContextHolder.getUserId();
        return userId != null ? userId : 1L;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateBatch(RegulatoryBatchGenerateDTO dto) {
        Long orgId = getCurrentOrgId();
        Long userId = getCurrentUserId();

        RegulatoryTemplate template = templateMapper.selectById(dto.getTemplateId());
        if (template == null || !template.getOrgId().equals(orgId)) {
            throw new BusinessException("报送模板不存在或无权访问");
        }
        if (!"ACTIVE".equalsIgnoreCase(template.getStatus())) {
            throw new BusinessException("所选报送模板处于停用状态，无法生成报送批次");
        }

        FieldMappingDTO mapping = deserializeFieldMapping(template.getFieldMappingJson());
        if (mapping.getFields() == null || mapping.getFields().isEmpty()) {
            throw new BusinessException("报送模板未配置任何字段映射明细");
        }

        // 1. 生成唯一批次号
        String timeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(100, 999);
        String batchNo = "REG" + timeStr + "-" + rand;

        // 2. 提取业务域待报送数据
        String domain = mapping.getBusinessDomain();
        List<Map<String, Object>> rawRows = extractDomainData(domain, orgId, dto.getPeriodStart(), dto.getPeriodEnd());

        // 3. 执行报送规则校验，精确收集字段与行级错误
        BatchValidationResultVO validationResult = validateData(rawRows, mapping.getFields());

        // 4. 生成标准化报送 CSV 文件
        String fileName = template.getTemplateCode() + "_" + batchNo + ".csv";
        String filePath = saveExportCsvFile(batchNo, fileName, rawRows, mapping.getFields());
        String fileUrl = "/api/v1/regulatory/batches/" + batchNo + "/download";

        // 5. 保存批次记录
        RegulatoryBatch batch = RegulatoryBatch.builder()
                .orgId(orgId)
                .batchNo(batchNo)
                .templateId(template.getId())
                .periodStart(dto.getPeriodStart())
                .periodEnd(dto.getPeriodEnd())
                .status("VALIDATED")
                .fileUrl(fileUrl)
                .validationResultJson(serializeValidationResult(validationResult))
                .responseDesc(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : "已抽取并完成合规性前置校验")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        batchMapper.insert(batch);
        log.info("成功生成监管报送批次: batchId={}, batchNo={}, totalRows={}, failedRows={}",
                batch.getId(), batchNo, validationResult.getTotalRows(), validationResult.getFailedRows());

        return batch.getId();
    }

    @Override
    public PageResult<RegulatoryBatchVO> pageBatches(RegulatoryBatchQueryDTO query) {
        Long orgId = getCurrentOrgId();
        LambdaQueryWrapper<RegulatoryBatch> wrapper = new LambdaQueryWrapper<RegulatoryBatch>()
                .eq(RegulatoryBatch::getOrgId, orgId);

        if (query.getTemplateId() != null) {
            wrapper.eq(RegulatoryBatch::getTemplateId, query.getTemplateId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RegulatoryBatch::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getBatchNo())) {
            wrapper.like(RegulatoryBatch::getBatchNo, query.getBatchNo().trim());
        }

        wrapper.orderByDesc(RegulatoryBatch::getCreatedAt);

        Page<RegulatoryBatch> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<RegulatoryBatch> page = batchMapper.selectPage(pageParam, wrapper);

        List<RegulatoryBatchVO> list = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return PageResult.of(list, page.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public RegulatoryBatchVO getBatchDetail(Long id) {
        Long orgId = getCurrentOrgId();
        RegulatoryBatch batch = batchMapper.selectById(id);
        if (batch == null || !batch.getOrgId().equals(orgId)) {
            throw new BusinessException("报送批次不存在或无权访问");
        }
        return convertToVO(batch);
    }

    @Override
    public BatchValidationResultVO getValidationResult(Long id) {
        Long orgId = getCurrentOrgId();
        RegulatoryBatch batch = batchMapper.selectById(id);
        if (batch == null || !batch.getOrgId().equals(orgId)) {
            throw new BusinessException("报送批次不存在或无权访问");
        }
        return deserializeValidationResult(batch.getValidationResultJson());
    }

    @Override
    public Resource downloadBatchFile(Long id) {
        Long orgId = getCurrentOrgId();
        RegulatoryBatch batch = batchMapper.selectById(id);
        if (batch == null || !batch.getOrgId().equals(orgId)) {
            throw new BusinessException("报送批次不存在或无权访问");
        }

        String exportDir = System.getProperty("user.dir") + File.separator + "exports" + File.separator + "regulatory";
        File dir = new File(exportDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 查找对应批次的文件
        File[] matching = dir.listFiles((d, name) -> name.contains(batch.getBatchNo()));
        if (matching == null || matching.length == 0) {
            // 如果物理文件丢失，重新动态生成一份
            RegulatoryTemplate template = templateMapper.selectById(batch.getTemplateId());
            FieldMappingDTO mapping = deserializeFieldMapping(template.getFieldMappingJson());
            List<Map<String, Object>> rawRows = extractDomainData(mapping.getBusinessDomain(), orgId, batch.getPeriodStart(), batch.getPeriodEnd());
            String fileName = template.getTemplateCode() + "_" + batch.getBatchNo() + ".csv";
            saveExportCsvFile(batch.getBatchNo(), fileName, rawRows, mapping.getFields());
            matching = dir.listFiles((d, name) -> name.contains(batch.getBatchNo()));
        }

        if (matching == null || matching.length == 0 || !matching[0].exists()) {
            throw new BusinessException("报送文件不存在或已损坏");
        }

        return new FileSystemResource(matching[0]);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitBatch(Long id, RegulatoryBatchSubmitDTO dto) {
        Long orgId = getCurrentOrgId();
        RegulatoryBatch batch = batchMapper.selectByIdForUpdate(id);
        if (batch == null || !batch.getOrgId().equals(orgId)) {
            throw new BusinessException("报送批次不存在或无权操作");
        }

        if ("SUBMITTED".equalsIgnoreCase(batch.getStatus()) || "CONFIRMED".equalsIgnoreCase(batch.getStatus())) {
            throw new BusinessException("该批次已处于提交状态，请勿重复登记");
        }

        // 调用预留外部监管平台适配器
        RegulatorySubmissionAdapter.SubmissionResponse resp = submissionAdapter.submit(batch, dto);

        batch.setStatus("SUBMITTED");
        batch.setSubmittedAt(LocalDateTime.now());
        batch.setResponseDesc(resp.responseMessage());

        batchMapper.updateById(batch);
        log.info("报送批次登记提交完成: batchId={}, receiptNo={}", id, resp.receiptNo());
    }

    // ================== 数据抽取层 ==================

    private List<Map<String, Object>> extractDomainData(String domain, Long orgId, LocalDate start, LocalDate end) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (!StringUtils.hasText(domain)) {
            domain = "ELDER";
        }

        switch (domain.toUpperCase()) {
            case "ELDER" -> {
                List<Elder> elders = elderMapper.selectList(new LambdaQueryWrapper<Elder>()
                        .eq(Elder::getOrgId, orgId)
                        .eq(Elder::getDeleted, 0)
                        .orderByAsc(Elder::getId));

                for (Elder elder : elders) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", elder.getId());
                    row.put("name", elder.getName());
                    row.put("gender", elder.getGender());
                    row.put("birthDate", elder.getBirthDate() != null ? elder.getBirthDate().toString() : "");
                    row.put("phone", elder.getPhone() != null ? elder.getPhone() : "");

                    // 解密身份证
                    String decryptedId = securityCryptoUtil.decrypt(elder.getIdNoCiphertext());
                    row.put("idCard", decryptedId != null ? decryptedId : "");

                    // 查询在住床位信息
                    Admission adm = admissionMapper.selectOne(new LambdaQueryWrapper<Admission>()
                            .eq(Admission::getOrgId, orgId)
                            .eq(Admission::getElderId, elder.getId())
                            .eq(Admission::getStatus, "CHECKED_IN")
                            .last("LIMIT 1"));
                    if (adm != null) {
                        row.put("bedNo", adm.getBedNo() != null ? adm.getBedNo() : "");
                        row.put("roomNo", adm.getRoomNo() != null ? adm.getRoomNo() : "");
                        row.put("checkInDate", adm.getCheckInDate() != null ? adm.getCheckInDate().toString() : "");
                        row.put("careLevelName", adm.getCareLevelName() != null ? adm.getCareLevelName() : "自理老人");
                    } else {
                        row.put("bedNo", "");
                        row.put("roomNo", "");
                        row.put("checkInDate", "");
                        row.put("careLevelName", "自理老人");
                    }

                    // 紧急联系人
                    ElderContact contact = elderContactMapper.selectOne(new LambdaQueryWrapper<ElderContact>()
                            .eq(ElderContact::getOrgId, orgId)
                            .eq(ElderContact::getElderId, elder.getId())
                            .eq(ElderContact::getIsEmergency, 1)
                            .last("LIMIT 1"));
                    if (contact != null) {
                        row.put("emergencyContact", contact.getContactName() != null ? contact.getContactName() : "");
                        row.put("emergencyPhone", contact.getPhone() != null ? contact.getPhone() : "");
                    } else {
                        row.put("emergencyContact", "");
                        row.put("emergencyPhone", "");
                    }

                    rows.add(row);
                }
            }

            case "BED" -> {
                List<Bed> beds = bedMapper.selectList(new LambdaQueryWrapper<Bed>()
                        .eq(Bed::getOrgId, orgId)
                        .eq(Bed::getDeleted, 0)
                        .orderByAsc(Bed::getId));

                for (Bed bed : beds) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", bed.getId());
                    row.put("bedNo", bed.getBedNo());
                    row.put("bedType", bed.getBedType());
                    row.put("bedStatus", bed.getStatus());

                    Room room = roomMapper.selectById(bed.getRoomId());
                    if (room != null) {
                        row.put("roomNo", room.getRoomNo());
                        row.put("floorNo", room.getFloorNo());

                        Building building = buildingMapper.selectById(room.getBuildingId());
                        row.put("buildingName", building != null ? building.getBuildingName() : "主生活楼");
                    } else {
                        row.put("roomNo", "未分配");
                        row.put("floorNo", 1);
                        row.put("buildingName", "主生活楼");
                    }

                    // 现住老人
                    Admission adm = admissionMapper.selectOne(new LambdaQueryWrapper<Admission>()
                            .eq(Admission::getOrgId, orgId)
                            .eq(Admission::getBedId, bed.getId())
                            .eq(Admission::getStatus, "CHECKED_IN")
                            .last("LIMIT 1"));
                    row.put("currentElderName", adm != null && StringUtils.hasText(adm.getElderName()) ? adm.getElderName() : "-");

                    rows.add(row);
                }
            }

            case "CARE" -> {
                LambdaQueryWrapper<CareTask> taskWrapper = new LambdaQueryWrapper<CareTask>()
                        .eq(CareTask::getOrgId, orgId);
                if (start != null) {
                    taskWrapper.ge(CareTask::getPlanDate, start);
                }
                if (end != null) {
                    taskWrapper.le(CareTask::getPlanDate, end);
                }
                taskWrapper.orderByDesc(CareTask::getPlanDate).last("LIMIT 200");

                List<CareTask> tasks = careTaskMapper.selectList(taskWrapper);
                for (CareTask t : tasks) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", t.getId());
                    row.put("elderName", t.getElderName());
                    row.put("itemName", t.getItemName());
                    row.put("planDate", t.getPlanDate() != null ? t.getPlanDate().toString() : "");
                    row.put("status", t.getStatus());
                    row.put("executorName", t.getExecutorName() != null ? t.getExecutorName() : "-");
                    row.put("executedAt", t.getExecutedAt() != null ? t.getExecutedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-");

                    Elder elder = elderMapper.selectById(t.getElderId());
                    if (elder != null) {
                        row.put("idCard", securityCryptoUtil.decrypt(elder.getIdNoCiphertext()));
                    } else {
                        row.put("idCard", "");
                    }

                    rows.add(row);
                }
            }

            case "RISK" -> {
                LambdaQueryWrapper<RiskEvent> riskWrapper = new LambdaQueryWrapper<RiskEvent>()
                        .eq(RiskEvent::getOrgId, orgId);
                if (start != null) {
                    riskWrapper.ge(RiskEvent::getOccurredAt, start.atStartOfDay());
                }
                if (end != null) {
                    riskWrapper.le(RiskEvent::getOccurredAt, end.atTime(23, 59, 59));
                }
                riskWrapper.orderByDesc(RiskEvent::getOccurredAt).last("LIMIT 200");

                List<RiskEvent> risks = riskEventMapper.selectList(riskWrapper);
                for (RiskEvent r : risks) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", r.getId());
                    row.put("eventNo", r.getEventNo());
                    row.put("eventType", r.getEventType());
                    row.put("riskLevel", r.getRiskLevel());
                    row.put("elderName", r.getElderName() != null ? r.getElderName() : "公共区域无特定长者");
                    row.put("location", r.getLocation());
                    row.put("occurredAt", r.getOccurredAt() != null ? r.getOccurredAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                    row.put("status", r.getStatus());
                    row.put("handlerName", r.getHandlerName() != null ? r.getHandlerName() : "-");

                    rows.add(row);
                }
            }

            default -> {
                log.warn("未知业务域代码: {}", domain);
            }
        }

        return rows;
    }

    // ================== 前置数据校验与定位层 ==================

    private BatchValidationResultVO validateData(List<Map<String, Object>> rows, List<FieldMappingDTO.FieldItem> fields) {
        if (rows == null || rows.isEmpty()) {
            return BatchValidationResultVO.builder()
                    .totalRows(0)
                    .passedRows(0)
                    .failedRows(0)
                    .validationStatus("EMPTY_DATA")
                    .validatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .errors(Collections.emptyList())
                    .build();
        }

        List<BatchValidationResultVO.ValidationErrorItemVO> errorList = new ArrayList<>();
        int passedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            int rowNum = i + 1;
            Long recordId = row.get("id") instanceof Number num ? num.longValue() : (long) rowNum;
            boolean rowHasError = false;

            for (FieldMappingDTO.FieldItem field : fields) {
                Object val = row.get(field.getSourceField());
                String valStr = val != null ? val.toString().trim() : "";

                // 1. 必填校验
                if (Boolean.TRUE.equals(field.getRequired())) {
                    if (!StringUtils.hasText(valStr)) {
                        errorList.add(BatchValidationResultVO.ValidationErrorItemVO.builder()
                                .rowNum(rowNum)
                                .recordId(recordId)
                                .fieldName(field.getSourceField())
                                .fieldLabel(field.getTargetField())
                                .rejectedValue(null)
                                .errorType("MISSING_REQUIRED")
                                .errorMessage(String.format("监管必填字段[%s]未提供，内容为空", field.getTargetField()))
                                .build());
                        rowHasError = true;
                        continue;
                    }
                }

                // 若非必填且值为空，跳过后续规则校验
                if (!StringUtils.hasText(valStr)) {
                    continue;
                }

                // 2. 格式与业务规则校验
                String rule = field.getValidationRule();
                if (StringUtils.hasText(rule)) {
                    if ("ID_CARD".equalsIgnoreCase(rule)) {
                        if (!ID_CARD_PATTERN.matcher(valStr).matches() || valStr.length() != 18) {
                            errorList.add(BatchValidationResultVO.ValidationErrorItemVO.builder()
                                    .rowNum(rowNum)
                                    .recordId(recordId)
                                    .fieldName(field.getSourceField())
                                    .fieldLabel(field.getTargetField())
                                    .rejectedValue(valStr)
                                    .errorType("FORMAT_INVALID")
                                    .errorMessage(String.format("字段[%s]值不符合国家标准18位公民身份号码校验规则", field.getTargetField()))
                                    .build());
                            rowHasError = true;
                        }
                    } else if ("PHONE".equalsIgnoreCase(rule)) {
                        if (!PHONE_PATTERN.matcher(valStr).matches()) {
                            errorList.add(BatchValidationResultVO.ValidationErrorItemVO.builder()
                                    .rowNum(rowNum)
                                    .recordId(recordId)
                                    .fieldName(field.getSourceField())
                                    .fieldLabel(field.getTargetField())
                                    .rejectedValue(valStr)
                                    .errorType("FORMAT_INVALID")
                                    .errorMessage(String.format("字段[%s]值[%s]不是合法的11位手机号码", field.getTargetField(), valStr))
                                    .build());
                            rowHasError = true;
                        }
                    } else if (rule.startsWith("REGEX:")) {
                        String regex = rule.substring(6);
                        if (!Pattern.matches(regex, valStr)) {
                            errorList.add(BatchValidationResultVO.ValidationErrorItemVO.builder()
                                    .rowNum(rowNum)
                                    .recordId(recordId)
                                    .fieldName(field.getSourceField())
                                    .fieldLabel(field.getTargetField())
                                    .rejectedValue(valStr)
                                    .errorType("RULE_VIOLATION")
                                    .errorMessage(String.format("字段[%s]未能通过自定义正则格式校验: %s", field.getTargetField(), regex))
                                    .build());
                            rowHasError = true;
                        }
                    }
                }
            }

            if (rowHasError) {
                failedCount++;
            } else {
                passedCount++;
            }
        }

        String overallStatus = failedCount > 0 ? "CONTAINS_ERRORS" : "ALL_PASSED";

        return BatchValidationResultVO.builder()
                .totalRows(rows.size())
                .passedRows(passedCount)
                .failedRows(failedCount)
                .validationStatus(overallStatus)
                .validatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .errors(errorList)
                .build();
    }

    // ================== CSV 文件生成 ==================

    private String saveExportCsvFile(String batchNo, String fileName, List<Map<String, Object>> rows, List<FieldMappingDTO.FieldItem> fields) {
        String baseDir = System.getProperty("user.dir") + File.separator + "exports" + File.separator + "regulatory";
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File targetFile = new File(dir, fileName);
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8))) {
            // 写入 UTF-8 BOM，解决 Excel 打开中文乱码
            writer.write('\ufeff');

            // 1. 写入表头
            List<FieldMappingDTO.FieldItem> sortedFields = fields.stream()
                    .sorted(Comparator.comparing(f -> f.getSortOrder() != null ? f.getSortOrder() : 999))
                    .collect(Collectors.toList());

            StringBuilder headerBuilder = new StringBuilder();
            for (int i = 0; i < sortedFields.size(); i++) {
                if (i > 0) headerBuilder.append(",");
                headerBuilder.append(escapeCsv(sortedFields.get(i).getTargetField()));
            }
            writer.write(headerBuilder.toString());
            writer.newLine();

            // 2. 写入数据行
            for (Map<String, Object> row : rows) {
                StringBuilder rowBuilder = new StringBuilder();
                for (int i = 0; i < sortedFields.size(); i++) {
                    if (i > 0) rowBuilder.append(",");
                    FieldMappingDTO.FieldItem field = sortedFields.get(i);
                    Object val = row.get(field.getSourceField());
                    String formattedVal = applyTransform(val, field);
                    rowBuilder.append(escapeCsv(formattedVal));
                }
                writer.write(rowBuilder.toString());
                writer.newLine();
            }

            writer.flush();
            log.info("合规报送 CSV 文件生成成功: path={}", targetFile.getAbsolutePath());
            return targetFile.getAbsolutePath();
        } catch (IOException e) {
            log.error("写入监管报送 CSV 文件失败", e);
            throw new BusinessException("写入合规报送 CSV 文件异常: " + e.getMessage());
        }
    }

    private String applyTransform(Object val, FieldMappingDTO.FieldItem field) {
        if (val == null || !StringUtils.hasText(val.toString())) {
            return field.getDefaultValue() != null ? field.getDefaultValue() : "";
        }
        String valStr = val.toString().trim();
        String transform = field.getTransformRule();
        if (StringUtils.hasText(transform)) {
            // 解析映射规则: MALE:1-男,FEMALE:2-女
            String[] pairs = transform.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2 && kv[0].trim().equalsIgnoreCase(valStr)) {
                    return kv[1].trim();
                }
            }
        }
        return valStr;
    }

    private String escapeCsv(String input) {
        if (input == null) return "\"\"";
        String escaped = input.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    // ================== 实体转换与 JSON 辅助 ==================

    private RegulatoryBatchVO convertToVO(RegulatoryBatch entity) {
        RegulatoryTemplate template = templateMapper.selectById(entity.getTemplateId());
        String templateName = template != null ? template.getTemplateName() : "未知模板";
        String templateCode = template != null ? template.getTemplateCode() : "";

        FieldMappingDTO mapping = template != null ? deserializeFieldMapping(template.getFieldMappingJson()) : null;
        String domain = mapping != null ? mapping.getBusinessDomain() : "";
        String targetAuthority = mapping != null ? mapping.getTargetAuthority() : "";

        String createdByName = "系统用户";
        if (entity.getCreatedBy() != null) {
            SysUser user = sysUserMapper.selectById(entity.getCreatedBy());
            if (user != null) {
                createdByName = user.getRealName();
            }
        }

        BatchValidationResultVO validationResult = deserializeValidationResult(entity.getValidationResultJson());

        String fileName = templateCode + "_" + entity.getBatchNo() + ".csv";

        return RegulatoryBatchVO.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .batchNo(entity.getBatchNo())
                .templateId(entity.getTemplateId())
                .templateCode(templateCode)
                .templateName(templateName)
                .businessDomain(domain)
                .targetAuthority(targetAuthority)
                .periodStart(entity.getPeriodStart())
                .periodEnd(entity.getPeriodEnd())
                .status(entity.getStatus())
                .statusName(getStatusName(entity.getStatus()))
                .fileUrl(entity.getFileUrl())
                .fileName(fileName)
                .validationResult(validationResult)
                .submittedAt(entity.getSubmittedAt())
                .responseDesc(entity.getResponseDesc())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String getStatusName(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "GENERATED" -> "已生成草稿";
            case "VALIDATED" -> "已完成校验";
            case "SUBMITTED" -> "已登记提交";
            case "REJECTED" -> "监管退回";
            case "CONFIRMED" -> "备案完成";
            default -> status;
        };
    }

    private String serializeValidationResult(BatchValidationResultVO vo) {
        if (vo == null) return "{}";
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            log.error("校验结果序列化失败", e);
            return "{}";
        }
    }

    private BatchValidationResultVO deserializeValidationResult(String json) {
        if (!StringUtils.hasText(json)) {
            return BatchValidationResultVO.builder()
                    .totalRows(0)
                    .passedRows(0)
                    .failedRows(0)
                    .validationStatus("EMPTY_DATA")
                    .errors(Collections.emptyList())
                    .build();
        }
        try {
            return objectMapper.readValue(json, BatchValidationResultVO.class);
        } catch (Exception e) {
            log.error("校验结果反序列化失败: {}", json, e);
            return BatchValidationResultVO.builder().errors(Collections.emptyList()).build();
        }
    }

    private FieldMappingDTO deserializeFieldMapping(String json) {
        if (!StringUtils.hasText(json)) {
            return FieldMappingDTO.builder().fields(Collections.emptyList()).build();
        }
        try {
            return objectMapper.readValue(json, FieldMappingDTO.class);
        } catch (Exception e) {
            log.error("字段映射反序列化失败: {}", json, e);
            return FieldMappingDTO.builder().fields(Collections.emptyList()).build();
        }
    }
}
