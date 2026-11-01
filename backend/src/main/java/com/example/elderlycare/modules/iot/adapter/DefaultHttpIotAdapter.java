package com.example.elderlycare.modules.iot.adapter;

import com.example.elderlycare.modules.iot.entity.Device;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认标准 HTTP / JSON 报文适配器 (支持模拟器以及标准开放平台协议)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultHttpIotAdapter implements IotDeviceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String vendor, String protocol) {
        if (!StringUtils.hasText(protocol) || "STANDARD".equalsIgnoreCase(protocol) || "DEFAULT".equalsIgnoreCase(protocol)) {
            return true;
        }
        return !StringUtils.hasText(vendor) || "DEFAULT".equalsIgnoreCase(vendor) || "STANDARD".equalsIgnoreCase(vendor);
    }

    @Override
    public IotNormalizedPayload normalize(Device device, String rawPayload) {
        LocalDateTime now = LocalDateTime.now();
        List<IotNormalizedPayload.NormalizedMetric> metrics = new ArrayList<>();
        List<IotNormalizedPayload.NormalizedEvent> events = new ArrayList<>();

        if (!StringUtils.hasText(rawPayload)) {
            return IotNormalizedPayload.builder()
                    .collectedAt(now)
                    .deviceStatus("ACTIVE")
                    .metrics(metrics)
                    .events(events)
                    .build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);

            // 1. 采集时间解析
            LocalDateTime collectedAt = now;
            if (root.hasNonNull("collectedAt")) {
                try {
                    String timeStr = root.get("collectedAt").asText();
                    if (timeStr.contains("T")) {
                        collectedAt = LocalDateTime.parse(timeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } else if (timeStr.contains(" ")) {
                        collectedAt = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                } catch (Exception e) {
                    log.warn("IoT报文采集时间格式解析异常，回退使用系统当前时间: {}", e.getMessage());
                }
            }

            // 2. 解析标准化 metrics 数组
            if (root.has("metrics") && root.get("metrics").isArray()) {
                for (JsonNode mNode : root.get("metrics")) {
                    String code = mNode.path("metricCode").asText(null);
                    if (StringUtils.hasText(code) && mNode.hasNonNull("metricValue")) {
                        BigDecimal val = new BigDecimal(mNode.get("metricValue").asText());
                        String unit = mNode.path("unit").asText("");
                        metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                                .metricCode(code.trim().toUpperCase())
                                .metricValue(val)
                                .unit(unit)
                                .collectedAt(collectedAt)
                                .build());
                    }
                }
            }

            // 3. 解析常规平面字段 (心率、血压、体温、血氧、呼吸、电量、离床等)
            parseFlatMetric(root, "heartRate", "HEART_RATE", "bpm", collectedAt, metrics);
            parseFlatMetric(root, "respiration", "RESPIRATORY_RATE", "次/分", collectedAt, metrics);
            parseFlatMetric(root, "spo2", "SPO2", "%", collectedAt, metrics);
            parseFlatMetric(root, "systolic", "SBP", "mmHg", collectedAt, metrics);
            parseFlatMetric(root, "diastolic", "DBP", "mmHg", collectedAt, metrics);
            parseFlatMetric(root, "bloodSugar", "BLOOD_SUGAR", "mmol/L", collectedAt, metrics);
            parseFlatMetric(root, "temperature", "BODY_TEMP", "℃", collectedAt, metrics);
            parseFlatMetric(root, "stepCount", "STEP_COUNT", "steps", collectedAt, metrics);
            parseFlatMetric(root, "battery", "BATTERY", "%", collectedAt, metrics);

            if (root.hasNonNull("inBed")) {
                boolean inBed = root.get("inBed").asBoolean();
                metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                        .metricCode("IN_BED_STATUS")
                        .metricValue(inBed ? BigDecimal.ONE : BigDecimal.ZERO)
                        .unit("状态")
                        .collectedAt(collectedAt)
                        .build());
            }

            // 4. 解析预置 events 数组
            if (root.has("events") && root.get("events").isArray()) {
                for (JsonNode eNode : root.get("events")) {
                    String eventType = eNode.path("eventType").asText("ALERT");
                    String severity = eNode.path("severity").asText("MEDIUM");
                    String msg = eNode.path("message").asText("设备上报预警事件");
                    events.add(IotNormalizedPayload.NormalizedEvent.builder()
                            .eventType(eventType.toUpperCase())
                            .severity(severity.toUpperCase())
                            .eventPayload(eNode.toString())
                            .occurredAt(collectedAt)
                            .message(msg)
                            .build());
                }
            }

            // 5. 显式事件字段检测 (SOS, 跌倒, 离床超时)
            if (root.path("sos").asBoolean(false) || "SOS".equalsIgnoreCase(root.path("eventType").asText(""))) {
                events.add(IotNormalizedPayload.NormalizedEvent.builder()
                        .eventType("SOS")
                        .severity("CRITICAL")
                        .eventPayload(rawPayload)
                        .occurredAt(collectedAt)
                        .message("长者触发一键紧急求助 (SOS)")
                        .build());
            }

            if (root.path("fallDetected").asBoolean(false) || "FALL_DETECTED".equalsIgnoreCase(root.path("eventType").asText(""))) {
                events.add(IotNormalizedPayload.NormalizedEvent.builder()
                        .eventType("FALL_DETECTED")
                        .severity("CRITICAL")
                        .eventPayload(rawPayload)
                        .occurredAt(collectedAt)
                        .message("传感器检测到长者跌倒风险")
                        .build());
            }

            if (root.path("leaveBedOvertime").asBoolean(false) || "LEAVE_BED_OVERTIME".equalsIgnoreCase(root.path("eventType").asText(""))) {
                events.add(IotNormalizedPayload.NormalizedEvent.builder()
                        .eventType("LEAVE_BED_OVERTIME")
                        .severity("HIGH")
                        .eventPayload(rawPayload)
                        .occurredAt(collectedAt)
                        .message("夜间离床超时未归预警")
                        .build());
            }

            // 6. 自动度量阈值越限判定产生告警事件
            for (IotNormalizedPayload.NormalizedMetric m : metrics) {
                if ("BATTERY".equals(m.getMetricCode()) && m.getMetricValue().compareTo(new BigDecimal("15")) < 0) {
                    events.add(IotNormalizedPayload.NormalizedEvent.builder()
                            .eventType("LOW_BATTERY")
                            .severity("LOW")
                            .eventPayload(String.format("{\"battery\": %s, \"deviceCode\": \"%s\"}", m.getMetricValue(), device.getDeviceCode()))
                            .occurredAt(collectedAt)
                            .message(String.format("设备电量低 (剩余 %s%%)，请及时充电", m.getMetricValue()))
                            .build());
                } else if ("HEART_RATE".equals(m.getMetricCode())) {
                    if (m.getMetricValue().compareTo(new BigDecimal("120")) > 0 || m.getMetricValue().compareTo(new BigDecimal("45")) < 0) {
                        events.add(IotNormalizedPayload.NormalizedEvent.builder()
                                .eventType("ABNORMAL_METRIC")
                                .severity("HIGH")
                                .eventPayload(String.format("{\"metricCode\": \"HEART_RATE\", \"value\": %s}", m.getMetricValue()))
                                .occurredAt(collectedAt)
                                .message(String.format("心率读数异常 (当前 %s bpm)，请值班护士密切关注", m.getMetricValue()))
                                .build());
                    }
                } else if ("SPO2".equals(m.getMetricCode()) && m.getMetricValue().compareTo(new BigDecimal("90")) < 0) {
                    events.add(IotNormalizedPayload.NormalizedEvent.builder()
                            .eventType("ABNORMAL_METRIC")
                            .severity("HIGH")
                            .eventPayload(String.format("{\"metricCode\": \"SPO2\", \"value\": %s}", m.getMetricValue()))
                            .occurredAt(collectedAt)
                            .message(String.format("血氧浓度过低 (当前 %s%%)，存在缺氧风险", m.getMetricValue()))
                            .build());
                }
            }

            return IotNormalizedPayload.builder()
                    .collectedAt(collectedAt)
                    .deviceStatus("ACTIVE")
                    .metrics(metrics)
                    .events(events)
                    .build();

        } catch (Exception e) {
            log.error("标准化解析标准IoT报文失败: {}", e.getMessage(), e);
            return IotNormalizedPayload.builder()
                    .collectedAt(now)
                    .deviceStatus("ACTIVE")
                    .metrics(metrics)
                    .events(events)
                    .build();
        }
    }

    private void parseFlatMetric(JsonNode root, String propName, String metricCode, String unit,
                                 LocalDateTime collectedAt, List<IotNormalizedPayload.NormalizedMetric> metrics) {
        if (root.hasNonNull(propName)) {
            try {
                BigDecimal val = new BigDecimal(root.get(propName).asText());
                metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                        .metricCode(metricCode)
                        .metricValue(val)
                        .unit(unit)
                        .collectedAt(collectedAt)
                        .build());
            } catch (Exception ignored) {
            }
        }
    }
}
