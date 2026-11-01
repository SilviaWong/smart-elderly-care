package com.example.elderlycare.modules.care.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long planId;

    private Long careItemId;

    private String itemCode;

    private String itemName;

    private String category;

    private String frequencyType;

    private String frequencyValue;

    private LocalTime timeWindowStart;

    private LocalTime timeWindowEnd;

    private Long assigneeId;

    private String assigneeName;

    private Integer enabled;
}
