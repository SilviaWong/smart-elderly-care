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
public class CareItemDTO {

    @NotBlank(message = "护理项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "护理项目名称不能为空")
    private String itemName;

    private String category;

    private String description;

    @Builder.Default
    private String status = "ACTIVE";
}
