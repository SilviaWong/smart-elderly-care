package com.example.elderlycare.modules.admission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionDischargeDTO {

    @NotNull(message = "实际退住日期不能为空")
    private LocalDate actualDischargeDate;

    private String dischargeReason;

    /**
     * 是否在存在可配置校验告警时强制退住
     */
    private Boolean forceDischarge;
}
