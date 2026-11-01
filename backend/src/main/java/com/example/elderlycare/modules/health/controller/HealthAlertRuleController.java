package com.example.elderlycare.modules.health.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.health.dto.HealthAlertRuleDTO;
import com.example.elderlycare.modules.health.service.HealthAlertRuleService;
import com.example.elderlycare.modules.health.vo.HealthAlertRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "健康预警规则管理", description = "指标异常阈值条件、预警级别及去重时间窗口配置接口")
@RestController
@RequestMapping("/api/v1/health/alert-rules")
@RequiredArgsConstructor
public class HealthAlertRuleController {

    private final HealthAlertRuleService healthAlertRuleService;

    @Operation(summary = "查询预警规则列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<List<HealthAlertRuleVO>> listRules(
            @RequestParam(required = false) Long metricId,
            @RequestParam(required = false) Integer enabled) {
        return ApiResponse.success(healthAlertRuleService.listRules(metricId, enabled));
    }

    @Operation(summary = "查询预警规则详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('health')")
    public ApiResponse<HealthAlertRuleVO> getRuleById(@PathVariable Long id) {
        return ApiResponse.success(healthAlertRuleService.getRuleById(id));
    }

    @Operation(summary = "新增预警规则")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Long> createRule(@Valid @RequestBody HealthAlertRuleDTO dto) {
        return ApiResponse.success(healthAlertRuleService.createRule(dto));
    }

    @Operation(summary = "修改预警规则")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Void> updateRule(@PathVariable Long id, @Valid @RequestBody HealthAlertRuleDTO dto) {
        healthAlertRuleService.updateRule(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/停用预警规则")
    @PutMapping("/{id}/enabled")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('health')")
    public ApiResponse<Void> toggleRuleEnabled(@PathVariable Long id, @RequestParam Integer enabled) {
        healthAlertRuleService.toggleRuleEnabled(id, enabled);
        return ApiResponse.success();
    }
}
