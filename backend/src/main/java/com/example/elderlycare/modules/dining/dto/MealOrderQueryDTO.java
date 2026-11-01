package com.example.elderlycare.modules.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订餐订单分页查询参数")
public class MealOrderQueryDTO {

    @Schema(description = "长者ID")
    private Long elderId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "订餐就餐日期")
    private LocalDate orderDate;

    @Schema(description = "餐次类型: BREAKFAST, LUNCH, DINNER")
    private String mealType;

    @Schema(description = "订单状态: CONFIRMED(已确认), DELIVERED(已配送/完成), CANCELLED(已取消)")
    private String status;

    @Schema(description = "关键字（单号/长者姓名/菜品）")
    private String keyword;

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
