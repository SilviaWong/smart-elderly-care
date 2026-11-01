package com.example.elderlycare.modules.regulatory.entity;

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
 * 监管报送批次实体 (regulatory_batch)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("regulatory_batch")
public class RegulatoryBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orgId;

    private String batchNo;

    private Long templateId;

    private LocalDate periodStart;

    private LocalDate periodEnd;

    private String status; // GENERATED, VALIDATED, SUBMITTED, REJECTED, CONFIRMED

    private String fileUrl;

    private String validationResultJson;

    private LocalDateTime submittedAt;

    private String responseDesc;

    private Long createdBy;

    private LocalDateTime createdAt;
}
