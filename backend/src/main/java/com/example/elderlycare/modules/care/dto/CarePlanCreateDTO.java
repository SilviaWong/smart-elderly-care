package com.example.elderlycare.modules.care.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanCreateDTO {

    @NotNull(message = "服务长者不能为空")
    private Long elderId;

    @NotNull(message = "护理等级不能为空")
    private Long careLevelId;

    @NotNull(message = "计划生效起始日期不能为空")
    private LocalDate startDate;

    private LocalDate endDate;

    private String remark;

    @NotEmpty(message = "护理计划明细项目至少需要一项")
    @Valid
    private List<CarePlanItemDTO> items;
}
