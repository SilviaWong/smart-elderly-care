package com.example.elderlycare.modules.health.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.health.dto.HealthRecordBatchCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordQueryDTO;
import com.example.elderlycare.modules.health.service.HealthRecordService;
import com.example.elderlycare.modules.health.vo.HealthRecordVO;
import com.example.elderlycare.modules.health.vo.HealthTrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "长者健康体征记录管理", description = "长者日常生命体征采集、历史分页、指标趋势分析（声明：系统不提供医疗诊断建议）")
@RestController
@RequestMapping("/api/v1/health/records")
@RequiredArgsConstructor
public class HealthRecordController {

    private final HealthRecordService healthRecordService;

    @Operation(summary = "分页查询健康体征记录")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<PageResult<HealthRecordVO>> pageRecords(HealthRecordQueryDTO query) {
        return ApiResponse.success(healthRecordService.pageRecords(query));
    }

    @Operation(summary = "查询长者特定体征指标时间序列趋势数据")
    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<List<HealthTrendPointVO>> listTrend(
            @RequestParam Long elderId,
            @RequestParam Long metricId,
            @RequestParam(required = false, defaultValue = "7") Integer days) {
        return ApiResponse.success(healthRecordService.listTrend(elderId, metricId, days));
    }

    @Operation(summary = "单条录入健康体征记录并触发预警规则")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<Long> createRecord(@Valid @RequestBody HealthRecordCreateDTO dto) {
        return ApiResponse.success(healthRecordService.createRecord(dto));
    }

    @Operation(summary = "批量录入长者健康体征（如血压/脉搏/体温套检）")
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<List<Long>> batchCreateRecords(@Valid @RequestBody HealthRecordBatchCreateDTO dto) {
        return ApiResponse.success(healthRecordService.batchCreateRecords(dto));
    }
}
