package com.example.elderlycare.modules.admission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionQueryDTO {

    private String admissionNo;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedNo;

    private String status;

    private Long buildingId;

    private LocalDate startDate;

    private LocalDate endDate;

    @Builder.Default
    private Long pageNum = 1L;

    @Builder.Default
    private Long pageSize = 10L;
}
