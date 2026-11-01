package com.example.elderlycare.modules.care.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanItemDTO {

    private Long id;

    @NotNull(message = "护理项目不能为空")
    private Long careItemId;

    @NotBlank(message = "执行频次类型不能为空（DAILY/WEEKLY/INTERVAL_DAYS）")
    @Builder.Default
    private String frequencyType = "DAILY";

    @Builder.Default
    private String frequencyValue = "1";

    private LocalTime timeWindowStart;

    private LocalTime timeWindowEnd;

    private Long assigneeId;

    @Builder.Default
    private Integer enabled = 1;
}
