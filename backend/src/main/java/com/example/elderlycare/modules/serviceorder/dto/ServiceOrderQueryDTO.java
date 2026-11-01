package com.example.elderlycare.modules.serviceorder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderQueryDTO {

    private String orderNo;

    private Long elderId;

    private String elderName;

    private Long serviceItemId;

    private Long assigneeId;

    private String status;

    private String priority;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
