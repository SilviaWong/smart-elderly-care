package com.example.elderlycare.modules.serviceorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItemDTO {

    @NotBlank(message = "项目编码不能为空")
    private String itemCode;

    @NotBlank(message = "项目名称不能为空")
    private String itemName;

    private String category;

    private String unit;

    @NotNull(message = "标准价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal standardPrice;

    @NotNull(message = "是否需要验收确认不能为空")
    private Integer needConfirm;

    private String description;

    private String status;
}
