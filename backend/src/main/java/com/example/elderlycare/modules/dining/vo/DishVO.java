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
@Schema(description = "菜品视图对象")
public class DishVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "菜品编码")
    private String dishCode;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "分类代码")
    private String category;

    @Schema(description = "分类中文名称")
    private String categoryName;

    @Schema(description = "菜品单价")
    private BigDecimal price;

    @Schema(description = "过敏原提示")
    private String allergens;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "状态: ACTIVE, INACTIVE")
    private String status;

    @Schema(description = "状态中文名")
    private String statusName;
}
