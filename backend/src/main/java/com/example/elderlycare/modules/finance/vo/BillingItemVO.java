package com.example.elderlycare.modules.finance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "账单明细项视图对象")
public class BillingItemVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "所属账单ID")
    private Long billingId;

    @Schema(description = "费用项目ID")
    private Long feeItemId;

    @Schema(description = "计费项目名称快照")
    private String itemNameSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "计费周期开始日期")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "计费周期结束日期")
    private LocalDate periodEnd;

    @Schema(description = "计费数量")
    private BigDecimal quantity;

    @Schema(description = "单价快照")
    private BigDecimal unitPrice;

    @Schema(description = "明细金额")
    private BigDecimal amount;
}
