package com.example.elderlycare.modules.admission.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BedBoardSummaryVO {

    private Integer totalBeds;

    private Integer occupiedBeds;

    private Integer availableBeds;

    private Integer maintenanceBeds;

    private Integer disabledBeds;

    /**
     * 入住率百分比 (0.00% - 100.00%)
     */
    private BigDecimal occupancyRate;
}
