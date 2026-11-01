package com.example.elderlycare.modules.dining.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "餐次菜单创建/配置参数")
public class MealMenuCreateDTO {

    @NotNull(message = "菜单日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "菜单日期")
    private LocalDate menuDate;

    @NotBlank(message = "餐次类型不能为空")
    @Schema(description = "餐次类型: BREAKFAST(早餐), LUNCH(午餐), DINNER(晚餐)")
    private String mealType;

    @Schema(description = "发布状态: PUBLISHED(已发布), DRAFT(草稿)")
    private String status;

    @NotEmpty(message = "菜单必须包含至少一道菜品")
    @Schema(description = "所选菜品ID集合")
    private List<Long> dishIds;
}
