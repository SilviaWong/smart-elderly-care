package com.example.elderlycare.modules.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "账单明细项配置参数")
public class BillingItemDTO {

    @Schema(description = "费用项目ID（可选）")
    private Long feeItemId;

    @NotNull(message = "计费项目名称不能为空")
    @Schema(description = "计费项目名称快照")
    private String itemNameSnapshot;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "计费周期开始日期")
    private LocalDate periodStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "计费周期结束日期")
    private LocalDate periodEnd;

    @NotNull(message = "计费数量不能为空")
    @DecimalMin(value = "0.01", message = "计费数量必须大于0")
    @Schema(description = "计费数量")
    private BigDecimal quantity;

    @NotNull(message = "单价快照不能为空")
    @DecimalMin(value = "0.00", message = "单价不能小于0")
    @Schema(description = "单价快照")
    private BigDecimal unitPrice;
}
