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
import java.time.LocalDateTime;

/**
 * 月度账单主实体 (billing)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("billing")
public class Billing implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String billNo;

    private Long elderId;

    private Long admissionId;

    private String billMonth;

    private LocalDate billDate;

    private LocalDate dueDate;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private String status;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;
}
