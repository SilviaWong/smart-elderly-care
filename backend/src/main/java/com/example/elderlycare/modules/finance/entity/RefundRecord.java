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
 * 退款记录实体 (refund_record)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("refund_record")
public class RefundRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String refundNo;

    private Long paymentId;

    private BigDecimal amount;

    private String reason;

    private String status;

    private Long approvedBy;

    private LocalDateTime refundedAt;

    private Long createdBy;

    private LocalDateTime createdAt;
}
