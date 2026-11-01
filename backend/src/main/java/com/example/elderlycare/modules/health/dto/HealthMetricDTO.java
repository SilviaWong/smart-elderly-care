package com.example.elderlycare.modules.health.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthMetricDTO {

    @NotBlank(message = "指标编码不能为空")
    private String metricCode;

    @NotBlank(message = "指标名称不能为空")
    private String metricName;

    private String unit;

    @Builder.Default
    private String valueType = "NUMBER";

    private String description;

    @Builder.Default
    private String status = "ACTIVE";
}
