package com.example.elderlycare.modules.finance.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.FeeItemDTO;
import com.example.elderlycare.modules.finance.dto.FeeItemQueryDTO;
import com.example.elderlycare.modules.finance.service.FeeItemService;
import com.example.elderlycare.modules.finance.vo.FeeItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "财务管理 - 收费项目配置", description = "收费标准、计费周期与项目启用停用管理接口")
@RestController
@RequestMapping("/api/v1/finance/fee-items")
@RequiredArgsConstructor
public class FeeItemController {

    private final FeeItemService feeItemService;

    @Operation(summary = "分页查询收费项目列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<PageResult<FeeItemVO>> pageFeeItems(FeeItemQueryDTO query) {
        return ApiResponse.success(feeItemService.pageFeeItems(query));
    }

    @Operation(summary = "获取所有已启用的收费项目列表（用于快速出账下拉勾选）")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<List<FeeItemVO>> listActiveFeeItems() {
        return ApiResponse.success(feeItemService.listActiveFeeItems());
    }

    @Operation(summary = "获取收费项目详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<FeeItemVO> getFeeItem(@PathVariable Long id) {
        return ApiResponse.success(feeItemService.getFeeItem(id));
    }

    @Operation(summary = "新增收费项目")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Long> createFeeItem(@Valid @RequestBody FeeItemDTO dto) {
        return ApiResponse.success(feeItemService.createFeeItem(dto));
    }

    @Operation(summary = "修改收费项目")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Void> updateFeeItem(@PathVariable Long id, @Valid @RequestBody FeeItemDTO dto) {
        feeItemService.updateFeeItem(id, dto);
        return ApiResponse.success();
    }

    @Operation(summary = "切换收费项目启用状态 (ACTIVE / INACTIVE)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Void> toggleFeeItemStatus(@PathVariable Long id, @RequestParam String status) {
        feeItemService.toggleFeeItemStatus(id, status);
        return ApiResponse.success();
    }
}
