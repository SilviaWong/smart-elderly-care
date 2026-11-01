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
@Schema(description = "订单菜品明细视图对象")
public class MealOrderItemVO {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "菜品ID")
    private Long dishId;

    @Schema(description = "菜品名称快照（不受后续改名改价影响）")
    private String dishNameSnapshot;

    @Schema(description = "份数")
    private Integer quantity;

    @Schema(description = "购买单价快照")
    private BigDecimal unitPrice;

    @Schema(description = "小计金额")
    private BigDecimal lineAmount;
}
