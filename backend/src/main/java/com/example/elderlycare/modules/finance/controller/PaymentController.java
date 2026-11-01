package com.example.elderlycare.modules.finance.controller;

import com.example.elderlycare.common.api.ApiResponse;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.PaymentCreateDTO;
import com.example.elderlycare.modules.finance.dto.PaymentQueryDTO;
import com.example.elderlycare.modules.finance.service.PaymentService;
import com.example.elderlycare.modules.finance.vo.PaymentRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "财务管理 - 收款与支付管理", description = "账单缴费登记、支付流水查询、幂等控制与可退额度查询接口")
@RestController
@RequestMapping("/api/v1/finance/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "分页查询收款支付记录")
    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<PageResult<PaymentRecordVO>> pagePayments(PaymentQueryDTO query) {
        return ApiResponse.success(paymentService.pagePayments(query));
    }

    @Operation(summary = "获取收款支付记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR', 'CAREGIVER') or hasAuthority('finance')")
    public ApiResponse<PaymentRecordVO> getPaymentDetail(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getPaymentDetail(id));
    }

    @Operation(summary = "登记收款/模拟支付入账 (具备全局幂等拦截)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<Long> registerPayment(@Valid @RequestBody PaymentCreateDTO dto) {
        return ApiResponse.success(paymentService.registerPayment(dto));
    }

    @Operation(summary = "查询指定收款流水当前剩余最大可退金额")
    @GetMapping("/{id}/refundable-amount")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'NURSE_DIRECTOR') or hasAuthority('finance')")
    public ApiResponse<BigDecimal> getRefundableAmount(@PathVariable Long id) {
        return ApiResponse.success(paymentService.getRefundableAmount(id));
    }
}
