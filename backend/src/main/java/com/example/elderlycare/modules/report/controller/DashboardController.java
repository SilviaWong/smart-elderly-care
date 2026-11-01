package com.example.elderlycare.modules.report.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.modules.report.service.DashboardService;
import com.example.elderlycare.modules.report.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "运营管理 - 运营大屏与看板", description = "在住人数、床位占用率、工单、护理、健康、财务收入与流转趋势全景接口")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取全景运营看板综合指标")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('dashboard')")
    public ApiResponse<DashboardStatsVO> getDashboardStats() {
        return ApiResponse.success(dashboardService.getDashboardStats());
    }
}
