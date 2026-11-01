package com.example.elderlycare.modules.serviceorder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderLogVO {

    private Long id;

    private Long orderId;

    private String fromStatus;

    private String toStatus;

    private String action;

    private Long operatorId;

    private String operatorName;

    private String remark;

    private LocalDateTime createdAt;
}
