package com.example.elderlycare.modules.health.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertHandleDTO {

    /**
     * 处置状态：RESOLVED (已处理), IGNORED (已忽略)
     */
    @NotBlank(message = "处理状态不能为空（RESOLVED/IGNORED）")
    private String status;

    @NotBlank(message = "处置说明及复核记录不能为空")
    private String handleResult;
}
