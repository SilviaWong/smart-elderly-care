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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订餐订单实体 (meal_order)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("meal_order")
public class MealOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String orderNo;

    private Long elderId;

    private LocalDate orderDate;

    private String mealType;

    private BigDecimal totalAmount;

    private String status;

    private Long orderedBy;

    private LocalDateTime createdAt;
}
