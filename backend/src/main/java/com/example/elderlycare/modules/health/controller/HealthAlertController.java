package com.example.elderlycare.modules.health.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.health.dto.HealthAlertAssignDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertHandleDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertQueryDTO;
import com.example.elderlycare.modules.health.service.HealthAlertService;
import com.example.elderlycare.modules.health.vo.HealthAlertSummaryVO;
import com.example.elderlycare.modules.health.vo.HealthAlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "健康预警处置管理", description = "指标异常预警列表、概览统计看板、指派责任人与处置关闭接口")
@RestController
@RequestMapping("/api/v1/health/alerts")
@RequiredArgsConstructor
public class HealthAlertController {

    private final HealthAlertService healthAlertService;

    @Operation(summary = "分页查询健康预警列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<PageResult<HealthAlertVO>> pageAlerts(HealthAlertQueryDTO query) {
        return ApiResponse.success(healthAlertService.pageAlerts(query));
    }

    @Operation(summary = "查询健康预警统计汇总（待处理/处理中/已处理/危急数）")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<HealthAlertSummaryVO> getAlertSummary() {
        return ApiResponse.success(healthAlertService.getAlertSummary());
    }

    @Operation(summary = "查询健康预警详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<HealthAlertVO> getAlertDetail(@PathVariable Long id) {
        return ApiResponse.success(healthAlertService.getAlertDetail(id));
    }

    @Operation(summary = "指派健康预警处理责任人")
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Void> assignAlert(@PathVariable Long id, @Valid @RequestBody HealthAlertAssignDTO dto) {
        healthAlertService.assignAlert(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "处置健康预警（解决或忽略，并填写处置复核说明）")
    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<Void> handleAlert(@PathVariable Long id, @Valid @RequestBody HealthAlertHandleDTO dto) {
        healthAlertService.handleAlert(id, dto);
        return ApiResponse.success();
    }
}
