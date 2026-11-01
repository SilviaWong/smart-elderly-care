package com.example.elderlycare.modules.finance.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.RefundApplyDTO;
import com.example.elderlycare.modules.finance.dto.RefundAuditDTO;
import com.example.elderlycare.modules.finance.dto.RefundQueryDTO;
import com.example.elderlycare.modules.finance.service.RefundService;
import com.example.elderlycare.modules.finance.vo.RefundRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "财务管理 - 退款管理", description = "退款申请、审批流转、原收款余额校验与账单回退接口")
@RestController
@RequestMapping("/api/v1/finance/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @Operation(summary = "分页查询退款记录列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<PageResult<RefundRecordVO>> pageRefunds(RefundQueryDTO query) {
        return ApiResponse.success(refundService.pageRefunds(query));
    }

    @Operation(summary = "获取退款记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<RefundRecordVO> getRefundDetail(@PathVariable Long id) {
        return ApiResponse.success(refundService.getRefundDetail(id));
    }

    @Operation(summary = "提交退款申请 (严格限制不可超过原收款可退额度)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Long> applyRefund(@Valid @RequestBody RefundApplyDTO dto) {
        return ApiResponse.success(refundService.applyRefund(dto));
    }

    @Operation(summary = "审批退款申请 (审核通过将直接冲抵已收款并回退账单状态)")
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAnyRole('ORG_ADMIN') or hasAuthority('finance')")
    public ApiResponse<Void> auditRefund(@PathVariable Long id, @Valid @RequestBody RefundAuditDTO dto) {
        refundService.auditRefund(id, dto);
        return ApiResponse.success();
    }
}
