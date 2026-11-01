package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "设备事件查询参数")
public class DeviceEventQueryDTO {

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "长者ID")
    private Long elderId;

    @Schema(description = "事件类型: SOS, FALL_DETECTED, LOW_BATTERY, OFFLINE, ABNORMAL_METRIC, LEAVE_BED_OVERTIME")
    private String eventType;

    @Schema(description = "事件严重等级: LOW, MEDIUM, HIGH, CRITICAL")
    private String severity;

    @Schema(description = "处理状态: OPEN(待处理), PROCESSING(处理中), CLOSED(已办结), IGNORED(已忽略)")
    private String eventStatus;

    @Schema(description = "关键词 (设备编码/处理结果)")
    private String keyword;

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
