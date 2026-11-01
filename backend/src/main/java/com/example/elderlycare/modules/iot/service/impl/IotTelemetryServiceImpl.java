package com.example.elderlycare.modules.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.mapper.HealthRecordMapper;
import com.example.elderlycare.modules.iot.adapter.IotAdapterFactory;
import com.example.elderlycare.modules.iot.adapter.IotDeviceAdapter;
import com.example.elderlycare.modules.iot.adapter.IotNormalizedPayload;
import com.example.elderlycare.modules.iot.dto.DeviceDataQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceReportDTO;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import com.example.elderlycare.modules.iot.entity.DeviceData;
import com.example.elderlycare.modules.iot.entity.DeviceEvent;
import com.example.elderlycare.modules.iot.mapper.DeviceBindingMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceEventMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.iot.service.IotTelemetryService;
import com.example.elderlycare.modules.iot.vo.DeviceDataVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotTelemetryServiceImpl implements IotTelemetryService {

    private final DeviceMapper deviceMapper;
    private final DeviceBindingMapper deviceBindingMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final DeviceEventMapper deviceEventMapper;
    private final ElderMapper elderMapper;
    private final IotAdapterFactory iotAdapterFactory;
    private final HealthMetricMapper healthMetricMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reportTelemetry(DeviceReportDTO dto) {
        // 1. 查询设备档案
        Device device = deviceMapper.selectOne(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getDeviceCode, dto.getDeviceCode().trim())
                        .last("LIMIT 1")
        );
        if (device == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "未登记的设备编码: " + dto.getDeviceCode());
        }

        // 2. 组装或获取原始报文 JSON
        String rawPayload = dto.getRawPayload();
        if (!StringUtils.hasText(rawPayload)) {
            try {
                rawPayload = objectMapper.writeValueAsString(dto);
            } catch (Exception e) {
                rawPayload = "{}";
            }
        }

        // 3. 动态通过适配器工厂解析为平台标准化数据模型
        IotDeviceAdapter adapter = iotAdapterFactory.getAdapter(device.getVendor(), dto.getProtocol());
        IotNormalizedPayload normalized = adapter.normalize(device, rawPayload);

        // 4. 更新设备最后在线时间与运行状态
        LocalDateTime now = LocalDateTime.now();
        device.setLastOnlineAt(now);
        if (StringUtils.hasText(normalized.getDeviceStatus())) {
            device.setStatus(normalized.getDeviceStatus());
        } else {
            device.setStatus("ACTIVE");
        }
        deviceMapper.updateById(device);

        // 5. 查询设备当前的活跃绑定对象 (优先绑定长者)
        DeviceBinding activeBinding = deviceBindingMapper.selectOne(
                new LambdaQueryWrapper<DeviceBinding>()
                        .eq(DeviceBinding::getDeviceId, device.getId())
                        .eq(DeviceBinding::getStatus, "BOUND")
                        .last("LIMIT 1")
        );
        Long boundElderId = activeBinding != null ? activeBinding.getElderId() : null;

        // 6. 持久化存储标准化遥测指标数据
        List<IotNormalizedPayload.NormalizedMetric> metrics = normalized.getMetrics();
        if (!CollectionUtils.isEmpty(metrics)) {
            for (IotNormalizedPayload.NormalizedMetric m : metrics) {
                DeviceData dd = DeviceData.builder()
                        .orgId(device.getOrgId())
                        .deviceId(device.getId())
                        .metricCode(m.getMetricCode())
                        .metricValue(m.getMetricValue())
                        .unit(m.getUnit())
                        .rawPayload(rawPayload)
                        .collectedAt(m.getCollectedAt() != null ? m.getCollectedAt() : now)
                        .receivedAt(now)
                        .build();
                deviceDataMapper.insert(dd);

                // 若设备绑定了长者，且该指标属于健康指标，则自动桥接同步至健康体征档案
                bridgeToHealthRecord(device, boundElderId, m);
            }
        }

        // 7. 处理并写入设备事件 / 告警记录
        List<IotNormalizedPayload.NormalizedEvent> events = normalized.getEvents();
        if (!CollectionUtils.isEmpty(events)) {
            for (IotNormalizedPayload.NormalizedEvent ev : events) {
                DeviceEvent de = DeviceEvent.builder()
                        .orgId(device.getOrgId())
                        .deviceId(device.getId())
                        .eventType(ev.getEventType())
                        .severity(ev.getSeverity() != null ? ev.getSeverity() : "MEDIUM")
                        .eventPayload(ev.getEventPayload() != null ? ev.getEventPayload() : rawPayload)
                        .eventStatus("OPEN")
                        .occurredAt(ev.getOccurredAt() != null ? ev.getOccurredAt() : now)
                        .createdAt(now)
                        .build();
                deviceEventMapper.insert(de);
                log.warn("IoT设备检测到异常事件: deviceCode={}, eventType={}, severity={}",
                        device.getDeviceCode(), de.getEventType(), de.getSeverity());
            }
        }
    }

    @Override
    public PageResult<DeviceDataVO> pageTelemetryData(DeviceDataQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<DeviceData> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(DeviceData::getOrgId, currentOrgId);
        }

        // 若按长者查询，关联长者历史或当前绑定的所有设备
        if (query.getElderId() != null) {
            List<DeviceBinding> bindings = deviceBindingMapper.selectList(
                    new LambdaQueryWrapper<DeviceBinding>()
                            .eq(DeviceBinding::getElderId, query.getElderId())
            );
            if (CollectionUtils.isEmpty(bindings)) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            Set<Long> deviceIds = bindings.stream().map(DeviceBinding::getDeviceId).collect(Collectors.toSet());
            qw.in(DeviceData::getDeviceId, deviceIds);
        } else if (query.getDeviceId() != null) {
            qw.eq(DeviceData::getDeviceId, query.getDeviceId());
        }

        if (StringUtils.hasText(query.getMetricCode())) {
            qw.eq(DeviceData::getMetricCode, query.getMetricCode().trim().toUpperCase());
        }
        if (query.getStartTime() != null) {
            qw.ge(DeviceData::getCollectedAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            qw.le(DeviceData::getCollectedAt, query.getEndTime());
        }

        qw.orderByDesc(DeviceData::getCollectedAt).orderByDesc(DeviceData::getId);

        Page<DeviceData> page = deviceDataMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<DeviceDataVO> voList = convertToDataVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void bridgeToHealthRecord(Device device, Long elderId, IotNormalizedPayload.NormalizedMetric metric) {
        if (elderId == null || metric.getMetricValue() == null) {
            return;
        }
        try {
            HealthMetric hm = healthMetricMapper.selectOne(
                    new LambdaQueryWrapper<HealthMetric>()
                            .eq(HealthMetric::getMetricCode, metric.getMetricCode())
                            .eq(HealthMetric::getStatus, "ACTIVE")
                            .last("LIMIT 1")
            );
            if (hm != null) {
                HealthRecord hr = HealthRecord.builder()
                        .orgId(device.getOrgId())
                        .elderId(elderId)
                        .metricId(hm.getId())
                        .metricValue(metric.getMetricValue())
                        .unitSnapshot(StringUtils.hasText(metric.getUnit()) ? metric.getUnit() : hm.getUnit())
                        .measuredAt(metric.getCollectedAt() != null ? metric.getCollectedAt() : LocalDateTime.now())
                        .sourceType("IOT_DEVICE")
                        .sourceDeviceId(device.getId())
                        .recorderId(1L)
                        .remark(String.format("智能设备[%s]自动遥测采集", device.getDeviceName()))
                        .build();
                healthRecordMapper.insert(hr);
            }
        } catch (Exception e) {
            log.warn("IoT数据桥接写入健康记录跳过: {}", e.getMessage());
        }
    }

    private List<DeviceDataVO> convertToDataVOList(List<DeviceData> records) {
        Set<Long> deviceIds = records.stream().map(DeviceData::getDeviceId).collect(Collectors.toSet());
        Map<Long, Device> deviceMap = deviceIds.isEmpty() ? Collections.emptyMap() :
                deviceMapper.selectBatchIds(deviceIds).stream().collect(Collectors.toMap(Device::getId, d -> d));

        List<DeviceBinding> activeBindings = deviceIds.isEmpty() ? Collections.emptyList() :
                deviceBindingMapper.selectList(
                        new LambdaQueryWrapper<DeviceBinding>()
                                .in(DeviceBinding::getDeviceId, deviceIds)
                                .eq(DeviceBinding::getStatus, "BOUND")
                );
        Map<Long, Long> deviceElderIdMap = activeBindings.stream()
                .filter(b -> b.getElderId() != null)
                .collect(Collectors.toMap(DeviceBinding::getDeviceId, DeviceBinding::getElderId, (b1, b2) -> b1));

        Set<Long> elderIds = new HashSet<>(deviceElderIdMap.values());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        return records.stream().map(r -> {
            Device dev = deviceMap.get(r.getDeviceId());
            Long eId = deviceElderIdMap.get(r.getDeviceId());
            Elder elder = eId != null ? elderMap.get(eId) : null;

            return DeviceDataVO.builder()
                    .id(r.getId())
                    .orgId(r.getOrgId())
                    .deviceId(r.getDeviceId())
                    .deviceCode(dev != null ? dev.getDeviceCode() : "-")
                    .deviceName(dev != null ? dev.getDeviceName() : "-")
                    .deviceType(dev != null ? dev.getDeviceType() : "-")
                    .elderId(eId)
                    .elderName(elder != null ? elder.getName() : "未绑定长者")
                    .metricCode(r.getMetricCode())
                    .metricName(getMetricName(r.getMetricCode()))
                    .metricValue(r.getMetricValue())
                    .unit(r.getUnit())
                    .rawPayload(r.getRawPayload())
                    .collectedAt(r.getCollectedAt())
                    .receivedAt(r.getReceivedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String getMetricName(String code) {
        if (code == null) return "-";
        return switch (code.toUpperCase()) {
            case "HEART_RATE" -> "心率 (HR)";
            case "SPO2" -> "血氧饱和度 (SpO2)";
            case "RESPIRATORY_RATE" -> "呼吸频率 (RR)";
            case "SBP" -> "收缩压 (高压)";
            case "DBP" -> "舒张压 (低压)";
            case "BLOOD_SUGAR" -> "血糖水平";
            case "BODY_TEMP" -> "体温";
            case "STEP_COUNT" -> "步行步数";
            case "IN_BED_STATUS" -> "在床/在垫状态";
            case "BATTERY" -> "设备电池电量";
            default -> code;
        };
    }
}
