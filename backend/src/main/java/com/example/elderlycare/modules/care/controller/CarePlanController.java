package com.example.elderlycare.modules.care.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.care.dto.CarePlanCreateDTO;
import com.example.elderlycare.modules.care.dto.CarePlanQueryDTO;
import com.example.elderlycare.modules.care.service.CarePlanService;
import com.example.elderlycare.modules.care.vo.CarePlanDetailVO;
import com.example.elderlycare.modules.care.vo.CarePlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "长者护理计划管理", description = "长者定制化护理计划制定、频次明细维护与计划启停接口")
@RestController
@RequestMapping("/api/v1/care/plans")
@RequiredArgsConstructor
public class CarePlanController {

    private final CarePlanService carePlanService;

    @Operation(summary = "分页查询护理计划")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<PageResult<CarePlanVO>> pagePlans(CarePlanQueryDTO query) {
        return ApiResponse.success(carePlanService.pagePlans(query));
    }

    @Operation(summary = "查询护理计划全息详情（含频次明细）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('care')")
    public ApiResponse<CarePlanDetailVO> getPlanDetail(@PathVariable Long id) {
        return ApiResponse.success(carePlanService.getPlanDetail(id));
    }

    @Operation(summary = "制定长者护理计划")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Long> createPlan(@Valid @RequestBody CarePlanCreateDTO dto) {
        return ApiResponse.success(carePlanService.createPlan(dto));
    }

    @Operation(summary = "修改护理计划及明细配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> updatePlan(@PathVariable Long id, @Valid @RequestBody CarePlanCreateDTO dto) {
        carePlanService.updatePlan(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "启停护理计划（ACTIVE/STOPPED）")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('care')")
    public ApiResponse<Void> togglePlanStatus(@PathVariable Long id, @RequestParam String status) {
        carePlanService.togglePlanStatus(id, status);
        return ApiResponse.success();
    }
}
