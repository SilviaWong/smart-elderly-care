package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备绑定参数")
public class DeviceBindDTO {

    @NotNull(message = "设备ID不能为空")
    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "绑定的长者ID (随身设备如手环、手表必填)")
    private Long elderId;

    @Schema(description = "绑定的床位ID (空间/床位设备如智能床垫选填)")
    private Long bedId;

    @NotBlank(message = "绑定类型不能为空")
    @Schema(description = "绑定类型: ELDER(长者个人随身), BED(床位), SPACE(公共/房间空间)")
    private String bindType;
}
