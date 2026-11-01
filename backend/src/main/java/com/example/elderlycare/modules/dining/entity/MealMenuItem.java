package com.example.elderlycare.modules.dining.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 每日菜单菜品项实体 (meal_menu_item)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("meal_menu_item")
public class MealMenuItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long menuId;

    private Long dishId;

    private BigDecimal priceSnapshot;
}
