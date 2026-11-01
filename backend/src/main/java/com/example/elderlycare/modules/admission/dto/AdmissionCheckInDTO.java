package com.example.elderlycare.modules.admission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionCheckInDTO {

    @NotNull(message = "长者不能为空")
    private Long elderId;

    @NotNull(message = "目标床位不能为空")
    private Long bedId;

    @NotNull(message = "入住日期不能为空")
    private LocalDate admissionDate;

    private LocalDate plannedDischargeDate;

    private String careLevelCode;

    private BigDecimal depositAmount;

    private String contractNo;

    private String remark;
}
