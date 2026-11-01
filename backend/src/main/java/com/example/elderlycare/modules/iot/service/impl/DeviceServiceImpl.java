package com.example.elderlycare.modules.iot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.iot.dto.DeviceBindDTO;
import com.example.elderlycare.modules.iot.dto.DeviceDTO;
import com.example.elderlycare.modules.iot.dto.DeviceQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceUnbindDTO;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import com.example.elderlycare.modules.iot.entity.DeviceData;
import com.example.elderlycare.modules.iot.entity.DeviceEvent;
import com.example.elderlycare.modules.iot.mapper.DeviceBindingMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceEventMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.iot.service.DeviceService;
import com.example.elderlycare.modules.iot.vo.DeviceBindingVO;
import com.example.elderlycare.modules.iot.vo.DeviceVO;
import com.example.elderlycare.modules.iot.vo.IotSummaryVO;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final DeviceBindingMapper deviceBindingMapper;
    private final DeviceDataMapper deviceDataMapper;
    private final DeviceEventMapper deviceEventMapper;
    private final ElderMapper elderMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDevice(DeviceDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long orgId = currentOrgId != null ? currentOrgId : 1L;

        String code = dto.getDeviceCode().trim();
        Long count = deviceMapper.selectCount(
                new LambdaQueryWrapper<Device>()
                        .eq(Device::getOrgId, orgId)
                        .eq(Device::getDeviceCode, code)
        );
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "机构下已存在相同设备编码: " + code);
        }

        String initialStatus = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "ACTIVE";

        Device device = Device.builder()
                .orgId(orgId)
                .deviceCode(code)
                .deviceName(dto.getDeviceName().trim())
                .deviceType(dto.getDeviceType().trim().toUpperCase())
                .vendor(StringUtils.hasText(dto.getVendor()) ? dto.getVendor().trim() : null)
                .model(StringUtils.hasText(dto.getModel()) ? dto.getModel().trim() : null)
                .serialNo(StringUtils.hasText(dto.getSerialNo()) ? dto.getSerialNo().trim() : null)
                .installLocation(StringUtils.hasText(dto.getInstallLocation()) ? dto.getInstallLocation().trim() : null)
                .status(initialStatus)
                .lastOnlineAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        deviceMapper.insert(device);

        sysOperationLogService.recordLog(
                "IOT_DEVICE",
                "CREATE",
                "DEVICE",
                device.getId(),
                "/api/v1/iot/devices",
                "127.0.0.1",
                String.format("登记新增IoT设备: 编码=%s, 名称=%s, 类型=%s",
                        device.getDeviceCode(), device.getDeviceName(), device.getDeviceType())
        );

        return device.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Long id, DeviceDTO dto) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备记录不存在");
        }
        checkDeviceAccess(device);

        String code = dto.getDeviceCode().trim();
        if (!code.equalsIgnoreCase(device.getDeviceCode())) {
            Long count = deviceMapper.selectCount(
                    new LambdaQueryWrapper<Device>()
                            .eq(Device::getOrgId, device.getOrgId())
                            .eq(Device::getDeviceCode, code)
                            .ne(Device::getId, id)
            );
            if (count != null && count > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "机构下已存在相同设备编码: " + code);
            }
            device.setDeviceCode(code);
        }

        device.setDeviceName(dto.getDeviceName().trim());
        device.setDeviceType(dto.getDeviceType().trim().toUpperCase());
        device.setVendor(dto.getVendor());
        device.setModel(dto.getModel());
        device.setSerialNo(dto.getSerialNo());
        device.setInstallLocation(dto.getInstallLocation());
        if (StringUtils.hasText(dto.getStatus())) {
            device.setStatus(dto.getStatus().trim().toUpperCase());
        }

        deviceMapper.updateById(device);

        sysOperationLogService.recordLog(
                "IOT_DEVICE",
                "UPDATE",
                "DEVICE",
                device.getId(),
                "/api/v1/iot/devices/" + id,
                "127.0.0.1",
                String.format("更新IoT设备资料: 编码=%s, 名称=%s", device.getDeviceCode(), device.getDeviceName())
        );
    }

    @Override
    public DeviceVO getDeviceDetail(Long id) {
        Device device = deviceMapper.selectById(id);
        if (device == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "设备记录不存在");
        }
        checkDeviceAccess(device);

        List<DeviceVO> list = convertToDeviceVOList(Collections.singletonList(device));
        return list.get(0);
    }

    @Override
    public PageResult<DeviceVO> pageDevices(DeviceQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(Device::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(query.getDeviceType())) {
            qw.eq(Device::getDeviceType, query.getDeviceType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(Device::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(Device::getDeviceCode, kw)
                    .or().like(Device::getDeviceName, kw)
                    .or().like(Device::getSerialNo, kw)
                    .or().like(Device::getInstallLocation, kw));
        }

        qw.orderByDesc(Device::getId);

        Page<Device> page = deviceMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<DeviceVO> voList = convertToDeviceVOList(page.getRecords());

        // 若前端指定了绑定状态过滤: BOUND (已绑定) 或 UNBOUND (未绑定)
        if (StringUtils.hasText(query.getBindStatus())) {
            String target = query.getBindStatus().trim().toUpperCase();
            voList = voList.stream().filter(v -> target.equals(v.getBindStatus())).collect(Collectors.toList());
        }

        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<DeviceVO> listActiveDevices(String deviceType) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<Device> qw = new LambdaQueryWrapper<Device>()
                .eq(Device::getStatus, "ACTIVE");
        if (currentOrgId != null) {
            qw.eq(Device::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(deviceType)) {
            qw.eq(Device::getDeviceType, deviceType.trim().toUpperCase());
        }
        qw.orderByAsc(Device::getId);
        List<Device> list = deviceMapper.selectList(qw);
        return convertToDeviceVOList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindDevice(DeviceBindDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 1. 排他行级锁锁定目标设备
        Device device = deviceMapper.selectByIdForUpdate(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目标设备不存在");
        }
        checkDeviceAccess(device);

        // 2. 检查设备当前是否已被绑定
        DeviceBinding activeBinding = deviceBindingMapper.selectActiveByDeviceIdForUpdate(device.getId());
        if (activeBinding != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该设备当前已处于绑定占用状态，请先解绑后再重新绑定");
        }

        // 3. 校验长者与床位有效性
        if (dto.getElderId() != null) {
            Elder elder = elderMapper.selectById(dto.getElderId());
            if (elder == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的长者不存在");
            }
            if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权绑定其他机构的长者");
            }
        }

        if (dto.getBedId() != null) {
            Bed bed = bedMapper.selectById(dto.getBedId());
            if (bed == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的床位不存在");
            }
        }

        // 4. 插入新的绑定流水记录 (状态为 BOUND)
        DeviceBinding newBinding = DeviceBinding.builder()
                .orgId(device.getOrgId())
                .deviceId(device.getId())
                .elderId(dto.getElderId())
                .bedId(dto.getBedId())
                .bindType(dto.getBindType().trim().toUpperCase())
                .boundAt(LocalDateTime.now())
                .status("BOUND")
                .build();

        deviceBindingMapper.insert(newBinding);

        sysOperationLogService.recordLog(
                "IOT_DEVICE",
                "BIND",
                "DEVICE_BINDING",
                newBinding.getId(),
                "/api/v1/iot/devices/bind",
                "127.0.0.1",
                String.format("完成设备绑定: 设备=[%s]%s, 绑定类型=%s, 长者ID=%s, 床位ID=%s",
                        device.getDeviceCode(), device.getDeviceName(), newBinding.getBindType(),
                        newBinding.getElderId(), newBinding.getBedId())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindDevice(DeviceUnbindDTO dto) {
        Device device = deviceMapper.selectByIdForUpdate(dto.getDeviceId());
        if (device == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目标设备不存在");
        }
        checkDeviceAccess(device);

        DeviceBinding activeBinding = deviceBindingMapper.selectActiveByDeviceIdForUpdate(device.getId());
        if (activeBinding == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该设备当前处于空闲状态，未绑定任何对象");
        }

        // 更新解绑时间与状态（历史记录不可物理删除，支持完整审计追溯）
        activeBinding.setStatus("UNBOUND");
        activeBinding.setUnboundAt(LocalDateTime.now());
        deviceBindingMapper.updateById(activeBinding);

        sysOperationLogService.recordLog(
                "IOT_DEVICE",
                "UNBIND",
                "DEVICE_BINDING",
                activeBinding.getId(),
                "/api/v1/iot/devices/unbind",
                "127.0.0.1",
                String.format("解除设备绑定: 设备=[%s]%s, 原长者ID=%s, 原因=%s",
                        device.getDeviceCode(), device.getDeviceName(), activeBinding.getElderId(), dto.getRemark())
        );
    }

    @Override
    public PageResult<DeviceBindingVO> pageBindings(Long deviceId, Long elderId, int pageNum, int pageSize) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<DeviceBinding> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(DeviceBinding::getOrgId, currentOrgId);
        }
        if (deviceId != null) {
            qw.eq(DeviceBinding::getDeviceId, deviceId);
        }
        if (elderId != null) {
            qw.eq(DeviceBinding::getElderId, elderId);
        }

        qw.orderByDesc(DeviceBinding::getBoundAt).orderByDesc(DeviceBinding::getId);

        Page<DeviceBinding> page = deviceBindingMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<DeviceBindingVO> voList = convertToBindingVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public IotSummaryVO getIotSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Device> dqw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            dqw.eq(Device::getOrgId, currentOrgId);
        }
        List<Device> allDevices = deviceMapper.selectList(dqw);

        long total = allDevices.size();
        long active = allDevices.stream().filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus())).count();
        long offline = allDevices.stream().filter(d -> "OFFLINE".equalsIgnoreCase(d.getStatus())).count();
        long fault = allDevices.stream().filter(d -> "FAULT".equalsIgnoreCase(d.getStatus())).count();

        // 统计当前活跃绑定设备数
        LambdaQueryWrapper<DeviceBinding> bqw = new LambdaQueryWrapper<DeviceBinding>()
                .eq(DeviceBinding::getStatus, "BOUND");
        if (currentOrgId != null) {
            bqw.eq(DeviceBinding::getOrgId, currentOrgId);
        }
        List<DeviceBinding> activeBindings = deviceBindingMapper.selectList(bqw);
        Set<Long> boundDeviceIds = activeBindings.stream().map(DeviceBinding::getDeviceId).collect(Collectors.toSet());
        long boundCount = allDevices.stream().filter(d -> boundDeviceIds.contains(d.getId())).count();
        long unboundCount = total - boundCount;

        // 待处理事件数
        LambdaQueryWrapper<DeviceEvent> eqw = new LambdaQueryWrapper<DeviceEvent>()
                .eq(DeviceEvent::getEventStatus, "OPEN");
        if (currentOrgId != null) {
            eqw.eq(DeviceEvent::getOrgId, currentOrgId);
        }
        Long openEvents = deviceEventMapper.selectCount(eqw);

        // 今日接收数据条数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LambdaQueryWrapper<DeviceData> dataQw = new LambdaQueryWrapper<DeviceData>()
                .ge(DeviceData::getReceivedAt, todayStart);
        if (currentOrgId != null) {
            dataQw.eq(DeviceData::getOrgId, currentOrgId);
        }
        Long todayTelemetry = deviceDataMapper.selectCount(dataQw);

        return IotSummaryVO.builder()
                .totalDevices(total)
                .activeDevices(active)
                .offlineDevices(offline)
                .faultDevices(fault)
                .boundDevices(boundCount)
                .unboundDevices(unboundCount)
                .openEventCount(openEvents != null ? openEvents : 0L)
                .todayTelemetryCount(todayTelemetry != null ? todayTelemetry : 0L)
                .build();
    }

    private void checkDeviceAccess(Device device) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(device.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构设备资料");
        }
    }

    private List<DeviceVO> convertToDeviceVOList(List<Device> devices) {
        if (CollectionUtils.isEmpty(devices)) {
            return Collections.emptyList();
        }

        Set<Long> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toSet());

        // 获取每个设备当前的活跃绑定状态 (status = 'BOUND')
        List<DeviceBinding> activeBindings = deviceBindingMapper.selectList(
                new LambdaQueryWrapper<DeviceBinding>()
                        .in(DeviceBinding::getDeviceId, deviceIds)
                        .eq(DeviceBinding::getStatus, "BOUND")
        );
        Map<Long, DeviceBinding> bindingMap = activeBindings.stream()
                .collect(Collectors.toMap(DeviceBinding::getDeviceId, b -> b, (b1, b2) -> b1));

        Set<Long> elderIds = activeBindings.stream().map(DeviceBinding::getElderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Set<Long> bedIds = activeBindings.stream().map(DeviceBinding::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> bedInfoMap = buildBedInfoMap(bedIds);

        return devices.stream().map(d -> {
            DeviceBinding binding = bindingMap.get(d.getId());
            Elder elder = binding != null && binding.getElderId() != null ? elderMap.get(binding.getElderId()) : null;
            String bedInfo = binding != null && binding.getBedId() != null ? bedInfoMap.get(binding.getBedId()) : "-";

            String bindStatus = binding != null ? "BOUND" : "UNBOUND";

            return DeviceVO.builder()
                    .id(d.getId())
                    .orgId(d.getOrgId())
                    .deviceCode(d.getDeviceCode())
                    .deviceName(d.getDeviceName())
                    .deviceType(d.getDeviceType())
                    .deviceTypeName(getDeviceTypeName(d.getDeviceType()))
                    .vendor(d.getVendor())
                    .model(d.getModel())
                    .serialNo(d.getSerialNo())
                    .installLocation(d.getInstallLocation())
                    .status(d.getStatus())
                    .statusName(getDeviceStatusName(d.getStatus()))
                    .lastOnlineAt(d.getLastOnlineAt())
                    .createdAt(d.getCreatedAt())
                    .bindStatus(bindStatus)
                    .bindType(binding != null ? binding.getBindType() : null)
                    .boundElderId(elder != null ? elder.getId() : null)
                    .boundElderName(elder != null ? elder.getName() : null)
                    .boundBedId(binding != null ? binding.getBedId() : null)
                    .boundBedInfo(bedInfo)
                    .boundAt(binding != null ? binding.getBoundAt() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<DeviceBindingVO> convertToBindingVOList(List<DeviceBinding> bindings) {
        if (CollectionUtils.isEmpty(bindings)) {
            return Collections.emptyList();
        }

        Set<Long> deviceIds = bindings.stream().map(DeviceBinding::getDeviceId).collect(Collectors.toSet());
        Map<Long, Device> deviceMap = deviceIds.isEmpty() ? Collections.emptyMap() :
                deviceMapper.selectBatchIds(deviceIds).stream().collect(Collectors.toMap(Device::getId, d -> d));

        Set<Long> elderIds = bindings.stream().map(DeviceBinding::getElderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Set<Long> bedIds = bindings.stream().map(DeviceBinding::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> bedInfoMap = buildBedInfoMap(bedIds);

        return bindings.stream().map(b -> {
            Device device = deviceMap.get(b.getDeviceId());
            Elder elder = b.getElderId() != null ? elderMap.get(b.getElderId()) : null;
            String bedInfo = b.getBedId() != null ? bedInfoMap.get(b.getBedId()) : "-";

            return DeviceBindingVO.builder()
                    .id(b.getId())
                    .orgId(b.getOrgId())
                    .deviceId(b.getDeviceId())
                    .deviceCode(device != null ? device.getDeviceCode() : "-")
                    .deviceName(device != null ? device.getDeviceName() : "-")
                    .elderId(b.getElderId())
                    .elderName(elder != null ? elder.getName() : "-")
                    .elderNo(elder != null ? elder.getElderNo() : "-")
                    .bedId(b.getBedId())
                    .bedInfo(bedInfo)
                    .bindType(b.getBindType())
                    .bindTypeName(getBindTypeName(b.getBindType()))
                    .boundAt(b.getBoundAt())
                    .unboundAt(b.getUnboundAt())
                    .status(b.getStatus())
                    .statusName("BOUND".equalsIgnoreCase(b.getStatus()) ? "使用中" : "已解绑")
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, String> buildBedInfoMap(Set<Long> bedIds) {
        if (CollectionUtils.isEmpty(bedIds)) {
            return Collections.emptyMap();
        }
        Map<Long, Bed> bedMap = bedMapper.selectBatchIds(bedIds).stream().collect(Collectors.toMap(Bed::getId, b -> b));
        Set<Long> roomIds = bedMap.values().stream().map(Bed::getRoomId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Room> roomMap = roomIds.isEmpty() ? Collections.emptyMap() :
                roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, r -> r));
        Set<Long> buildingIds = roomMap.values().stream().map(Room::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Collections.emptyMap() :
                buildingMapper.selectBatchIds(buildingIds).stream().collect(Collectors.toMap(Building::getId, b -> b));

        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<Long, Bed> entry : bedMap.entrySet()) {
            Bed bed = entry.getValue();
            Room room = roomMap.get(bed.getRoomId());
            Building building = room != null ? buildingMap.get(room.getBuildingId()) : null;
            String bName = building != null ? building.getBuildingName() : "";
            String rName = room != null ? room.getRoomNumber() + "室" : "";
            String bedStr = bed.getBedNumber() + "床";
            result.put(entry.getKey(), String.join(" ", bName, rName, bedStr).trim());
        }
        return result;
    }

    private String getDeviceTypeName(String type) {
        if (type == null) return "-";
        return switch (type.toUpperCase()) {
            case "SMART_WATCH" -> "智能手环/手表";
            case "SMART_MATTRESS" -> "睡眠体征床垫";
            case "BLOOD_PRESSURE" -> "智能血压计";
            case "BLOOD_GLUCOSE" -> "智能血糖仪";
            case "SOS_BUTTON" -> "一键呼叫器";
            case "RADAR_FALL" -> "毫米波跌倒雷达";
            case "THERMOMETER" -> "红外体温计";
            default -> type;
        };
    }

    private String getDeviceStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "ACTIVE" -> "在线运行";
            case "INACTIVE" -> "已停用";
            case "OFFLINE" -> "设备离线";
            case "FAULT" -> "故障告警";
            default -> status;
        };
    }

    private String getBindTypeName(String bindType) {
        if (bindType == null) return "-";
        return switch (bindType.toUpperCase()) {
            case "ELDER" -> "长者随身佩戴";
            case "BED" -> "床位固定安装";
            case "SPACE" -> "公共空间部署";
            default -> bindType;
        };
    }
}
