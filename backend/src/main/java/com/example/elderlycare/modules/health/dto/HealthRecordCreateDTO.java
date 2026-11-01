package com.example.elderlycare.modules.health.dto;

import jakarta.validation.constraints.NotNull;
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
public class HealthRecordCreateDTO {

    @NotNull(message = "长者ID不能为空")
    private Long elderId;

    @NotNull(message = "指标ID不能为空")
    private Long metricId;

    @NotNull(message = "指标测量值不能为空")
    private BigDecimal metricValue;

    @NotNull(message = "测量时间不能为空")
    private LocalDateTime measuredAt;

    @Builder.Default
    private String sourceType = "MANUAL";

    private Long sourceDeviceId;

    private String remark;
}
