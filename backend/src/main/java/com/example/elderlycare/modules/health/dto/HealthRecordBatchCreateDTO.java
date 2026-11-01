package com.example.elderlycare.modules.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordBatchCreateDTO {

    @NotNull(message = "长者ID不能为空")
    private Long elderId;

    @NotNull(message = "测量时间不能为空")
    private LocalDateTime measuredAt;

    @Builder.Default
    private String sourceType = "MANUAL";

    private Long sourceDeviceId;

    private String remark;

    @NotEmpty(message = "指标测量列表不能为空")
    @Valid
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull(message = "指标ID不能为空")
        private Long metricId;

        @NotNull(message = "测量值不能为空")
        private BigDecimal metricValue;
    }
}
