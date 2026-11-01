package com.example.elderlycare.modules.finance.entity;

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

/**
 * 账单收费明细项实体 (billing_item)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("billing_item")
public class BillingItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billingId;

    private Long feeItemId;

    private String itemNameSnapshot;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;
}
