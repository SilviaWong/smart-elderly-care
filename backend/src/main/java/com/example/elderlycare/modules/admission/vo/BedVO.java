package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedVO {

    private Long id;

    private Long orgId;

    private Long roomId;

    private String roomNo;

    private Long buildingId;

    private String buildingName;

    private String floorNo;

    private String bedNo;

    private String bedType;

    private String status;

    private Integer version;

    private Long currentAdmissionId;

    private Long currentElderId;

    private String currentElderName;

    private String currentElderNo;

    private LocalDate admissionDate;

    private LocalDateTime createdAt;
}
