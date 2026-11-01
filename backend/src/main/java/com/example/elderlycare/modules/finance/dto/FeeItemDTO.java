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
@Schema(description = "费用项目新增/修改参数")
public class FeeItemDTO {

    @NotBlank(message = "费用项目编码不能为空")
    @Schema(description = "费用编码")
    private String feeCode;

    @NotBlank(message = "费用项目名称不能为空")
    @Schema(description = "费用名称")
    private String feeName;

    @NotBlank(message = "费用分类不能为空")
    @Schema(description = "费用分类: BED(床位费), CARE(护理费), MEAL(餐饮费), SERVICE(服务费), MEDICAL(医疗耗材), DEPOSIT(押金), OTHER(其他)")
    private String feeCategory;

    @NotBlank(message = "计费周期单位不能为空")
    @Schema(description = "计费单位: 月, 天, 次, 件")
    private String billingUnit;

    @NotNull(message = "默认基准单价不能为空")
    @DecimalMin(value = "0.00", message = "单价不能小于0")
    @Schema(description = "默认单价")
    private BigDecimal defaultPrice;

    @Schema(description = "状态: ACTIVE, INACTIVE")
    private String status;
}
