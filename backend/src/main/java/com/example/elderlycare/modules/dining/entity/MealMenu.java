package com.example.elderlycare.modules.dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日餐次菜单实体 (meal_menu)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("meal_menu")
public class MealMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private LocalDate menuDate;

    private String mealType;

    private String status;

    private Long createdBy;

    private LocalDateTime createdAt;
}
