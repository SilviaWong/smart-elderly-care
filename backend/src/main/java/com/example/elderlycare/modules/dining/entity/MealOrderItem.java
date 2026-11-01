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
 * 订餐订单菜品明细实体 (meal_order_item)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("meal_order_item")
public class MealOrderItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long dishId;

    private String dishNameSnapshot;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal lineAmount;
}
