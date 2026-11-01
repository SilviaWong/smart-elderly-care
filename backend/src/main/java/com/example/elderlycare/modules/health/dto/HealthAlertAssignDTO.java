package com.example.elderlycare.modules.health.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthAlertAssignDTO {

    @NotNull(message = "处理责任人不能为空")
    private Long assigneeId;
}
