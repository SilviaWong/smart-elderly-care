package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备事件处理参数")
public class DeviceEventHandleDTO {

    @NotBlank(message = "处理状态不能为空")
    @Schema(description = "目标状态: PROCESSING(处置中), CLOSED(处置完毕结案), IGNORED(误报忽略)")
    private String eventStatus;

    @NotBlank(message = "处理结论不能为空")
    @Schema(description = "现场复核情况及处理结论说明")
    private String handleResult;
}
