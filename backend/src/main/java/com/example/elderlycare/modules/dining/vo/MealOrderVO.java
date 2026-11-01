package com.example.elderlycare.modules.dining.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订餐订单视图对象")
public class MealOrderVO {

    @Schema(description = "订单主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "订单流水号")
    private String orderNo;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "长者编号")
    private String elderNo;

    @Schema(description = "长者床位位置 (楼栋-房间-床位)")
    private String bedInfo;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "就餐日期")
    private LocalDate orderDate;

    @Schema(description = "餐次代码: BREAKFAST, LUNCH, DINNER")
    private String mealType;

    @Schema(description = "餐次名称")
    private String mealTypeName;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态: CONFIRMED(已确认), DELIVERED(已配送/完成), CANCELLED(已取消)")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "下单人用户ID")
    private Long orderedBy;

    @Schema(description = "下单人真实姓名")
    private String orderedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间")
    private LocalDateTime createdAt;

    @Schema(description = "是否已过取消截止时间")
    private Boolean isPastCancelCutoff;

    @Schema(description = "订单菜品明细")
    private List<MealOrderItemVO> items;
}
