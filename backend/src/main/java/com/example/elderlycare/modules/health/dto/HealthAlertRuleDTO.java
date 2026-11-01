package com.example.elderlycare.modules.health.dto;

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
public class HealthAlertRuleDTO {

    @NotNull(message = "关联健康指标不能为空")
    private Long metricId;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "条件类型不能为空（GT-高于上限, LT-低于下限, OUT_OF_RANGE-超出区间）")
    private String conditionType;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    @Builder.Default
    private String severity = "MEDIUM";

    @Builder.Default
    private Integer dedupMinutes = 30;

    @Builder.Default
    private Integer enabled = 1;
}
