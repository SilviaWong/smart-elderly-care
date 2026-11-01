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
@Schema(description = "设备创建或编辑参数")
public class DeviceDTO {

    @NotBlank(message = "设备编码不能为空")
    @Schema(description = "设备唯一编码，如 DEV_WATCH_001")
    private String deviceCode;

    @NotBlank(message = "设备名称不能为空")
    @Schema(description = "设备名称，如 华为智能体征手环")
    private String deviceName;

    @NotBlank(message = "设备类型不能为空")
    @Schema(description = "设备类型: SMART_WATCH, SMART_MATTRESS, BLOOD_PRESSURE, BLOOD_GLUCOSE, SOS_BUTTON, RADAR_FALL, THERMOMETER")
    private String deviceType;

    @Schema(description = "设备厂商，如 HUAWEI, SleepAce, OMRON, TuyaSmart, Hikvision")
    private String vendor;

    @Schema(description = "设备规格型号")
    private String model;

    @Schema(description = "出厂序列号 (SN)")
    private String serialNo;

    @Schema(description = "默认安装或摆放位置")
    private String installLocation;

    @Schema(description = "设备初始状态: ACTIVE, INACTIVE, OFFLINE, FAULT")
    private String status;
}
