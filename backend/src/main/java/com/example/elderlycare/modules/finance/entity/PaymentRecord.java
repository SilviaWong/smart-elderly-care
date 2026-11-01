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
import java.time.LocalDateTime;

/**
 * 收款记录实体 (payment_record)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_record")
public class PaymentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String paymentNo;

    private Long billingId;

    private BigDecimal amount;

    private String paymentMethod;

    private String transactionNo;

    private String idempotencyKey;

    private String status;

    private LocalDateTime paidAt;

    private Long operatorId;

    private LocalDateTime createdAt;
}
