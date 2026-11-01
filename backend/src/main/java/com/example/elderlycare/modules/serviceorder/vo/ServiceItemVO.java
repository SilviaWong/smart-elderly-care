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
public class ServiceItemVO {

    private Long id;

    private Long orgId;

    private String itemCode;

    private String itemName;

    private String category;

    private String unit;

    private BigDecimal standardPrice;

    private Integer needConfirm;

    private String description;

    private String status;

    private LocalDateTime createdAt;
}
