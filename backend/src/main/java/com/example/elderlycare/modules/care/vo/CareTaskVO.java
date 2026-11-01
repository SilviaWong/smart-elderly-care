package com.example.elderlycare.modules.care.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long orgId;

    private String taskNo;

    private Long planId;

    private String planNo;

    private Long planItemId;

    private Long careItemId;

    private String careItemCode;

    private String careItemName;

    private String category;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedInfo;

    private LocalDate taskDate;

    private LocalDateTime scheduledAt;

    private Long assigneeId;

    private String assigneeName;

    private String status;

    private String idempotencyKey;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
