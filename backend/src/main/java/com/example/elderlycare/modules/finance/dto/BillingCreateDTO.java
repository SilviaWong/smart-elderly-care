package com.example.elderlycare.modules.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成账单参数")
public class BillingCreateDTO {

    @NotNull(message = "长者ID不能为空")
    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "关联入住办理记录ID（可选）")
    private Long admissionId;

    @NotBlank(message = "账单月份不能为空 (格式: yyyy-MM)")
    @Schema(description = "账单所属月份，如 2026-11")
    private String billMonth;

    @NotNull(message = "出账日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "出账日期")
    private LocalDate billDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "应缴款截止日期")
    private LocalDate dueDate;

    @Schema(description = "账单备注说明")
    private String remark;

    @NotEmpty(message = "账单明细列表不能为空")
    @Valid
    @Schema(description = "收费项目明细列表")
    private List<BillingItemDTO> items;
}
