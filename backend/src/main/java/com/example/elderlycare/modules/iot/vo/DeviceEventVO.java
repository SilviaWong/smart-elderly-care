package com.example.elderlycare.modules.iot.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Schema(description = "设备事件视图对象")
public class DeviceEventVO {

    @Schema(description = "事件主键ID")
    private Long id;

    @Schema(description = "机构ID")
    private Long orgId;

    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "设备编码")
    private String deviceCode;

    @Schema(description = "设备名称")
    private String deviceName;

    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "设备位置")
    private String installLocation;

    @Schema(description = "关联合同长者ID")
    private Long elderId;

    @Schema(description = "长者姓名")
    private String elderName;

    @Schema(description = "事件类型")
    private String eventType;

    @Schema(description = "事件类型中文名")
    private String eventTypeName;

    @Schema(description = "事件严重等级: LOW, MEDIUM, HIGH, CRITICAL")
    private String severity;

    @Schema(description = "严重等级中文名")
    private String severityName;

    @Schema(description = "事件负载详情 JSON")
    private String eventPayload;

    @Schema(description = "处理状态: OPEN, PROCESSING, CLOSED, IGNORED")
    private String eventStatus;

    @Schema(description = "处理状态中文名")
    private String eventStatusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "处理人ID")
    private Long handledBy;

    @Schema(description = "处理人姓名")
    private String handledByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理完成时间")
    private LocalDateTime handledAt;

    @Schema(description = "处置结论与措施")
    private String handleResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "告警入库时间")
    private LocalDateTime createdAt;
}
