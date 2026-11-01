package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备解绑参数")
public class DeviceUnbindDTO {

    @NotNull(message = "设备ID不能为空")
    @Schema(description = "解绑设备ID")
    private Long deviceId;

    @Schema(description = "解绑说明/原因备注")
    private String remark;
}
