package com.example.elderlycare.modules.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收款记录视图对象")
public class PaymentRecordVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "收款流水编号")
    private String paymentNo;

    @Schema(description = "关联账单ID")
    private Long billingId;

    @Schema(description = "账单单号")
    private String billNo;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者床位")
    private String bedInfo;

    @Schema(description = "收款金额")
    private BigDecimal amount;

    @Schema(description = "支付方式代码: CASH, BANK_TRANSFER, ALIPAY, WECHAT_PAY, POS, MOCK_ONLINE")
    private String paymentMethod;

    @Schema(description = "支付方式中文名称")
    private String paymentMethodName;

    @Schema(description = "外部交易流水号")
    private String transactionNo;

    @Schema(description = "支付幂等键")
    private String idempotencyKey;

    @Schema(description = "状态代码: SUCCESS, CANCELLED")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "支付到账时间")
    private LocalDateTime paidAt;

    @Schema(description = "收款操作人ID")
    private Long operatorId;

    @Schema(description = "收款操作人真实姓名")
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "当前该笔收款剩余最大可退金额")
    private BigDecimal refundableAmount;
}
