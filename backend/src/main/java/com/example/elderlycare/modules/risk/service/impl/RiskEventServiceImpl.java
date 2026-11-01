package com.example.elderlycare.modules.risk.service.impl;

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
import com.example.elderlycare.modules.risk.dto.*;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.entity.RiskEventLog;
import com.example.elderlycare.modules.risk.mapper.RiskEventLogMapper;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.risk.service.RiskEventService;
import com.example.elderlycare.modules.risk.vo.RiskEventDetailVO;
import com.example.elderlycare.modules.risk.vo.RiskEventLogVO;
import com.example.elderlycare.modules.risk.vo.RiskEventSummaryVO;
import com.example.elderlycare.modules.risk.vo.RiskEventVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEventServiceImpl implements RiskEventService {

    private final RiskEventMapper riskEventMapper;
    private final RiskEventLogMapper riskEventLogMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long reportEvent(RiskEventReportDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        Long orgId = currentOrgId;
        Elder elder = null;
        if (dto.getElderId() != null) {
            elder = elderMapper.selectById(dto.getElderId());
            if (elder == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的长者档案不存在");
            }
            if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权为其他机构长者上报风险事件");
            }
            if (orgId == null) {
                orgId = elder.getOrgId();
            }
        }
        if (orgId == null) {
            orgId = 1L;
        }

        if (dto.getAssigneeId() != null) {
            SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
            if (assignee == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指定的处理负责人不存在");
            }
            if (assignee.getOrgId() != null && !orgId.equals(assignee.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员作为负责人");
            }
        }

        String eventNo = generateEventNo();
        String initialStatus = dto.getAssigneeId() != null ? "ASSIGNED" : "REPORTED";

        RiskEvent event = RiskEvent.builder()
                .orgId(orgId)
                .eventNo(eventNo)
                .elderId(dto.getElderId())
                .eventType(dto.getEventType().trim().toUpperCase())
                .severity(dto.getSeverity().trim().toUpperCase())
                .occurredAt(dto.getOccurredAt())
                .locationDesc(dto.getLocationDesc())
                .description(dto.getDescription())
                .reporterId(currentUserId)
                .assigneeId(dto.getAssigneeId())
                .status(initialStatus)
                .conclusion(null)
                .closedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        riskEventMapper.insert(event);

        // 记录初始上报流转日志
        String logContent = dto.getAssigneeId() != null
                ? "上报登记风险事件，并直接指派处理负责人"
                : "上报登记风险事件（待分派处理）";
        RiskEventLog eventLog = RiskEventLog.builder()
                .orgId(orgId)
                .riskEventId(event.getId())
                .action("REPORT")
                .fromStatus(null)
                .toStatus(initialStatus)
                .operatorId(currentUserId)
                .content(logContent)
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(eventLog);

        // 系统审计日志
        sysOperationLogService.recordLog(
                "RISK_EVENT",
                "CREATE",
                "RISK_EVENT",
                event.getId(),
                "/api/v1/risk-events",
                "127.0.0.1",
                String.format("上报风险事件: 单号=%s, 类型=%s, 等级=%s, 初始状态=%s",
                        eventNo, event.getEventType(), event.getSeverity(), initialStatus)
        );

        return event.getId();
    }

    @Override
    public PageResult<RiskEventVO> pageEvents(RiskEventQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<RiskEvent> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(RiskEvent::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(query.getEventType())) {
            qw.eq(RiskEvent::getEventType, query.getEventType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getSeverity())) {
            qw.eq(RiskEvent::getSeverity, query.getSeverity().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(RiskEvent::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (query.getElderId() != null) {
            qw.eq(RiskEvent::getElderId, query.getElderId());
        }
        if (query.getAssigneeId() != null) {
            qw.eq(RiskEvent::getAssigneeId, query.getAssigneeId());
        }
        if (query.getReporterId() != null) {
            qw.eq(RiskEvent::getReporterId, query.getReporterId());
        }
        if (query.getStartTime() != null) {
            qw.ge(RiskEvent::getOccurredAt, query.getStartTime());
        }
        if (query.getEndTime() != null) {
            qw.le(RiskEvent::getOccurredAt, query.getEndTime());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(RiskEvent::getEventNo, kw)
                    .or().like(RiskEvent::getLocationDesc, kw)
                    .or().like(RiskEvent::getDescription, kw)
                    .or().like(RiskEvent::getConclusion, kw));
        }

        qw.orderByDesc(RiskEvent::getOccurredAt).orderByDesc(RiskEvent::getId);

        Page<RiskEvent> page = riskEventMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<RiskEventVO> voList = convertToVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public RiskEventSummaryVO getSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<RiskEvent> baseQw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            baseQw.eq(RiskEvent::getOrgId, currentOrgId);
        }

        Long totalCount = riskEventMapper.selectCount(baseQw);

        Long reportedCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .eq(RiskEvent::getStatus, "REPORTED")
        );

        Long assignedCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .eq(RiskEvent::getStatus, "ASSIGNED")
        );

        Long processingCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .eq(RiskEvent::getStatus, "PROCESSING")
        );

        Long pendingReviewCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .eq(RiskEvent::getStatus, "PENDING_REVIEW")
        );

        Long closedCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .eq(RiskEvent::getStatus, "CLOSED")
        );

        Long highCriticalCount = riskEventMapper.selectCount(
                new LambdaQueryWrapper<RiskEvent>()
                        .eq(currentOrgId != null, RiskEvent::getOrgId, currentOrgId)
                        .in(RiskEvent::getSeverity, Arrays.asList("HIGH", "CRITICAL"))
        );

        return RiskEventSummaryVO.builder()
                .totalCount(totalCount)
                .reportedCount(reportedCount)
                .assignedCount(assignedCount)
                .processingCount(processingCount)
                .pendingReviewCount(pendingReviewCount)
                .closedCount(closedCount)
                .highCriticalCount(highCriticalCount)
                .build();
    }

    @Override
    public RiskEventDetailVO getEventDetail(Long id) {
        RiskEvent event = getAndCheckRiskEvent(id);
        List<RiskEventVO> voList = convertToVOList(Collections.singletonList(event));
        RiskEventVO eventVO = voList.get(0);

        List<RiskEventLog> logs = riskEventLogMapper.selectList(
                new LambdaQueryWrapper<RiskEventLog>()
                        .eq(RiskEventLog::getRiskEventId, id)
                        .orderByAsc(RiskEventLog::getCreatedAt)
                        .orderByAsc(RiskEventLog::getId)
        );

        List<RiskEventLogVO> logVOList = convertToLogVOList(logs);

        return RiskEventDetailVO.builder()
                .event(eventVO)
                .logs(logVOList)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignEvent(Long id, RiskEventAssignDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已结案关闭的风险事件不可重新指派");
        }

        SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指定的处理负责人不存在");
        }
        if (assignee.getOrgId() != null && !event.getOrgId().equals(assignee.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员作为负责人");
        }

        String oldStatus = event.getStatus();
        String newStatus = "REPORTED".equalsIgnoreCase(oldStatus) ? "ASSIGNED" : oldStatus;

        event.setAssigneeId(dto.getAssigneeId());
        event.setStatus(newStatus);
        riskEventMapper.updateById(event);

        String remarkSuffix = StringUtils.hasText(dto.getRemark()) ? "，要求: " + dto.getRemark().trim() : "";
        RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("ASSIGN")
                .fromStatus(oldStatus)
                .toStatus(newStatus)
                .operatorId(currentUserId)
                .content("指派处理负责人: " + assignee.getRealName() + remarkSuffix)
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(logRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startProcess(Long id, RiskEventProcessDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已结案关闭的风险事件不可重复处置");
        }

        String oldStatus = event.getStatus();
        // 如果当前未指派负责人，开始处置时自动将当前用户作为处理负责人
        if (event.getAssigneeId() == null) {
            event.setAssigneeId(currentUserId);
        }
        event.setStatus("PROCESSING");
        riskEventMapper.updateById(event);

        RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("START_PROCESS")
                .fromStatus(oldStatus)
                .toStatus("PROCESSING")
                .operatorId(currentUserId)
                .content(StringUtils.hasText(dto.getContent()) ? dto.getContent() : "现场介入并开始处置风险事件")
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(logRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long id, RiskEventProcessDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已结案关闭的事件不可更新处置进展");
        }

        RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("PROGRESS_UPDATE")
                .fromStatus(event.getStatus())
                .toStatus(event.getStatus())
                .operatorId(currentUserId)
                .content(dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(logRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long id, RiskEventSubmitReviewDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已结案关闭的事件不可重复提交复核");
        }
        if ("PENDING_REVIEW".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "当前事件已处于待复核状态，请勿重复提交");
        }

        String oldStatus = event.getStatus();
        event.setStatus("PENDING_REVIEW");
        riskEventMapper.updateById(event);

        RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("SUBMIT_REVIEW")
                .fromStatus(oldStatus)
                .toStatus("PENDING_REVIEW")
                .operatorId(currentUserId)
                .content("提交复核申请: " + dto.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(logRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewEvent(Long id, RiskEventReviewDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        // 校验主管/管理员复核权限
        if (!isSupervisorOrAdmin(currentUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅机构管理员或护理主管有权限复核风险事件");
        }

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该事件已结案，无法重复复核");
        }

        String oldStatus = event.getStatus();

        if (Boolean.TRUE.equals(dto.getApproved())) {
            if (!StringUtils.hasText(dto.getConclusion())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "复核通过结案必须填写处理结论");
            }

            event.setStatus("CLOSED");
            event.setConclusion(dto.getConclusion().trim());
            event.setClosedAt(LocalDateTime.now());
            riskEventMapper.updateById(event);

            RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("CLOSE")
                .fromStatus(oldStatus)
                .toStatus("CLOSED")
                .operatorId(currentUserId)
                .content("主管复核通过并结案归档。结论: " + dto.getConclusion().trim())
                .createdAt(LocalDateTime.now())
                .build();
            riskEventLogMapper.insert(logRecord);

            // 系统审计日志
            sysOperationLogService.recordLog(
                    "RISK_EVENT",
                    "CLOSE",
                    "RISK_EVENT",
                    event.getId(),
                    "/api/v1/risk-events/" + id + "/review",
                    "127.0.0.1",
                    String.format("主管复核关闭风险事件: 单号=%s, 结论=%s", event.getEventNo(), event.getConclusion())
            );
        } else {
            if (!StringUtils.hasText(dto.getRejectReason())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "复核驳回必须填写驳回原因与整改要求");
            }

            event.setStatus("PROCESSING");
            riskEventMapper.updateById(event);

            RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("REVIEW_REJECT")
                .fromStatus(oldStatus)
                .toStatus("PROCESSING")
                .operatorId(currentUserId)
                .content("主管复核驳回，退回处置阶段重新整改。原因: " + dto.getRejectReason().trim())
                .createdAt(LocalDateTime.now())
                .build();
            riskEventLogMapper.insert(logRecord);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeEvent(Long id, RiskEventCloseDTO dto) {
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;
        RiskEvent event = riskEventMapper.selectByIdForUpdate(id);
        checkEventAccess(event);

        if ("CLOSED".equalsIgnoreCase(event.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该事件已结案关闭，无需重复操作");
        }

        if (!StringUtils.hasText(dto.getConclusion())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关闭事件必须填写处理结论");
        }

        boolean isHighRisk = "HIGH".equalsIgnoreCase(event.getSeverity()) || "CRITICAL".equalsIgnoreCase(event.getSeverity());
        boolean isSupervisor = isSupervisorOrAdmin(currentUserId);

        // PRD 规则：高等级事件需主管复核后关闭
        if (isHighRisk && !"PENDING_REVIEW".equalsIgnoreCase(event.getStatus()) && !isSupervisor) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "高风险或危急事件必须由机构主管复核后方可结案，请先提交复核申请");
        }

        String oldStatus = event.getStatus();
        event.setStatus("CLOSED");
        event.setConclusion(dto.getConclusion().trim());
        event.setClosedAt(LocalDateTime.now());
        riskEventMapper.updateById(event);

        RiskEventLog logRecord = RiskEventLog.builder()
                .orgId(event.getOrgId())
                .riskEventId(event.getId())
                .action("CLOSE")
                .fromStatus(oldStatus)
                .toStatus("CLOSED")
                .operatorId(currentUserId)
                .content("结案关闭: " + dto.getConclusion().trim())
                .createdAt(LocalDateTime.now())
                .build();
        riskEventLogMapper.insert(logRecord);

        // 系统审计日志
        sysOperationLogService.recordLog(
                "RISK_EVENT",
                "CLOSE",
                "RISK_EVENT",
                event.getId(),
                "/api/v1/risk-events/" + id + "/close",
                "127.0.0.1",
                String.format("结案关闭风险事件: 单号=%s, 结论=%s", event.getEventNo(), event.getConclusion())
        );
    }

    private RiskEvent getAndCheckRiskEvent(Long id) {
        RiskEvent event = riskEventMapper.selectById(id);
        if (event == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "风险事件记录不存在");
        }
        checkEventAccess(event);
        return event;
    }

    private void checkEventAccess(RiskEvent event) {
        if (event == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "风险事件记录不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(event.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的风险事件");
        }
    }

    private boolean isSupervisorOrAdmin(Long userId) {
        // 1. 尝试从 Spring Security 上下文获取
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null) {
            boolean hasAuth = auth.getAuthorities().stream().anyMatch(a ->
                    "ROLE_ORG_ADMIN".equals(a.getAuthority()) ||
                    "ROLE_NURSE_DIRECTOR".equals(a.getAuthority()) ||
                    "risk:review".equals(a.getAuthority())
            );
            if (hasAuth) {
                return true;
            }
        }

        // 2. 备用：从数据库查询用户分配的角色
        if (userId != null) {
            List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
            if (!CollectionUtils.isEmpty(roleCodes)) {
                return roleCodes.contains("ORG_ADMIN") || roleCodes.contains("NURSE_DIRECTOR");
            }
        }
        return false;
    }

    private String generateEventNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "RSK" + dateStr + rand;
    }

    private List<RiskEventVO> convertToVOList(List<RiskEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = events.stream()
                .map(RiskEvent::getElderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream()
                        .collect(Collectors.toMap(Elder::getId, e -> e));

        // 批量查询在住长者当前床位信息
        Map<Long, String> elderBedMap = new HashMap<>();
        if (!elderIds.isEmpty()) {
            List<Admission> activeAdmissions = admissionMapper.selectList(
                    new LambdaQueryWrapper<Admission>()
                            .in(Admission::getElderId, elderIds)
                            .eq(Admission::getStatus, "CHECKED_IN")
            );
            if (!CollectionUtils.isEmpty(activeAdmissions)) {
                Set<Long> bedIds = activeAdmissions.stream().map(Admission::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
                if (!bedIds.isEmpty()) {
                    Map<Long, Bed> bedMap = bedMapper.selectBatchIds(bedIds).stream().collect(Collectors.toMap(Bed::getId, b -> b));
                    Set<Long> roomIds = bedMap.values().stream().map(Bed::getRoomId).filter(Objects::nonNull).collect(Collectors.toSet());
                    Map<Long, Room> roomMap = roomIds.isEmpty() ? Collections.emptyMap() :
                            roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, r -> r));
                    Set<Long> buildingIds = roomMap.values().stream().map(Room::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
                    Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Collections.emptyMap() :
                            buildingMapper.selectBatchIds(buildingIds).stream().collect(Collectors.toMap(Building::getId, b -> b));

                    for (Admission adm : activeAdmissions) {
                        Bed bed = bedMap.get(adm.getBedId());
                        if (bed != null) {
                            Room room = roomMap.get(bed.getRoomId());
                            Building building = room != null ? buildingMap.get(room.getBuildingId()) : null;
                            String bName = building != null ? building.getBuildingName() : "";
                            String rName = room != null ? room.getRoomNumber() + "室" : "";
                            String bedStr = bed.getBedNumber() + "床";
                            elderBedMap.put(adm.getElderId(), String.join(" ", bName, rName, bedStr).trim());
                        }
                    }
                }
            }
        }

        Set<Long> userIds = new HashSet<>();
        for (RiskEvent e : events) {
            if (e.getReporterId() != null) userIds.add(e.getReporterId());
            if (e.getAssigneeId() != null) userIds.add(e.getAssigneeId());
        }
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return events.stream().map(e -> {
            Elder el = e.getElderId() != null ? elderMap.get(e.getElderId()) : null;
            SysUser reporter = e.getReporterId() != null ? userMap.get(e.getReporterId()) : null;
            SysUser assignee = e.getAssigneeId() != null ? userMap.get(e.getAssigneeId()) : null;

            boolean reqReview = "HIGH".equalsIgnoreCase(e.getSeverity()) || "CRITICAL".equalsIgnoreCase(e.getSeverity());

            return RiskEventVO.builder()
                    .id(e.getId())
                    .orgId(e.getOrgId())
                    .eventNo(e.getEventNo())
                    .elderId(e.getElderId())
                    .elderName(el != null ? el.getName() : null)
                    .elderNo(el != null ? el.getElderNo() : null)
                    .bedInfo(e.getElderId() != null ? elderBedMap.getOrDefault(e.getElderId(), "-") : "-")
                    .eventType(e.getEventType())
                    .eventTypeName(getEventTypeName(e.getEventType()))
                    .severity(e.getSeverity())
                    .severityName(getSeverityName(e.getSeverity()))
                    .occurredAt(e.getOccurredAt())
                    .locationDesc(e.getLocationDesc())
                    .description(e.getDescription())
                    .reporterId(e.getReporterId())
                    .reporterName(reporter != null ? reporter.getRealName() : "系统自动")
                    .assigneeId(e.getAssigneeId())
                    .assigneeName(assignee != null ? assignee.getRealName() : "未分派")
                    .status(e.getStatus())
                    .statusName(getStatusName(e.getStatus()))
                    .conclusion(e.getConclusion())
                    .closedAt(e.getClosedAt())
                    .createdAt(e.getCreatedAt())
                    .requiresReview(reqReview)
                    .build();
        }).collect(Collectors.toList());
    }

    private List<RiskEventLogVO> convertToLogVOList(List<RiskEventLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }

        Set<Long> operatorIds = logs.stream()
                .map(RiskEventLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = operatorIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(operatorIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return logs.stream().map(l -> {
            SysUser operator = l.getOperatorId() != null ? userMap.get(l.getOperatorId()) : null;
            return RiskEventLogVO.builder()
                    .id(l.getId())
                    .riskEventId(l.getRiskEventId())
                    .action(l.getAction())
                    .actionName(getActionName(l.getAction()))
                    .fromStatus(l.getFromStatus())
                    .fromStatusName(getStatusName(l.getFromStatus()))
                    .toStatus(l.getToStatus())
                    .toStatusName(getStatusName(l.getToStatus()))
                    .operatorId(l.getOperatorId())
                    .operatorName(operator != null ? operator.getRealName() : "系统自动")
                    .content(l.getContent())
                    .createdAt(l.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String getEventTypeName(String type) {
        if (type == null) return "未知类型";
        return switch (type.toUpperCase()) {
            case "FALL" -> "跌倒事件";
            case "LOST" -> "长者走失";
            case "SUDDEN_ILLNESS" -> "突发急症";
            case "DEVICE_SOS" -> "设备呼救报警";
            case "CHOKING" -> "噎食误吸";
            case "CONFLICT" -> "争执纠纷";
            case "OTHER" -> "其他安全事件";
            default -> type;
        };
    }

    private String getSeverityName(String severity) {
        if (severity == null) return "未知等级";
        return switch (severity.toUpperCase()) {
            case "LOW" -> "低风险 / 轻微";
            case "MEDIUM" -> "中风险 / 一般";
            case "HIGH" -> "高风险 / 严重";
            case "CRITICAL" -> "危急 / 特大";
            default -> severity;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "REPORTED" -> "待分派";
            case "ASSIGNED" -> "已分派";
            case "PROCESSING" -> "处置中";
            case "PENDING_REVIEW" -> "待复核";
            case "CLOSED" -> "已结案";
            default -> status;
        };
    }

    private String getActionName(String action) {
        if (action == null) return "-";
        return switch (action.toUpperCase()) {
            case "REPORT" -> "事件上报登记";
            case "ASSIGN" -> "指派责任人";
            case "START_PROCESS" -> "接单开始处置";
            case "PROGRESS_UPDATE" -> "更新处置进展";
            case "SUBMIT_REVIEW" -> "提交复核申请";
            case "REVIEW_REJECT" -> "复核驳回整改";
            case "CLOSE" -> "结案关闭归档";
            default -> action;
        };
    }
}
