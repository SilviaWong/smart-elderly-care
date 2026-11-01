package com.example.elderlycare.modules.risk.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.risk.dto.*;
import com.example.elderlycare.modules.risk.service.RiskEventService;
import com.example.elderlycare.modules.risk.vo.RiskEventDetailVO;
import com.example.elderlycare.modules.risk.vo.RiskEventSummaryVO;
import com.example.elderlycare.modules.risk.vo.RiskEventVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "安全风险事件管理", description = "风险事件上报、分派、处置、复核、结案关闭及流转日志接口")
@RestController
@RequestMapping("/api/v1/risk-events")
@RequiredArgsConstructor
public class RiskEventController {

    private final RiskEventService riskEventService;

    @Operation(summary = "分页查询风险事件列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<PageResult<RiskEventVO>> pageEvents(RiskEventQueryDTO query) {
        return ApiResponse.success(riskEventService.pageEvents(query));
    }

    @Operation(summary = "风险事件统计看板数据")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<RiskEventSummaryVO> getSummary() {
        return ApiResponse.success(riskEventService.getSummary());
    }

    @Operation(summary = "获取风险事件详情及全流程轨迹时间线")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<RiskEventDetailVO> getEventDetail(@PathVariable Long id) {
        return ApiResponse.success(riskEventService.getEventDetail(id));
    }

    @Operation(summary = "登记上报安全风险事件")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<Long> reportEvent(@Valid @RequestBody RiskEventReportDTO dto) {
        return ApiResponse.success(riskEventService.reportEvent(dto));
    }

    @Operation(summary = "指派或转派处理负责人")
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('risk')")
    public ApiResponse<Void> assignEvent(@PathVariable Long id, @Valid @RequestBody RiskEventAssignDTO dto) {
        riskEventService.assignEvent(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "开始现场处置风险事件")
    @PutMapping("/{id}/start-process")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<Void> startProcess(@PathVariable Long id, @Valid @RequestBody RiskEventProcessDTO dto) {
        riskEventService.startProcess(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "更新处置过程与现场进展记录")
    @PostMapping("/{id}/progress")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<Void> updateProgress(@PathVariable Long id, @Valid @RequestBody RiskEventProcessDTO dto) {
        riskEventService.updateProgress(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "提交主管复核申请")
    @PutMapping("/{id}/submit-review")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<Void> submitReview(@PathVariable Long id, @Valid @RequestBody RiskEventSubmitReviewDTO dto) {
        riskEventService.submitReview(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "主管复核审批（通过关闭或驳回整改）")
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('risk')")
    public ApiResponse<Void> reviewEvent(@PathVariable Long id, @Valid @RequestBody RiskEventReviewDTO dto) {
        riskEventService.reviewEvent(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "结案关闭风险事件")
    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('risk')")
    public ApiResponse<Void> closeEvent(@PathVariable Long id, @Valid @RequestBody RiskEventCloseDTO dto) {
        riskEventService.closeEvent(id, dto);
        return ApiResponse.success();
    }
}
