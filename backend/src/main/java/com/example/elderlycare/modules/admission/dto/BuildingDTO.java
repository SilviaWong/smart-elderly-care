package com.example.elderlycare.modules.admission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingDTO {

    @NotBlank(message = "楼栋名称不能为空")
    private String buildingName;

    @NotBlank(message = "楼栋编码不能为空")
    private String buildingCode;

    private String address;

    private String status;
}
