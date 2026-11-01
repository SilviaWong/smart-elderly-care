package com.example.elderlycare.modules.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "退款申请参数")
public class RefundApplyDTO {

    @NotNull(message = "原收款记录ID不能为空")
    @Schema(description = "关联的原收款记录ID")
    private Long paymentId;

    @NotNull(message = "申请退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于 0 元")
    @Schema(description = "申请退款金额 (不得大于原收款可退余额)")
    private BigDecimal amount;

    @NotBlank(message = "退款原因不能为空")
    @Schema(description = "退款具体原因说明")
    private String reason;
}
