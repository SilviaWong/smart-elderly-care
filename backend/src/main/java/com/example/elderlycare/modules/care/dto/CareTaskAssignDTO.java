package com.example.elderlycare.modules.care.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskAssignDTO {

    @NotNull(message = "指派护理责任人不能为空")
    private Long assigneeId;
}
