package com.example.elderlycare.modules.health.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.health.dto.HealthMetricDTO;
import com.example.elderlycare.modules.health.service.HealthMetricService;
import com.example.elderlycare.modules.health.vo.HealthMetricVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "健康指标配置管理", description = "生命体征指标定义、单位及状态维护接口")
@RestController
@RequestMapping("/api/v1/health/metrics")
@RequiredArgsConstructor
public class HealthMetricController {

    private final HealthMetricService healthMetricService;

    @Operation(summary = "查询健康指标列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<List<HealthMetricVO>> listMetrics(@RequestParam(required = false) String status) {
        return ApiResponse.success(healthMetricService.listMetrics(status));
    }

    @Operation(summary = "查询健康指标详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<HealthMetricVO> getMetricById(@PathVariable Long id) {
        return ApiResponse.success(healthMetricService.getMetricById(id));
    }

    @Operation(summary = "新增健康指标")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Long> createMetric(@Valid @RequestBody HealthMetricDTO dto) {
        return ApiResponse.success(healthMetricService.createMetric(dto));
    }

    @Operation(summary = "修改健康指标")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Void> updateMetric(@PathVariable Long id, @Valid @RequestBody HealthMetricDTO dto) {
        healthMetricService.updateMetric(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/停用健康指标")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Void> toggleMetricStatus(@PathVariable Long id, @RequestParam String status) {
        healthMetricService.toggleMetricStatus(id, status);
        return ApiResponse.success();
    }
}
