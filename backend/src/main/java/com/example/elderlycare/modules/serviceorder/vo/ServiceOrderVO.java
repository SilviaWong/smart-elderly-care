package com.example.elderlycare.modules.serviceorder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderVO {

    private Long id;

    private Long orgId;

    private String orderNo;

    private Long elderId;

    private String elderName;

    private String elderNo;

    private String bedInfo;

    private Long serviceItemId;

    private String itemName;

    private String itemCode;

    private String category;

    private BigDecimal standardPrice;

    private Integer needConfirm;

    private String sourceType;

    private String priority;

    private LocalDateTime expectedStartAt;

    private Long assigneeId;

    private String assigneeName;

    private Long creatorId;

    private String creatorName;

    private String status;

    private String requestDesc;

    private String executionResult;

    private LocalDateTime completedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
