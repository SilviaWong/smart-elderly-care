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
@Schema(description = "餐饮运营看板统计指标")
public class DiningSummaryVO {

    @Schema(description = "在售菜品总数")
    private Long activeDishCount;

    @Schema(description = "今日已发布菜单数")
    private Long todayMenuCount;

    @Schema(description = "今日订单总数")
    private Long todayOrderCount;

    @Schema(description = "待配餐/待配送单数")
    private Long pendingDeliverCount;

    @Schema(description = "今日已完成配送单数")
    private Long deliveredCount;

    @Schema(description = "今日订餐流水总额")
    private BigDecimal todayRevenue;
}
