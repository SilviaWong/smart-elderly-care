package com.example.elderlycare.modules.serviceorder.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceOrderCreateDTO {

    @NotNull(message = "服务长者不能为空")
    private Long elderId;

    private Long admissionId;

    @NotNull(message = "服务项目不能为空")
    private Long serviceItemId;

    private String sourceType;

    private String priority;

    private LocalDateTime expectedStartAt;

    private Long assigneeId;

    private String requestDesc;
}
