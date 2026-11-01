package com.example.elderlycare.modules.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "风险事件上报登记参数")
public class RiskEventReportDTO {

    @Schema(description = "关联长者ID（若为园区公共设施事件可为空）")
    private Long elderId;

    @NotBlank(message = "事件类型不能为空")
    @Schema(description = "事件类型: FALL(跌倒), LOST(走失), SUDDEN_ILLNESS(突发急症), DEVICE_SOS(设备求助), CHOKING(噎食), CONFLICT(冲突纠纷), OTHER(其他)")
    private String eventType;

    @NotBlank(message = "严重程度不能为空")
    @Schema(description = "严重等级: LOW(低/轻微), MEDIUM(中/一般), HIGH(高/严重), CRITICAL(危急/特大)")
    private String severity;

    @NotNull(message = "发生时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发生时间")
    private LocalDateTime occurredAt;

    @Size(max = 255, message = "发生地点描述不能超过255个字符")
    @Schema(description = "发生具体地点")
    private String locationDesc;

    @NotBlank(message = "现场情况描述不能为空")
    @Schema(description = "事件详细描述")
    private String description;

    @Schema(description = "指派处理负责人用户ID（可选）")
    private Long assigneeId;
}
