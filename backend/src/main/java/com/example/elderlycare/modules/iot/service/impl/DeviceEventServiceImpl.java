package com.example.elderlycare.modules.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.iot.dto.DeviceEventHandleDTO;
import com.example.elderlycare.modules.iot.dto.DeviceEventQueryDTO;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import com.example.elderlycare.modules.iot.entity.DeviceEvent;
import com.example.elderlycare.modules.iot.mapper.DeviceBindingMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceEventMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.iot.service.DeviceEventService;
import com.example.elderlycare.modules.iot.vo.DeviceEventVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
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
public class DeviceEventServiceImpl implements DeviceEventService {

    private final DeviceEventMapper deviceEventMapper;
    private final DeviceMapper deviceMapper;
    private final DeviceBindingMapper deviceBindingMapper;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public PageResult<DeviceEventVO> pageEvents(DeviceEventQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<DeviceEvent> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(DeviceEvent::getOrgId, currentOrgId);
        }

        // 若指定了长者过滤，关联长者名下的设备
        if (query.getElderId() != null) {
            List<DeviceBinding> bindings = deviceBindingMapper.selectList(
                    new LambdaQueryWrapper<DeviceBinding>()
                            .eq(DeviceBinding::getElderId, query.getElderId())
            );
            if (CollectionUtils.isEmpty(bindings)) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            Set<Long> deviceIds = bindings.stream().map(DeviceBinding::getDeviceId).collect(Collectors.toSet());
            qw.in(DeviceEvent::getDeviceId, deviceIds);
        } else if (query.getDeviceId() != null) {
            qw.eq(DeviceEvent::getDeviceId, query.getDeviceId());
        }

        if (StringUtils.hasText(query.getEventType())) {
            qw.eq(DeviceEvent::getEventType, query.getEventType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getSeverity())) {
            qw.eq(DeviceEvent::getSeverity, query.getSeverity().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getEventStatus())) {
            qw.eq(DeviceEvent::getEventStatus, query.getEventStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(DeviceEvent::getHandleResult, kw)
                    .or().like(DeviceEvent::getEventPayload, kw));
        }

        qw.orderByDesc(DeviceEvent::getOccurredAt).orderByDesc(DeviceEvent::getId);

        Page<DeviceEvent> page = deviceEventMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<DeviceEventVO> voList = convertToEventVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DeviceEventVO getEventDetail(Long id) {
        DeviceEvent event = deviceEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备事件不存在");
        }
        checkEventAccess(event);

        List<DeviceEventVO> voList = convertToEventVOList(Collections.singletonList(event));
        return voList.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleEvent(Long id, DeviceEventHandleDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        DeviceEvent event = deviceEventMapper.selectByIdForUpdate(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备事件不存在");
        }
        checkEventAccess(event);

        String targetStatus = dto.getEventStatus().trim().toUpperCase();
        if ("CLOSED".equalsIgnoreCase(event.getEventStatus()) && "CLOSED".equalsIgnoreCase(targetStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该事件已处置结案，无需重复操作");
        }

        event.setEventStatus(targetStatus);
        event.setHandleResult(dto.getHandleResult().trim());
        event.setHandledBy(currentUserId);
        event.setHandledAt(LocalDateTime.now());

        deviceEventMapper.updateById(event);

        Device device = deviceMapper.selectById(event.getDeviceId());

        sysOperationLogService.recordLog(
                "IOT_EVENT",
                "HANDLE",
                "DEVICE_EVENT",
                event.getId(),
                "/api/v1/iot/events/" + id + "/handle",
                "127.0.0.1",
                String.format("处置设备事件: 设备=[%s], 类型=%s, 处置结论=%s, 变更状态=%s",
                        device != null ? device.getDeviceCode() : "-", event.getEventType(),
                        event.getHandleResult(), targetStatus)
        );
    }

    private void checkEventAccess(DeviceEvent event) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(event.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构设备告警事件");
        }
    }

    private List<DeviceEventVO> convertToEventVOList(List<DeviceEvent> events) {
        Set<Long> deviceIds = events.stream().map(DeviceEvent::getDeviceId).collect(Collectors.toSet());
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

        Set<Long> userIds = events.stream().map(DeviceEvent::getHandledBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return events.stream().map(e -> {
            Device dev = deviceMap.get(e.getDeviceId());
            Long eId = deviceElderIdMap.get(e.getDeviceId());
            Elder elder = eId != null ? elderMap.get(eId) : null;
            SysUser handler = e.getHandledBy() != null ? userMap.get(e.getHandledBy()) : null;

            return DeviceEventVO.builder()
                    .id(e.getId())
                    .orgId(e.getOrgId())
                    .deviceId(e.getDeviceId())
                    .deviceCode(dev != null ? dev.getDeviceCode() : "-")
                    .deviceName(dev != null ? dev.getDeviceName() : "-")
                    .deviceType(dev != null ? dev.getDeviceType() : "-")
                    .installLocation(dev != null ? dev.getInstallLocation() : "-")
                    .elderId(eId)
                    .elderName(elder != null ? elder.getName() : "未绑定长者")
                    .eventType(e.getEventType())
                    .eventTypeName(getEventTypeName(e.getEventType()))
                    .severity(e.getSeverity())
                    .severityName(getSeverityName(e.getSeverity()))
                    .eventPayload(e.getEventPayload())
                    .eventStatus(e.getEventStatus())
                    .eventStatusName(getEventStatusName(e.getEventStatus()))
                    .occurredAt(e.getOccurredAt())
                    .handledBy(e.getHandledBy())
                    .handledByName(handler != null ? handler.getRealName() : "-")
                    .handledAt(e.getHandledAt())
                    .handleResult(e.getHandleResult())
                    .createdAt(e.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String getEventTypeName(String type) {
        if (type == null) return "-";
        return switch (type.toUpperCase()) {
            case "SOS" -> "SOS 紧急求救呼叫";
            case "FALL_DETECTED" -> "跌倒监测告警";
            case "LOW_BATTERY" -> "设备低电量预警";
            case "OFFLINE" -> "设备通信离线";
            case "ABNORMAL_METRIC" -> "健康指标异常偏离";
            case "LEAVE_BED_OVERTIME" -> "夜间离床超时告警";
            default -> type;
        };
    }

    private String getSeverityName(String severity) {
        if (severity == null) return "-";
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "特急重大";
            case "HIGH" -> "高风险";
            case "MEDIUM" -> "中度注意";
            case "LOW" -> "低度提醒";
            default -> severity;
        };
    }

    private String getEventStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "OPEN" -> "待处置";
            case "PROCESSING" -> "处置中";
            case "CLOSED" -> "已结案";
            case "IGNORED" -> "已忽略";
            default -> status;
        };
    }
}
