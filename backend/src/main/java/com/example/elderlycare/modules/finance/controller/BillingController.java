package com.example.elderlycare.modules.finance.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.BillingCreateDTO;
import com.example.elderlycare.modules.finance.dto.BillingQueryDTO;
import com.example.elderlycare.modules.finance.service.BillingService;
import com.example.elderlycare.modules.finance.service.RefundService;
import com.example.elderlycare.modules.finance.vo.BillingVO;
import com.example.elderlycare.modules.finance.vo.FinanceSummaryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "财务管理 - 账单管理", description = "月度账单生成、明细查询、作废与汇总看板接口")
@RestController
@RequestMapping("/api/v1/finance/billings")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final RefundService refundService;

    @Operation(summary = "分页查询账单列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<PageResult<BillingVO>> pageBillings(BillingQueryDTO query) {
        return ApiResponse.success(billingService.pageBillings(query));
    }

    @Operation(summary = "获取账单详情（含明细快照、长者与床位、实收与待付差额）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<BillingVO> getBillingDetail(@PathVariable Long id) {
        return ApiResponse.success(billingService.getBillingDetail(id));
    }

    @Operation(summary = "生成长者月度账单")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Long> createBilling(@Valid @RequestBody BillingCreateDTO dto) {
        return ApiResponse.success(billingService.createBilling(dto));
    }

    @Operation(summary = "作废账单（未发生有效收款时允许直接作废）")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Void> cancelBilling(@PathVariable Long id) {
        billingService.cancelBilling(id);
        return ApiResponse.success();
    }

    @Operation(summary = "财务综合看板统计指标（应收、实收、欠费、退款与账单状态分布）")
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<FinanceSummaryVO> getFinanceSummary() {
        return ApiResponse.success(refundService.getFinanceSummary());
    }
}
