package com.example.elderlycare.modules.care.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskQueryDTO {

    private String taskNo;

    private Long planId;

    private Long elderId;

    private String elderName;

    private Long assigneeId;

    private String status;

    private LocalDate taskDate;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
