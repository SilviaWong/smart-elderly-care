package com.example.elderlycare.modules.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订餐菜品明细参数")
public class MealOrderItemDTO {

    @NotNull(message = "菜品ID不能为空")
    @Schema(description = "菜品ID")
    private Long dishId;

    @NotNull(message = "菜品份数不能为空")
    @Min(value = 1, message = "菜品份数至少为 1 份")
    @Schema(description = "份数，默认 1")
    private Integer quantity;
}
