package com.example.elderlycare.modules.finance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "财务综合看板统计指标")
public class FinanceSummaryVO {

    @Schema(description = "本月应收账单总额")
    private BigDecimal totalBilledAmount;

    @Schema(description = "本月实际已收金额 (有效收款 - 成功退款)")
    private BigDecimal totalPaidAmount;

    @Schema(description = "待清缴欠费总额 (应收 - 已收)")
    private BigDecimal totalUnpaidAmount;

    @Schema(description = "待缴清账单总数 (UNPAID + PARTIAL + OVERDUE)")
    private Long unpaidBillCount;

    @Schema(description = "部分付款账单数 (PARTIAL)")
    private Long partialBillCount;

    @Schema(description = "已结清账单数 (PAID)")
    private Long paidBillCount;

    @Schema(description = "逾期未结账单数 (OVERDUE)")
    private Long overdueBillCount;

    @Schema(description = "累计成功退款总金额")
    private BigDecimal totalRefundAmount;
}
