package com.example.elderlycare.modules.dining.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "餐次菜单视图对象")
public class MealMenuVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "菜单日期")
    private LocalDate menuDate;

    @Schema(description = "餐次代码: BREAKFAST, LUNCH, DINNER")
    private String mealType;

    @Schema(description = "餐次名称")
    private String mealTypeName;

    @Schema(description = "状态代码: PUBLISHED, DRAFT")
    private String status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "创建人用户ID")
    private Long createdBy;

    @Schema(description = "创建人姓名")
    private String createdByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "菜品总数")
    private Integer dishCount;

    @Schema(description = "菜单菜品明细列表")
    private List<MealMenuItemVO> items;
}
