package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备遥测数据查询参数")
public class DeviceDataQueryDTO {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "长者ID (查询长者绑定设备上报的历史遥测数据)")
    private Long elderId;

    @Schema(description = "指标编码，如 HEART_RATE, SPO2, SBP, DBP, IN_BED_STATUS")
    private String metricCode;

    @Schema(description = "采集起始时间")
    private LocalDateTime startTime;

    @Schema(description = "采集结束时间")
    private LocalDateTime endTime;

    @Schema(description = "页码，默认 1")
    private Integer pageNum;

    @Schema(description = "每页条数，默认 10")
    private Integer pageSize;

    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public int getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }
}
