package com.example.elderlycare.modules.admission.entity;

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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("admission")
public class Admission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String admissionNo;

    private Long elderId;

    private Long bedId;

    private String contractNo;

    private LocalDate admissionDate;

    private LocalDate plannedDischargeDate;

    private LocalDate actualDischargeDate;

    private String careLevelCode;

    private String status;

    private BigDecimal depositAmount;

    private String dischargeReason;

    private Long operatorId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
