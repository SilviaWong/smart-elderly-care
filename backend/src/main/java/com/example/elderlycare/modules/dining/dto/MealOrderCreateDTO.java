package com.example.elderlycare.modules.dining.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "长者订餐下单参数")
public class MealOrderCreateDTO {

    @NotNull(message = "长者ID不能为空")
    @Schema(description = "长者ID")
    private Long elderId;

    @NotNull(message = "就餐日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "就餐日期")
    private LocalDate orderDate;

    @NotBlank(message = "餐次类型不能为空")
    @Schema(description = "餐次: BREAKFAST, LUNCH, DINNER")
    private String mealType;

    @NotEmpty(message = "订餐菜品明细不能为空")
    @Valid
    @Schema(description = "菜品选购明细列表")
    private List<MealOrderItemDTO> items;

    @Schema(description = "是否确认忽略过敏/禁忌提醒强行下单（二次确认）")
    private Boolean overrideWarning;
}
