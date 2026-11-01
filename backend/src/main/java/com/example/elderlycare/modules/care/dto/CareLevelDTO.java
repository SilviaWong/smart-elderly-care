package com.example.elderlycare.modules.care.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareLevelDTO {

    @NotBlank(message = "护理等级编码不能为空")
    private String levelCode;

    @NotBlank(message = "护理等级名称不能为空")
    private String levelName;

    private String description;

    @Builder.Default
    private String status = "ACTIVE";
}
