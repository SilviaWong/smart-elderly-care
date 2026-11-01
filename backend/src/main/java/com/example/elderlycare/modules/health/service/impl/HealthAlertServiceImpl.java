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
import com.example.elderlycare.modules.health.dto.HealthAlertAssignDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertHandleDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertQueryDTO;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import com.example.elderlycare.modules.health.entity.HealthAlertRule;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.health.mapper.HealthAlertRuleMapper;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.service.HealthAlertService;
import com.example.elderlycare.modules.health.vo.HealthAlertSummaryVO;
import com.example.elderlycare.modules.health.vo.HealthAlertVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthAlertServiceImpl implements HealthAlertService {

    private final HealthAlertMapper healthAlertMapper;
    private final HealthAlertRuleMapper healthAlertRuleMapper;
    private final HealthMetricMapper healthMetricMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public PageResult<HealthAlertVO> pageAlerts(HealthAlertQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, HealthAlert::getOrgId, currentOrgId);

        if (StringUtils.hasText(query.getAlertNo())) {
            wrapper.like(HealthAlert::getAlertNo, query.getAlertNo().trim());
        }
        if (query.getElderId() != null) {
            wrapper.eq(HealthAlert::getElderId, query.getElderId());
        } else if (StringUtils.hasText(query.getElderName())) {
            LambdaQueryWrapper<Elder> elderWrapper = new LambdaQueryWrapper<>();
            elderWrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(Elder::getName, query.getElderName().trim());
            List<Elder> matchedElders = elderMapper.selectList(elderWrapper);
            if (matchedElders.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(HealthAlert::getElderId, elderIds);
        }

        if (query.getRuleId() != null) {
            wrapper.eq(HealthAlert::getRuleId, query.getRuleId());
        }
        if (StringUtils.hasText(query.getSeverity())) {
            wrapper.eq(HealthAlert::getSeverity, query.getSeverity());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(HealthAlert::getStatus, query.getStatus());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(HealthAlert::getAssigneeId, query.getAssigneeId());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(HealthAlert::getCreatedAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(HealthAlert::getCreatedAt, query.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(HealthAlert::getId);

        Page<HealthAlert> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<HealthAlert> pageResult = healthAlertMapper.selectPage(pageParam, wrapper);

        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return PageResult.of(Collections.emptyList(), pageResult.getTotal(), query.getPageNum(), query.getPageSize());
        }

        List<HealthAlertVO> vos = enrichAlerts(pageResult.getRecords());
        return PageResult.of(vos, pageResult.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public HealthAlertSummaryVO getAlertSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<HealthAlert> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, HealthAlert::getOrgId, currentOrgId);
        List<HealthAlert> alerts = healthAlertMapper.selectList(wrapper);

        int total = alerts.size();
        int open = 0;
        int processing = 0;
        int resolved = 0;
        int ignored = 0;
        int critical = 0;

        for (HealthAlert a : alerts) {
            if ("OPEN".equals(a.getStatus())) {
                open++;
            } else if ("PROCESSING".equals(a.getStatus())) {
                processing++;
            } else if ("RESOLVED".equals(a.getStatus())) {
                resolved++;
            } else if ("IGNORED".equals(a.getStatus())) {
                ignored++;
            }
            if ("CRITICAL".equalsIgnoreCase(a.getSeverity())) {
                critical++;
            }
        }

        return HealthAlertSummaryVO.builder()
                .totalAlerts(total)
                .openCount(open)
                .processingCount(processing)
                .resolvedCount(resolved)
                .ignoredCount(ignored)
                .criticalCount(critical)
                .build();
    }

    @Override
    public HealthAlertVO getAlertDetail(Long id) {
        HealthAlert alert = checkOrgAccess(id);
        List<HealthAlertVO> vos = enrichAlerts(Collections.singletonList(alert));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluateAndTriggerAlert(HealthRecord record) {
        if (record == null || record.getMetricValue() == null) {
            return;
        }

        // 查询针对当前机构和当前指标启用的所有预警规则
        LambdaQueryWrapper<HealthAlertRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(HealthAlertRule::getOrgId, record.getOrgId())
                .eq(HealthAlertRule::getMetricId, record.getMetricId())
                .eq(HealthAlertRule::getEnabled, 1);
        List<HealthAlertRule> rules = healthAlertRuleMapper.selectList(ruleWrapper);

        if (CollectionUtils.isEmpty(rules)) {
            return;
        }

        BigDecimal value = record.getMetricValue();

        for (HealthAlertRule rule : rules) {
            boolean matched = isConditionMatched(rule, value);
            if (!matched) {
                continue;
            }

            // 命中规则，进行时间窗口去重检查
            int dedupMinutes = rule.getDedupMinutes() != null && rule.getDedupMinutes() > 0 ? rule.getDedupMinutes() : 30;
            LocalDateTime windowStart = LocalDateTime.now().minusMinutes(dedupMinutes);

            Long recentAlertCount = healthAlertMapper.selectCount(
                    new LambdaQueryWrapper<HealthAlert>()
                            .eq(HealthAlert::getOrgId, record.getOrgId())
                            .eq(HealthAlert::getElderId, record.getElderId())
                            .eq(HealthAlert::getRuleId, rule.getId())
                            .ge(HealthAlert::getCreatedAt, windowStart)
            );

            if (recentAlertCount > 0) {
                log.info("长者 [ID={}] 触发健康预警规则 [ID={}, 规则名={}], 但在去重窗口 [{}分钟] 内已存在预警, 抑制重复告警",
                        record.getElderId(), rule.getId(), rule.getRuleName(), dedupMinutes);
                continue;
            }

            // 未处于去重窗口内，生成新预警
            String alertNo = generateAlertNo();
            HealthAlert alert = HealthAlert.builder()
                    .orgId(record.getOrgId())
                    .elderId(record.getElderId())
                    .healthRecordId(record.getId())
                    .ruleId(rule.getId())
                    .alertNo(alertNo)
                    .severity(rule.getSeverity())
                    .alertValue(record.getMetricValue())
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .build();

            healthAlertMapper.insert(alert);

            log.warn("生成健康预警: 单号={}, 长者ID={}, 规则={}, 级别={}, 测量值={}",
                    alertNo, record.getElderId(), rule.getRuleName(), rule.getSeverity(), record.getMetricValue());

            // 记录系统审计日志
            sysOperationLogService.recordLog(
                    "HEALTH_ALERT",
                    "TRIGGER",
                    "HEALTH_ALERT",
                    alert.getId(),
                    "/api/v1/health/alerts",
                    "127.0.0.1",
                    String.format("健康记录触发预警: 单号=%s, 长者ID=%d, 规则=%s, 级别=%s, 异常数值=%s",
                            alertNo, record.getElderId(), rule.getRuleName(), rule.getSeverity(), record.getMetricValue())
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignAlert(Long id, HealthAlertAssignDTO dto) {
        HealthAlert alert = healthAlertMapper.selectByIdForUpdate(id);
        if (alert == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康预警不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(alert.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的健康预警");
        }

        SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指定的处理人不存在");
        }
        if (currentOrgId != null && assignee.getOrgId() != null && !currentOrgId.equals(assignee.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员处理");
        }

        alert.setAssigneeId(dto.getAssigneeId());
        if ("OPEN".equals(alert.getStatus())) {
            alert.setStatus("PROCESSING");
        }
        healthAlertMapper.updateById(alert);

        sysOperationLogService.recordLog(
                "HEALTH_ALERT",
                "ASSIGN",
                "HEALTH_ALERT",
                alert.getId(),
                "/api/v1/health/alerts/" + id + "/assign",
                "127.0.0.1",
                String.format("指派预警 [%s] 给责任人 [%s]", alert.getAlertNo(), assignee.getRealName())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAlert(Long id, HealthAlertHandleDTO dto) {
        HealthAlert alert = healthAlertMapper.selectByIdForUpdate(id);
        if (alert == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康预警不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();
        if (currentOrgId != null && !currentOrgId.equals(alert.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的健康预警");
        }

        String targetStatus = dto.getStatus().toUpperCase().trim();
        if (!"RESOLVED".equals(targetStatus) && !"IGNORED".equals(targetStatus)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "处置状态必须为 RESOLVED 或 IGNORED");
        }

        alert.setStatus(targetStatus);
        alert.setHandleResult(dto.getHandleResult().trim());
        alert.setHandledAt(LocalDateTime.now());
        if (alert.getAssigneeId() == null && currentUserId != null) {
            alert.setAssigneeId(currentUserId);
        }

        healthAlertMapper.updateById(alert);

        sysOperationLogService.recordLog(
                "HEALTH_ALERT",
                "HANDLE",
                "HEALTH_ALERT",
                alert.getId(),
                "/api/v1/health/alerts/" + id + "/handle",
                "127.0.0.1",
                String.format("处置健康预警 [%s]，结果状态: %s，处置说明: %s",
                        alert.getAlertNo(), targetStatus, dto.getHandleResult())
        );
    }

    private boolean isConditionMatched(HealthAlertRule rule, BigDecimal value) {
        String ct = rule.getConditionType().toUpperCase().trim();
        if ("GT".equals(ct) || "GREATER_THAN".equals(ct)) {
            return rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0;
        } else if ("LT".equals(ct) || "LESS_THAN".equals(ct)) {
            return rule.getMinValue() != null && value.compareTo(rule.getMinValue()) < 0;
        } else if ("OUT_OF_RANGE".equals(ct)) {
            boolean lowerViolated = rule.getMinValue() != null && value.compareTo(rule.getMinValue()) < 0;
            boolean upperViolated = rule.getMaxValue() != null && value.compareTo(rule.getMaxValue()) > 0;
            return lowerViolated || upperViolated;
        }
        return false;
    }

    private HealthAlert checkOrgAccess(Long id) {
        HealthAlert alert = healthAlertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "健康预警不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(alert.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的健康预警");
        }
        return alert;
    }

    private List<HealthAlertVO> enrichAlerts(List<HealthAlert> alerts) {
        if (CollectionUtils.isEmpty(alerts)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = alerts.stream().map(HealthAlert::getElderId).collect(Collectors.toSet());
        Set<Long> ruleIds = alerts.stream().map(HealthAlert::getRuleId).collect(Collectors.toSet());
        Set<Long> userIds = alerts.stream().map(HealthAlert::getAssigneeId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, HealthAlertRule> ruleMap = ruleIds.isEmpty() ? Collections.emptyMap() :
                healthAlertRuleMapper.selectBatchIds(ruleIds).stream().collect(Collectors.toMap(HealthAlertRule::getId, r -> r));

        Set<Long> metricIds = ruleMap.values().stream().map(HealthAlertRule::getMetricId).collect(Collectors.toSet());
        Map<Long, HealthMetric> metricMap = metricIds.isEmpty() ? Collections.emptyMap() :
                healthMetricMapper.selectBatchIds(metricIds).stream().collect(Collectors.toMap(HealthMetric::getId, m -> m));

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Map<Long, String> bedInfoMap = buildElderBedInfoMap(elderIds);

        return alerts.stream().map(a -> {
            Elder elder = elderMap.get(a.getElderId());
            HealthAlertRule rule = ruleMap.get(a.getRuleId());
            HealthMetric metric = rule != null ? metricMap.get(rule.getMetricId()) : null;
            SysUser assignee = a.getAssigneeId() != null ? userMap.get(a.getAssigneeId()) : null;

            return HealthAlertVO.builder()
                    .id(a.getId())
                    .orgId(a.getOrgId())
                    .alertNo(a.getAlertNo())
                    .elderId(a.getElderId())
                    .elderName(elder != null ? elder.getName() : null)
                    .elderNo(elder != null ? elder.getElderNo() : null)
                    .bedInfo(elder != null ? bedInfoMap.get(elder.getId()) : null)
                    .healthRecordId(a.getHealthRecordId())
                    .metricId(metric != null ? metric.getId() : null)
                    .metricCode(metric != null ? metric.getMetricCode() : null)
                    .metricName(metric != null ? metric.getMetricName() : null)
                    .unit(metric != null ? metric.getUnit() : null)
                    .ruleId(a.getRuleId())
                    .ruleName(rule != null ? rule.getRuleName() : null)
                    .conditionType(rule != null ? rule.getConditionType() : null)
                    .thresholdMin(rule != null ? rule.getMinValue() : null)
                    .thresholdMax(rule != null ? rule.getMaxValue() : null)
                    .severity(a.getSeverity())
                    .alertValue(a.getAlertValue())
                    .status(a.getStatus())
                    .assigneeId(a.getAssigneeId())
                    .assigneeName(assignee != null ? assignee.getRealName() : null)
                    .handledAt(a.getHandledAt())
                    .handleResult(a.getHandleResult())
                    .createdAt(a.getCreatedAt())
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

    private String generateAlertNo() {
        return "ALT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
