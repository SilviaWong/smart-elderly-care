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
public class HealthRecordQueryDTO {

    private Long elderId;

    private String elderName;

    private Long metricId;

    private String sourceType;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
