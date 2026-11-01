package com.example.elderlycare.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.report.dto.ExportTaskCreateDTO;
import com.example.elderlycare.modules.report.dto.ExportTaskQueryDTO;
import com.example.elderlycare.modules.report.dto.ReportQueryDTO;
import com.example.elderlycare.modules.report.entity.ReportExportTask;
import com.example.elderlycare.modules.report.mapper.ReportExportTaskMapper;
import com.example.elderlycare.modules.report.service.ReportService;
import com.example.elderlycare.modules.report.vo.BusinessReportResultVO;
import com.example.elderlycare.modules.report.vo.ReportExportTaskVO;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportExportTaskMapper exportTaskMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final ServiceOrderMapper serviceOrderMapper;
    private final CareTaskMapper careTaskMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final BillingMapper billingMapper;
    private final DeviceMapper deviceMapper;
    private final RiskEventMapper riskEventMapper;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public BusinessReportResultVO queryBusinessReport(ReportQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        String reportType = query.getReportType().trim().toUpperCase();

        return switch (reportType) {
            case "ADMISSION" -> buildAdmissionReport(currentOrgId, query);
            case "BED" -> buildBedReport(currentOrgId, query);
            case "SERVICE_ORDER" -> buildServiceOrderReport(currentOrgId, query);
            case "CARE_TASK" -> buildCareTaskReport(currentOrgId, query);
            case "HEALTH_ALERT" -> buildHealthAlertReport(currentOrgId, query);
            case "FINANCE_BILLING" -> buildFinanceBillingReport(currentOrgId, query);
            case "IOT_DEVICE" -> buildIotDeviceReport(currentOrgId, query);
            case "RISK_EVENT" -> buildRiskEventReport(currentOrgId, query);
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "未知的报表类型: " + reportType);
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createExportTask(ExportTaskCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long orgId = currentOrgId != null ? currentOrgId : 1L;
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        String taskNo = generateTaskNo();
        String reportType = dto.getReportType().trim().toUpperCase();
        String typeName = getReportTypeName(reportType);

        String fileName = dto.getCustomFileName();
        if (!StringUtils.hasText(fileName)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            fileName = String.format("%s_%s.csv", typeName, timestamp);
        }
        if (!fileName.toLowerCase().endsWith(".csv")) {
            fileName += ".csv";
        }

        ReportExportTask task = ReportExportTask.builder()
                .orgId(orgId)
                .taskNo(taskNo)
                .reportType(reportType)
                .filterJson(dto.getFilterJson())
                .status("PENDING")
                .fileName(fileName)
                .fileUrl("/api/v1/reports/export-tasks/" + taskNo + "/download")
                .totalRows(0L)
                .createdBy(currentUserId)
                .expireAt(LocalDateTime.now().plusDays(7)) // 默认 7 天有效期
                .createdAt(LocalDateTime.now())
                .build();

        exportTaskMapper.insert(task);

        sysOperationLogService.recordLog(
                "REPORT_EXPORT",
                "CREATE",
                "EXPORT_TASK",
                task.getId(),
                "/api/v1/reports/export-tasks",
                "127.0.0.1",
                String.format("提交异步报表导出任务: 单号=%s, 类型=%s, 文件名=%s",
                        task.getTaskNo(), typeName, task.getFileName())
        );

        // 触发异步流式后台分批导出
        Long taskId = task.getId();
        CompletableFuture.runAsync(() -> executeStreamingExport(taskId, orgId, reportType, dto.getFilterJson(), fileName));

        return task.getId();
    }

    @Override
    public PageResult<ReportExportTaskVO> pageExportTasks(ExportTaskQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<ReportExportTask> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(ReportExportTask::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(query.getReportType())) {
            qw.eq(ReportExportTask::getReportType, query.getReportType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(ReportExportTask::getStatus, query.getStatus().trim().toUpperCase());
        }
        qw.orderByDesc(ReportExportTask::getCreatedAt).orderByDesc(ReportExportTask::getId);

        Page<ReportExportTask> page = exportTaskMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<ReportExportTaskVO> voList = convertToTaskVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ReportExportTaskVO getExportTaskDetail(Long id) {
        ReportExportTask task = exportTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "导出任务不存在");
        }
        checkTaskAccess(task);

        List<ReportExportTaskVO> voList = convertToTaskVOList(Collections.singletonList(task));
        return voList.get(0);
    }

    @Override
    public Resource downloadExportFile(Long id) {
        ReportExportTask task = exportTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "导出任务记录不存在");
        }
        checkTaskAccess(task);

        if (!"SUCCESS".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务尚未导出成功，无法下载");
        }

        if (task.getExpireAt() != null && LocalDateTime.now().isAfter(task.getExpireAt())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该报表导出文件已超过 7 天保存有效期，已被自动清理；请重新提交导出任务");
        }

        File file = getExportFileObject(task.getId(), task.getFileName());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "文件在存储系统中不存在或已被清理");
        }

        return new FileSystemResource(file);
    }

    @Override
    public int cleanExpiredExportFiles() {
        LocalDateTime now = LocalDateTime.now();
        List<ReportExportTask> expiredTasks = exportTaskMapper.selectList(
                new LambdaQueryWrapper<ReportExportTask>()
                        .le(ReportExportTask::getExpireAt, now)
                        .eq(ReportExportTask::getStatus, "SUCCESS")
        );

        int cleanedCount = 0;
        for (ReportExportTask t : expiredTasks) {
            File f = getExportFileObject(t.getId(), t.getFileName());
            if (f.exists()) {
                boolean deleted = f.delete();
                if (deleted) {
                    cleanedCount++;
                }
            }
        }
        log.info("已完成过期导出文件安全清理, 累计清理物理文件数: {}", cleanedCount);
        return cleanedCount;
    }

    /**
     * 大数据量异步分批流式写入 CSV 文件，严格避免将大量数据一次性加载到 JVM 堆内存导致 OOM
     */
    private void executeStreamingExport(Long taskId, Long orgId, String reportType, String filterJson, String fileName) {
        ReportExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null) return;

        task.setStatus("PROCESSING");
        task.setStartedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);

        File outputFile = getExportFileObject(taskId, fileName);
        long totalRows = 0L;

        try {
            if (!outputFile.getParentFile().exists()) {
                outputFile.getParentFile().mkdirs();
            }

            try (OutputStream os = new FileOutputStream(outputFile);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

                // 写入 UTF-8 BOM，确保 Excel 打开中文毫无乱码
                writer.write('\ufeff');

                // 1. 获取表头列定义
                ReportQueryDTO query = ReportQueryDTO.builder()
                        .reportType(reportType)
                        .pageNum(1)
                        .pageSize(1)
                        .build();
                BusinessReportResultVO meta = queryBusinessReport(query);
                List<BusinessReportResultVO.ReportColumnDef> columns = meta.getColumns();

                // 写入 CSV 表头
                String headerLine = columns.stream()
                        .map(c -> escapeCsvField(c.getLabel()))
                        .collect(Collectors.joining(","));
                writer.write(headerLine);
                writer.newLine();

                // 2. 游标/分批流式翻页获取（每批 200 条），写完立即 flush，释放堆内存对象
                int pageNum = 1;
                int pageSize = 200;
                boolean hasMore = true;

                while (hasMore) {
                    ReportQueryDTO batchQuery = ReportQueryDTO.builder()
                            .reportType(reportType)
                            .pageNum(pageNum)
                            .pageSize(pageSize)
                            .build();

                    BusinessReportResultVO batchResult = queryBusinessReport(batchQuery);
                    List<Map<String, Object>> batchRows = batchResult.getData() != null ? batchResult.getData().getList() : Collections.emptyList();

                    if (CollectionUtils.isEmpty(batchRows)) {
                        hasMore = false;
                        break;
                    }

                    for (Map<String, Object> row : batchRows) {
                        String line = columns.stream()
                                .map(c -> {
                                    Object val = row.get(c.getProp());
                                    return escapeCsvField(val != null ? String.valueOf(val) : "");
                                })
                                .collect(Collectors.joining(","));
                        writer.write(line);
                        writer.newLine();
                        totalRows++;
                    }

                    writer.flush(); // 强制刷盘

                    if (batchRows.size() < pageSize || totalRows >= 50000) {
                        hasMore = false;
                    } else {
                        pageNum++;
                    }
                }
            }

            // 任务成功完成
            task.setStatus("SUCCESS");
            task.setTotalRows(totalRows);
            task.setFinishedAt(LocalDateTime.now());
            task.setFileUrl("/api/v1/reports/export-tasks/" + task.getId() + "/download");
            exportTaskMapper.updateById(task);
            log.info("异步报表导出成功: taskNo={}, rows={}, file={}", task.getTaskNo(), totalRows, outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("异步报表导出失败: taskId={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setFinishedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);
        }
    }

    private File getExportFileObject(Long taskId, String fileName) {
        String baseDir = System.getProperty("user.dir") + File.separator + "exports";
        String safeFileName = StringUtils.hasText(fileName) ? new File(fileName).getName() : "report.csv";
        return new File(baseDir + File.separator + "task_" + taskId + "_" + safeFileName);
    }

    private String escapeCsvField(String field) {
        if (field == null) return "\"\"";
        String s = field.replace("\"", "\"\"");
        return "\"" + s + "\"";
    }

    private String generateTaskNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "EXP" + dateStr + rand;
    }

    private void checkTaskAccess(ReportExportTask task) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构导出文件");
        }
    }

    private List<ReportExportTaskVO> convertToTaskVOList(List<ReportExportTask> tasks) {
        Set<Long> userIds = tasks.stream().map(ReportExportTask::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        LocalDateTime now = LocalDateTime.now();

        return tasks.stream().map(t -> {
            SysUser u = t.getCreatedBy() != null ? userMap.get(t.getCreatedBy()) : null;
            boolean expired = t.getExpireAt() != null && now.isAfter(t.getExpireAt());

            return ReportExportTaskVO.builder()
                    .id(t.getId())
                    .orgId(t.getOrgId())
                    .taskNo(t.getTaskNo())
                    .reportType(t.getReportType())
                    .reportTypeName(getReportTypeName(t.getReportType()))
                    .filterJson(t.getFilterJson())
                    .status(t.getStatus())
                    .statusName(getTaskStatusName(t.getStatus()))
                    .fileUrl(t.getFileUrl())
                    .fileName(t.getFileName())
                    .totalRows(t.getTotalRows())
                    .errorMessage(t.getErrorMessage())
                    .createdBy(t.getCreatedBy())
                    .createdByName(u != null ? u.getRealName() : "-")
                    .startedAt(t.getStartedAt())
                    .finishedAt(t.getFinishedAt())
                    .expireAt(t.getExpireAt())
                    .isExpired(expired)
                    .createdAt(t.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String getReportTypeName(String type) {
        if (type == null) return "综合业务报表";
        return switch (type.toUpperCase()) {
            case "ADMISSION" -> "入住与退住统计报表";
            case "BED" -> "床位资源使用台账报表";
            case "SERVICE_ORDER" -> "照护服务工单报表";
            case "CARE_TASK" -> "护理照料任务执行报表";
            case "HEALTH_ALERT" -> "健康指标异常预警报表";
            case "FINANCE_BILLING" -> "财务账单与收款流水报表";
            case "IOT_DEVICE" -> "物联网终端台账报表";
            case "RISK_EVENT" -> "安全风险与意外事件报表";
            default -> type;
        };
    }

    private String getTaskStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "排队中";
            case "PROCESSING" -> "生成中";
            case "SUCCESS" -> "导出成功";
            case "FAILED" -> "导出失败";
            default -> status;
        };
    }

    /* ================= 8 类业务报表数据构建器 ================= */

    private BusinessReportResultVO buildAdmissionReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("admissionNo", "入住编号", 150, "left"),
                new BusinessReportResultVO.ReportColumnDef("elderName", "长者姓名", 120, "left"),
                new BusinessReportResultVO.ReportColumnDef("admissionDate", "入住日期", 120, "center"),
                new BusinessReportResultVO.ReportColumnDef("plannedDischargeDate", "预计退住", 120, "center"),
                new BusinessReportResultVO.ReportColumnDef("actualDischargeDate", "实际退住", 120, "center"),
                new BusinessReportResultVO.ReportColumnDef("status", "当前状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("depositAmount", "入住押金(元)", 120, "right")
        );

        LambdaQueryWrapper<Admission> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(Admission::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(Admission::getStatus, query.getStatus().trim());
        if (StringUtils.hasText(query.getStartDate())) qw.ge(Admission::getAdmissionDate, query.getStartDate());
        if (StringUtils.hasText(query.getEndDate())) qw.le(Admission::getAdmissionDate, query.getEndDate());
        qw.orderByDesc(Admission::getAdmissionDate);

        Page<Admission> page = admissionMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        Set<Long> elderIds = page.getRecords().stream().map(Admission::getElderId).collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        List<Map<String, Object>> rows = page.getRecords().stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            Elder el = elderMap.get(a.getElderId());
            map.put("admissionNo", a.getAdmissionNo());
            map.put("elderName", el != null ? el.getName() : "长者" + a.getElderId());
            map.put("admissionDate", String.valueOf(a.getAdmissionDate()));
            map.put("plannedDischargeDate", a.getPlannedDischargeDate() != null ? String.valueOf(a.getPlannedDischargeDate()) : "-");
            map.put("actualDischargeDate", a.getActualDischargeDate() != null ? String.valueOf(a.getActualDischargeDate()) : "-");
            map.put("status", "CHECKED_IN".equals(a.getStatus()) ? "在住" : "已退住");
            map.put("depositAmount", a.getDepositAmount() != null ? a.getDepositAmount().toString() : "0.00");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("ADMISSION")
                .reportTypeName("入住与退住统计报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildBedReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("id", "床位ID", 90, "center"),
                new BusinessReportResultVO.ReportColumnDef("bedNumber", "床位号", 110, "left"),
                new BusinessReportResultVO.ReportColumnDef("bedType", "床位类型", 120, "center"),
                new BusinessReportResultVO.ReportColumnDef("status", "使用状态", 110, "center")
        );

        LambdaQueryWrapper<Bed> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(Bed::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(Bed::getStatus, query.getStatus().trim());
        qw.orderByAsc(Bed::getId);

        Page<Bed> page = bedMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(b -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", b.getId());
            map.put("bedNumber", b.getBedNumber() + "床");
            map.put("bedType", b.getBedType() != null ? b.getBedType() : "标准照护床");
            map.put("status", "OCCUPIED".equals(b.getStatus()) ? "已占用" : "空闲");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("BED")
                .reportTypeName("床位资源使用台账报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildServiceOrderReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("orderNo", "工单单号", 160, "left"),
                new BusinessReportResultVO.ReportColumnDef("itemName", "服务项目", 150, "left"),
                new BusinessReportResultVO.ReportColumnDef("price", "金额(元)", 100, "right"),
                new BusinessReportResultVO.ReportColumnDef("status", "工单状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("createdAt", "下单时间", 160, "center")
        );

        LambdaQueryWrapper<ServiceOrder> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(ServiceOrder::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(ServiceOrder::getStatus, query.getStatus().trim());
        qw.orderByDesc(ServiceOrder::getId);

        Page<ServiceOrder> page = serviceOrderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(o -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("orderNo", o.getOrderNo());
            map.put("itemName", o.getItemNameSnapshot());
            map.put("price", o.getPriceSnapshot() != null ? o.getPriceSnapshot().toString() : "0.00");
            map.put("status", o.getStatus());
            map.put("createdAt", String.valueOf(o.getCreatedAt()));
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("SERVICE_ORDER")
                .reportTypeName("照护服务工单报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildCareTaskReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("taskDate", "任务日期", 120, "center"),
                new BusinessReportResultVO.ReportColumnDef("taskContent", "照料服务内容", 200, "left"),
                new BusinessReportResultVO.ReportColumnDef("status", "执行状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("planTime", "计划时段", 120, "center")
        );

        LambdaQueryWrapper<CareTask> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(CareTask::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(CareTask::getStatus, query.getStatus().trim());
        qw.orderByDesc(CareTask::getTaskDate).orderByDesc(CareTask::getId);

        Page<CareTask> page = careTaskMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(t -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskDate", String.valueOf(t.getTaskDate()));
            map.put("taskContent", t.getTaskContent());
            map.put("status", "COMPLETED".equals(t.getStatus()) ? "已完成" : "待执行");
            map.put("planTime", t.getPlanStartTime() != null ? t.getPlanStartTime().toString() : "全天");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("CARE_TASK")
                .reportTypeName("护理照料任务执行报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildHealthAlertReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("alertNo", "预警单号", 160, "left"),
                new BusinessReportResultVO.ReportColumnDef("alertType", "预警类型", 130, "center"),
                new BusinessReportResultVO.ReportColumnDef("severity", "级别", 100, "center"),
                new BusinessReportResultVO.ReportColumnDef("metricValue", "触发测量值", 120, "right"),
                new BusinessReportResultVO.ReportColumnDef("status", "状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("createdAt", "预警发生时间", 160, "center")
        );

        LambdaQueryWrapper<HealthAlert> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(HealthAlert::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(HealthAlert::getStatus, query.getStatus().trim());
        qw.orderByDesc(HealthAlert::getCreatedAt);

        Page<HealthAlert> page = healthAlertMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(a -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("alertNo", a.getAlertNo());
            map.put("alertType", a.getAlertType());
            map.put("severity", a.getSeverity());
            map.put("metricValue", a.getMetricValueSnapshot() != null ? a.getMetricValueSnapshot().toString() : "-");
            map.put("status", a.getStatus());
            map.put("createdAt", String.valueOf(a.getCreatedAt()));
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("HEALTH_ALERT")
                .reportTypeName("健康指标异常预警报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildFinanceBillingReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("billNo", "账单编号", 170, "left"),
                new BusinessReportResultVO.ReportColumnDef("billMonth", "月份", 100, "center"),
                new BusinessReportResultVO.ReportColumnDef("totalAmount", "应收总额(元)", 120, "right"),
                new BusinessReportResultVO.ReportColumnDef("paidAmount", "已收金额(元)", 120, "right"),
                new BusinessReportResultVO.ReportColumnDef("status", "账单状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("dueDate", "付款截止日", 120, "center")
        );

        LambdaQueryWrapper<Billing> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(Billing::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(Billing::getStatus, query.getStatus().trim());
        qw.orderByDesc(Billing::getBillMonth).orderByDesc(Billing::getId);

        Page<Billing> page = billingMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(b -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("billNo", b.getBillNo());
            map.put("billMonth", b.getBillMonth());
            map.put("totalAmount", b.getTotalAmount().toString());
            map.put("paidAmount", b.getPaidAmount().toString());
            map.put("status", b.getStatus());
            map.put("dueDate", b.getDueDate() != null ? String.valueOf(b.getDueDate()) : "-");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("FINANCE_BILLING")
                .reportTypeName("财务账单与收款流水报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildIotDeviceReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("deviceCode", "设备编码", 150, "left"),
                new BusinessReportResultVO.ReportColumnDef("deviceName", "设备名称", 170, "left"),
                new BusinessReportResultVO.ReportColumnDef("deviceType", "设备类型", 130, "center"),
                new BusinessReportResultVO.ReportColumnDef("vendor", "厂商", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("status", "运行状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("lastOnlineAt", "最后在线时间", 160, "center")
        );

        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(Device::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(Device::getStatus, query.getStatus().trim());
        qw.orderByDesc(Device::getId);

        Page<Device> page = deviceMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(d -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("deviceCode", d.getDeviceCode());
            map.put("deviceName", d.getDeviceName());
            map.put("deviceType", d.getDeviceType());
            map.put("vendor", d.getVendor() != null ? d.getVendor() : "-");
            map.put("status", d.getStatus());
            map.put("lastOnlineAt", d.getLastOnlineAt() != null ? String.valueOf(d.getLastOnlineAt()) : "-");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("IOT_DEVICE")
                .reportTypeName("物联网终端台账报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }

    private BusinessReportResultVO buildRiskEventReport(Long orgId, ReportQueryDTO query) {
        List<BusinessReportResultVO.ReportColumnDef> cols = Arrays.asList(
                new BusinessReportResultVO.ReportColumnDef("eventNo", "事件编号", 160, "left"),
                new BusinessReportResultVO.ReportColumnDef("eventType", "风险类型", 130, "center"),
                new BusinessReportResultVO.ReportColumnDef("riskLevel", "等级", 100, "center"),
                new BusinessReportResultVO.ReportColumnDef("status", "状态", 110, "center"),
                new BusinessReportResultVO.ReportColumnDef("occurredAt", "发生时间", 160, "center"),
                new BusinessReportResultVO.ReportColumnDef("conclusion", "处置结论", 200, "left")
        );

        LambdaQueryWrapper<RiskEvent> qw = new LambdaQueryWrapper<>();
        if (orgId != null) qw.eq(RiskEvent::getOrgId, orgId);
        if (StringUtils.hasText(query.getStatus())) qw.eq(RiskEvent::getStatus, query.getStatus().trim());
        qw.orderByDesc(RiskEvent::getOccurredAt);

        Page<RiskEvent> page = riskEventMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        List<Map<String, Object>> rows = page.getRecords().stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("eventNo", r.getEventNo());
            map.put("eventType", r.getEventType());
            map.put("riskLevel", r.getRiskLevel());
            map.put("status", r.getStatus());
            map.put("occurredAt", String.valueOf(r.getOccurredAt()));
            map.put("conclusion", r.getConclusion() != null ? r.getConclusion() : "-");
            return map;
        }).collect(Collectors.toList());

        return BusinessReportResultVO.builder()
                .reportType("RISK_EVENT")
                .reportTypeName("安全风险与意外事件报表")
                .columns(cols)
                .data(PageResult.of(rows, page.getTotal(), page.getCurrent(), page.getSize()))
                .build();
    }
}
