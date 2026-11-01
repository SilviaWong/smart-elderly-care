package com.example.elderlycare.modules.serviceorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderAssignDTO {

    @NotNull(message = "指派护理人员不能为空")
    private Long assigneeId;

    private String remark;
}
