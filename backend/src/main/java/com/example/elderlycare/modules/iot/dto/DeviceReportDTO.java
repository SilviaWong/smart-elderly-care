package com.example.elderlycare.modules.iot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "模拟设备数据上报参数 (支持标准JSON或厂商原始报文)")
public class DeviceReportDTO {

    @NotBlank(message = "设备编码不能为空")
    @Schema(description = "设备唯一编码，如 DEV_WATCH_001")
    private String deviceCode;

    @Schema(description = "厂商通信协议，如 STANDARD, HUAWEI_CLOUD, TUYA_DPS, 默认为 STANDARD")
    private String protocol;

    @Schema(description = "原始报文字符串 (若填写则直接交给对应厂商 Adapter 解析)")
    private String rawPayload;

    @Schema(description = "采集时间 (可选，默认系统当前时间)")
    private LocalDateTime collectedAt;

    @Schema(description = "标准指标项列表 (可选，便捷模拟测试)")
    private List<MetricItemDTO> metrics;

    @Schema(description = "上报告警事件列表 (可选)")
    private List<EventItemDTO> events;

    @Schema(description = "剩余电量百分比 (0-100)")
    private BigDecimal battery;

    @Schema(description = "是否触发 SOS 紧急求救 (true/false)")
    private Boolean sos;

    @Schema(description = "是否检测到跌倒 (true/false)")
    private Boolean fallDetected;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单项指标上报")
    public static class MetricItemDTO {
        @NotBlank(message = "指标编码不能为空")
        private String metricCode;
        private BigDecimal metricValue;
        private String unit;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "设备事件上报")
    public static class EventItemDTO {
        @NotBlank(message = "事件类型不能为空")
        private String eventType;
        private String severity;
        private String message;
    }
}
