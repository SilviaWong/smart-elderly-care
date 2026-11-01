package com.example.elderlycare.modules.iot.adapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 经由适配器标准化转换后的通用 IoT 数据载荷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotNormalizedPayload {

    /**
     * 采集时间
     */
    private LocalDateTime collectedAt;

    /**
     * 设备状态推断: ACTIVE, OFFLINE, FAULT 等
     */
    private String deviceStatus;

    /**
     * 标准化指标列表
     */
    @Builder.Default
    private List<NormalizedMetric> metrics = new ArrayList<>();

    /**
     * 识别出的设备事件/异常告警
     */
    @Builder.Default
    private List<NormalizedEvent> events = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NormalizedMetric {
        private String metricCode;
        private BigDecimal metricValue;
        private String unit;
        private LocalDateTime collectedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NormalizedEvent {
        private String eventType;
        private String severity; // LOW, MEDIUM, HIGH, CRITICAL
        private String eventPayload;
        private LocalDateTime occurredAt;
        private String message;
    }
}
