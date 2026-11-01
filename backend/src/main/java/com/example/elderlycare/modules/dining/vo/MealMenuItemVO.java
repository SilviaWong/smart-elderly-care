package com.example.elderlycare.modules.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "菜单项视图对象")
public class MealMenuItemVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品编码")
    private String dishCode;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "菜品分类")
    private String category;

    @Schema(description = "菜品分类中文名")
    private String categoryName;

    @Schema(description = "菜单发布时价格快照")
    private BigDecimal priceSnapshot;

    @Schema(description = "过敏原信息")
    private String allergens;

    @Schema(description = "菜品介绍")
    private String description;
}
