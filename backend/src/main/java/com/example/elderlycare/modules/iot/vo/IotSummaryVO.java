package com.example.elderlycare.modules.iot.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "物联网与设备综合看板统计指标")
public class IotSummaryVO {

    @Schema(description = "设备总台数")
    private Long totalDevices;

    @Schema(description = "正常在线设备数")
    private Long activeDevices;

    @Schema(description = "离线设备数")
    private Long offlineDevices;

    @Schema(description = "故障异常设备数")
    private Long faultDevices;

    @Schema(description = "已绑定就养设备数")
    private Long boundDevices;

    @Schema(description = "空闲未绑定设备数")
    private Long unboundDevices;

    @Schema(description = "待处置设备告警事件数 (OPEN)")
    private Long openEventCount;

    @Schema(description = "今日累计接收遥测数据条数")
    private Long todayTelemetryCount;
}
