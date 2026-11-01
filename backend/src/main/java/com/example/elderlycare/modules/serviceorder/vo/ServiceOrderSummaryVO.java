package com.example.elderlycare.modules.serviceorder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderSummaryVO {

    private Integer totalOrders;

    private Integer pendingAssignCount;

    private Integer assignedCount;

    private Integer inProgressCount;

    private Integer pendingConfirmCount;

    private Integer completedCount;
}
