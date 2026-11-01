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
import java.util.ArrayList;
import java.util.List;

/**
 * 华为健康 / 华为云 IoT 厂商私有协议适配器
 * 示例报文格式：
 * {
 *   "services": [
 *     {
 *       "service_id": "vital_signs",
 *       "properties": { "heart_rate": 78, "spo2": 99, "battery_pct": 85 }
 *     }
 *   ]
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HuaweiHealthIotAdapter implements IotDeviceAdapter {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(String vendor, String protocol) {
        if ("HUAWEI_CLOUD".equalsIgnoreCase(protocol) || "HUAWEI_HEALTH".equalsIgnoreCase(protocol)) {
            return true;
        }
        return StringUtils.hasText(vendor) && vendor.toUpperCase().contains("HUAWEI");
    }

    @Override
    public IotNormalizedPayload normalize(Device device, String rawPayload) {
        LocalDateTime now = LocalDateTime.now();
        List<IotNormalizedPayload.NormalizedMetric> metrics = new ArrayList<>();
        List<IotNormalizedPayload.NormalizedEvent> events = new ArrayList<>();

        if (!StringUtils.hasText(rawPayload)) {
            return IotNormalizedPayload.builder().collectedAt(now).deviceStatus("ACTIVE").metrics(metrics).events(events).build();
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            if (root.has("services") && root.get("services").isArray()) {
                for (JsonNode sNode : root.get("services")) {
                    JsonNode props = sNode.path("properties");
                    if (props.isObject()) {
                        if (props.hasNonNull("heart_rate")) {
                            metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                                    .metricCode("HEART_RATE")
                                    .metricValue(new BigDecimal(props.get("heart_rate").asText()))
                                    .unit("bpm")
                                    .collectedAt(now)
                                    .build());
                        }
                        if (props.hasNonNull("spo2")) {
                            metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                                    .metricCode("SPO2")
                                    .metricValue(new BigDecimal(props.get("spo2").asText()))
                                    .unit("%")
                                    .collectedAt(now)
                                    .build());
                        }
                        if (props.hasNonNull("battery_pct")) {
                            metrics.add(IotNormalizedPayload.NormalizedMetric.builder()
                                    .metricCode("BATTERY")
                                    .metricValue(new BigDecimal(props.get("battery_pct").asText()))
                                    .unit("%")
                                    .collectedAt(now)
                                    .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("华为协议报文解析失败: {}", e.getMessage());
        }

        return IotNormalizedPayload.builder()
                .collectedAt(now)
                .deviceStatus("ACTIVE")
                .metrics(metrics)
                .events(events)
                .build();
    }
}
