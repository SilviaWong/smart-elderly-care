package com.example.elderlycare.modules.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertQueryDTO {

    private String alertNo;

    private Long elderId;

    private String elderName;

    private Long metricId;

    private Long ruleId;

    private String severity;

    private String status;

    private Long assigneeId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
