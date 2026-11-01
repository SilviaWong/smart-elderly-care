package com.example.elderlycare.modules.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "菜品新增/编辑参数")
public class DishDTO {

    @NotBlank(message = "菜品编码不能为空")
    @Schema(description = "菜品编码")
    private String dishCode;

    @NotBlank(message = "菜品名称不能为空")
    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "分类: STAPLE(主食), MEAT(荤菜), VEGETABLE(素菜), SOUP(汤羹), FRUIT(水果点心), SPECIAL(特需食疗)")
    private String category;

    @NotNull(message = "菜品单价不能为空")
    @DecimalMin(value = "0.00", message = "菜品单价不能低于 0 元")
    @Schema(description = "菜品单价")
    private BigDecimal price;

    @Schema(description = "常见过敏原成分，如: 海鲜、花生、蛋类、乳制品、麸质等")
    private String allergens;

    @Schema(description = "烹饪特点与营养风味描述")
    private String description;

    @Schema(description = "启用状态: ACTIVE(在售), INACTIVE(停售)")
    private String status;
}
