package com.example.elderlycare.modules.admission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdmissionTransferDTO {

    @NotNull(message = "目标新床位不能为空")
    private Long newBedId;

    private String reason;
}
