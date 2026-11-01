package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionVO {

    private Long id;

    private Long orgId;

    private String admissionNo;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String elderGender;

    private Integer elderAge;

    private String elderPhone;

    private Long bedId;

    private String bedNo;

    private Long roomId;

    private String roomNo;

    private Long buildingId;

    private String buildingName;

    private String contractNo;

    private LocalDate admissionDate;

    private LocalDate plannedDischargeDate;

    private LocalDate actualDischargeDate;

    private String careLevelCode;

    private String status;

    private BigDecimal depositAmount;

    private String dischargeReason;

    private String operatorName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
