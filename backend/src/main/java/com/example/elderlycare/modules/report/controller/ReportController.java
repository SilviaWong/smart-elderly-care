package com.example.elderlycare.modules.report.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.report.dto.ExportTaskCreateDTO;
import com.example.elderlycare.modules.report.dto.ExportTaskQueryDTO;
import com.example.elderlycare.modules.report.dto.ReportQueryDTO;
import com.example.elderlycare.modules.report.service.ReportService;
import com.example.elderlycare.modules.report.vo.BusinessReportResultVO;
import com.example.elderlycare.modules.report.vo.ReportExportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "运营管理 - 业务报表与异步导出", description = "多维业务报表查询、大数据量异步分批流式导出、安全下载与过期清理接口")
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "查询业务报表数据 (入住/床位/工单/护理/健康/财务/设备/风险)")
    @GetMapping("/business-data")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('report')")
    public ApiResponse<BusinessReportResultVO> queryBusinessReport(@Valid ReportQueryDTO query) {
        return ApiResponse.success(reportService.queryBusinessReport(query));
    }

    @Operation(summary = "提交异步报表导出任务 (后台分批流式导出，防止内存溢出)")
    @PostMapping("/export-tasks")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('report')")
    public ApiResponse<Long> createExportTask(@Valid @RequestBody ExportTaskCreateDTO dto) {
        return ApiResponse.success(reportService.createExportTask(dto));
    }

    @Operation(summary = "分页查询导出任务列表")
    @GetMapping("/export-tasks")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('report')")
    public ApiResponse<PageResult<ReportExportTaskVO>> pageExportTasks(ExportTaskQueryDTO query) {
        return ApiResponse.success(reportService.pageExportTasks(query));
    }

    @Operation(summary = "获取指定导出任务详情")
    @GetMapping("/export-tasks/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('report')")
    public ApiResponse<ReportExportTaskVO> getExportTaskDetail(@PathVariable Long id) {
        return ApiResponse.success(reportService.getExportTaskDetail(id));
    }

    @Operation(summary = "下载导出的报表文件 (校验租户隔离与文件有效期)")
    @GetMapping("/export-tasks/{id}/download")
    public ResponseEntity<Resource> downloadExportFile(@PathVariable Long id) {
        ReportExportTaskVO task = reportService.getExportTaskDetail(id);
        Resource resource = reportService.downloadExportFile(id);

        String encodedFileName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(resource);
    }

    @Operation(summary = "手动触发过期物理导出文件清理")
    @PostMapping("/export-tasks/clean-expired")
    @PreAuthorize("hasAnyRole('ORG_ADMIN') or hasAuthority('report')")
    public ApiResponse<Integer> cleanExpiredExportFiles() {
        return ApiResponse.success(reportService.cleanExpiredExportFiles());
    }
}
