package com.example.elderlycare.modules.care.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareTaskGenerateDTO {

    /**
     * 生成任务的目标日期（默认为当天）
     */
    @NotNull(message = "生成任务日期不能为空")
    private LocalDate taskDate;

    /**
     * 可选：指定长者ID（若为空则为机构内所有在住长者生成）
     */
    private Long elderId;
}
