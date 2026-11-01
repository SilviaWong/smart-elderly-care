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
 * 菜品实体 (dish)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("dish")
public class Dish implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String dishCode;

    private String dishName;

    private String category;

    private BigDecimal price;

    private String allergens;

    private String description;

    private String status;
}
