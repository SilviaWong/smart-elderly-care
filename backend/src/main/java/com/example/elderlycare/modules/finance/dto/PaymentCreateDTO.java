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
@Schema(description = "登记收款/模拟支付参数")
public class PaymentCreateDTO {

    @NotNull(message = "账单ID不能为空")
    @Schema(description = "关联账单ID")
    private Long billingId;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于 0 元")
    @Schema(description = "支付收款金额")
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    @Schema(description = "支付渠道/方式: CASH(现金), BANK_TRANSFER(银行转账), ALIPAY(支付宝), WECHAT_PAY(微信支付), POS(POS刷卡), MOCK_ONLINE(模拟在线支付)")
    private String paymentMethod;

    @Schema(description = "外部支付流水交易号/转账单号")
    private String transactionNo;

    @NotBlank(message = "支付幂等键不能为空")
    @Schema(description = "全局唯一幂等键 (防止重复提交造成重复入账扣款)")
    private String idempotencyKey;
}
