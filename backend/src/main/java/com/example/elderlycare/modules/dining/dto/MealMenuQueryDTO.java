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
@Schema(description = "菜单查询参数")
public class MealMenuQueryDTO {

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "菜单日期")
    private LocalDate menuDate;

    @Schema(description = "餐次类型: BREAKFAST, LUNCH, DINNER")
    private String mealType;

    @Schema(description = "状态: PUBLISHED, DRAFT")
    private String status;

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
