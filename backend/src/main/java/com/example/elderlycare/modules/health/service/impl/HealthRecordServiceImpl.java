package com.example.elderlycare.modules.health.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.health.dto.HealthRecordBatchCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordCreateDTO;
import com.example.elderlycare.modules.health.dto.HealthRecordQueryDTO;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.mapper.HealthRecordMapper;
import com.example.elderlycare.modules.health.service.HealthAlertService;
import com.example.elderlycare.modules.health.service.HealthRecordService;
import com.example.elderlycare.modules.health.vo.HealthRecordVO;
import com.example.elderlycare.modules.health.vo.HealthTrendPointVO;
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
public class HealthRecordServiceImpl implements HealthRecordService {

    private final HealthRecordMapper healthRecordMapper;
    private final HealthMetricMapper healthMetricMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final HealthAlertService healthAlertService;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public PageResult<HealthRecordVO> pageRecords(HealthRecordQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, HealthRecord::getOrgId, currentOrgId);

        if (query.getElderId() != null) {
            wrapper.eq(HealthRecord::getElderId, query.getElderId());
        } else if (StringUtils.hasText(query.getElderName())) {
            LambdaQueryWrapper<Elder> elderWrapper = new LambdaQueryWrapper<>();
            elderWrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(Elder::getName, query.getElderName().trim());
            List<Elder> matchedElders = elderMapper.selectList(elderWrapper);
            if (matchedElders.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(HealthRecord::getElderId, elderIds);
        }

        if (query.getMetricId() != null) {
            wrapper.eq(HealthRecord::getMetricId, query.getMetricId());
        }
        if (StringUtils.hasText(query.getSourceType())) {
            wrapper.eq(HealthRecord::getSourceType, query.getSourceType());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(HealthRecord::getMeasuredAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(HealthRecord::getMeasuredAt, query.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(HealthRecord::getMeasuredAt);

        Page<HealthRecord> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<HealthRecord> pageResult = healthRecordMapper.selectPage(pageParam, wrapper);

        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return PageResult.of(Collections.emptyList(), pageResult.getTotal(), query.getPageNum(), query.getPageSize());
        }

        List<HealthRecordVO> vos = enrichRecords(pageResult.getRecords());
        return PageResult.of(vos, pageResult.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<HealthTrendPointVO> listTrend(Long elderId, Long metricId, Integer days) {
        if (elderId == null || metricId == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "长者ID和指标ID不能为空");
        }
        checkElderOrgAccess(elderId);

        int queryDays = (days != null && days > 0) ? days : 7;
        LocalDateTime startTime = LocalDateTime.now().minusDays(queryDays);

        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthRecord::getElderId, elderId)
                .eq(HealthRecord::getMetricId, metricId)
                .ge(HealthRecord::getMeasuredAt, startTime)
                .orderByAsc(HealthRecord::getMeasuredAt);

        List<HealthRecord> records = healthRecordMapper.selectList(wrapper);
        return records.stream().map(r -> HealthTrendPointVO.builder()
                .id(r.getId())
                .measuredAt(r.getMeasuredAt())
                .metricValue(r.getMetricValue())
                .unitSnapshot(r.getUnitSnapshot())
                .sourceType(r.getSourceType())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRecord(HealthRecordCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        Elder elder = checkElderOrgAccess(dto.getElderId());
        HealthMetric metric = checkMetricOrgAccess(dto.getMetricId());

        if (dto.getMeasuredAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "测量时间不能晚于当前时间");
        }

        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();

        HealthRecord record = HealthRecord.builder()
                .orgId(orgId)
                .elderId(dto.getElderId())
                .metricId(dto.getMetricId())
                .metricValue(dto.getMetricValue())
                .unitSnapshot(metric.getUnit())
                .measuredAt(dto.getMeasuredAt())
                .sourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "MANUAL")
                .sourceDeviceId(dto.getSourceDeviceId())
                .recorderId(currentUserId)
                .remark(dto.getRemark())
                .createdAt(LocalDateTime.now())
                .build();

        healthRecordMapper.insert(record);

        // 触发规则匹配与去重告警评估
        healthAlertService.evaluateAndTriggerAlert(record);

        // 记录系统审计日志
        sysOperationLogService.recordLog(
                "HEALTH_RECORD",
                "CREATE",
                "HEALTH_RECORD",
                record.getId(),
                "/api/v1/health/records",
                "127.0.0.1",
                String.format("录入健康体征记录: 长者=%s, 指标=%s, 数值=%s %s",
                        elder.getName(), metric.getMetricName(), dto.getMetricValue(), metric.getUnit() != null ? metric.getUnit() : "")
        );

        return record.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateRecords(HealthRecordBatchCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        Elder elder = checkElderOrgAccess(dto.getElderId());

        if (dto.getMeasuredAt().isAfter(LocalDateTime.now().plusMinutes(10))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "测量时间不能晚于当前时间");
        }

        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();
        List<Long> resultIds = new ArrayList<>();

        for (HealthRecordBatchCreateDTO.Item item : dto.getItems()) {
            HealthMetric metric = checkMetricOrgAccess(item.getMetricId());

            HealthRecord record = HealthRecord.builder()
                    .orgId(orgId)
                    .elderId(dto.getElderId())
                    .metricId(item.getMetricId())
                    .metricValue(item.getMetricValue())
                    .unitSnapshot(metric.getUnit())
                    .measuredAt(dto.getMeasuredAt())
                    .sourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "MANUAL")
                    .sourceDeviceId(dto.getSourceDeviceId())
                    .recorderId(currentUserId)
                    .remark(dto.getRemark())
                    .createdAt(LocalDateTime.now())
                    .build();

            healthRecordMapper.insert(record);
            healthAlertService.evaluateAndTriggerAlert(record);
            resultIds.add(record.getId());
        }

        sysOperationLogService.recordLog(
                "HEALTH_RECORD",
                "BATCH_CREATE",
                "HEALTH_RECORD",
                dto.getElderId(),
                "/api/v1/health/records/batch",
                "127.0.0.1",
                String.format("批量录入健康体征: 长者=%s, 包含 %d 项指标", elder.getName(), dto.getItems().size())
        );

        return resultIds;
    }

    private Elder checkElderOrgAccess(Long elderId) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的长者不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的长者健康数据");
        }
        return elder;
    }

    private HealthMetric checkMetricOrgAccess(Long metricId) {
        HealthMetric metric = healthMetricMapper.selectById(metricId);
        if (metric == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康指标不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(metric.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权使用其他机构的健康指标");
        }
        if (!"ACTIVE".equalsIgnoreCase(metric.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "指标 [" + metric.getMetricName() + "] 已停用");
        }
        return metric;
    }

    private List<HealthRecordVO> enrichRecords(List<HealthRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = records.stream().map(HealthRecord::getElderId).collect(Collectors.toSet());
        Set<Long> metricIds = records.stream().map(HealthRecord::getMetricId).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(HealthRecord::getRecorderId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, HealthMetric> metricMap = metricIds.isEmpty() ? Collections.emptyMap() :
                healthMetricMapper.selectBatchIds(metricIds).stream().collect(Collectors.toMap(HealthMetric::getId, m -> m));

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Map<Long, String> bedInfoMap = buildElderBedInfoMap(elderIds);

        return records.stream().map(r -> {
            Elder elder = elderMap.get(r.getElderId());
            HealthMetric metric = metricMap.get(r.getMetricId());
            SysUser recorder = r.getRecorderId() != null ? userMap.get(r.getRecorderId()) : null;

            return HealthRecordVO.builder()
                    .id(r.getId())
                    .orgId(r.getOrgId())
                    .elderId(r.getElderId())
                    .elderName(elder != null ? elder.getName() : null)
                    .elderNo(elder != null ? elder.getElderNo() : null)
                    .bedInfo(elder != null ? bedInfoMap.get(elder.getId()) : null)
                    .metricId(r.getMetricId())
                    .metricCode(metric != null ? metric.getMetricCode() : null)
                    .metricName(metric != null ? metric.getMetricName() : null)
                    .metricValue(r.getMetricValue())
                    .unitSnapshot(r.getUnitSnapshot())
                    .measuredAt(r.getMeasuredAt())
                    .sourceType(r.getSourceType())
                    .recorderId(r.getRecorderId())
                    .recorderName(recorder != null ? recorder.getRealName() : "系统自动")
                    .remark(r.getRemark())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, String> buildElderBedInfoMap(Set<Long> elderIds) {
        if (CollectionUtils.isEmpty(elderIds)) {
            return Collections.emptyMap();
        }
        List<Admission> activeAdmissions = admissionMapper.selectList(
                new LambdaQueryWrapper<Admission>()
                        .in(Admission::getElderId, elderIds)
                        .eq(Admission::getStatus, "CHECKED_IN")
        );
        if (activeAdmissions.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> bedIds = activeAdmissions.stream().map(Admission::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (bedIds.isEmpty()) {
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
        for (Admission admission : activeAdmissions) {
            Bed bed = bedMap.get(admission.getBedId());
            if (bed != null) {
                Room room = roomMap.get(bed.getRoomId());
                Building building = room != null ? buildingMap.get(room.getBuildingId()) : null;
                String desc = (building != null ? building.getBuildingName() + " " : "")
                        + (room != null ? room.getRoomNumber() + "室 " : "")
                        + bed.getBedNo() + "床";
                result.put(admission.getElderId(), desc);
            }
        }
        return result;
    }
}
