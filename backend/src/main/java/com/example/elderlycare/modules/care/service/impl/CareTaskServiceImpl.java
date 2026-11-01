package com.example.elderlycare.modules.care.service.impl;

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
import com.example.elderlycare.modules.care.dto.CareTaskActionDTO;
import com.example.elderlycare.modules.care.dto.CareTaskAssignDTO;
import com.example.elderlycare.modules.care.dto.CareTaskGenerateDTO;
import com.example.elderlycare.modules.care.dto.CareTaskQueryDTO;
import com.example.elderlycare.modules.care.entity.*;
import com.example.elderlycare.modules.care.mapper.*;
import com.example.elderlycare.modules.care.service.CareTaskService;
import com.example.elderlycare.modules.care.vo.CareTaskRecordVO;
import com.example.elderlycare.modules.care.vo.CareTaskSummaryVO;
import com.example.elderlycare.modules.care.vo.CareTaskVO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CareTaskServiceImpl implements CareTaskService {

    private final CareTaskMapper careTaskMapper;
    private final CareTaskRecordMapper careTaskRecordMapper;
    private final CarePlanMapper carePlanMapper;
    private final CarePlanItemMapper carePlanItemMapper;
    private final CareItemMapper careItemMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateTasks(CareTaskGenerateDTO dto) {
        LocalDate taskDate = dto.getTaskDate() != null ? dto.getTaskDate() : LocalDate.now();
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 1. 查询当前机构内对该日期有效的所有启用护理计划
        LambdaQueryWrapper<CarePlan> planWrapper = new LambdaQueryWrapper<>();
        planWrapper.eq(currentOrgId != null, CarePlan::getOrgId, currentOrgId)
                .eq(CarePlan::getStatus, "ACTIVE")
                .le(CarePlan::getStartDate, taskDate)
                .and(w -> w.isNull(CarePlan::getEndDate).or().ge(CarePlan::getEndDate, taskDate));

        if (dto.getElderId() != null) {
            planWrapper.eq(CarePlan::getElderId, dto.getElderId());
        }

        List<CarePlan> activePlans = carePlanMapper.selectList(planWrapper);
        if (CollectionUtils.isEmpty(activePlans)) {
            log.info("日期 [{}] 未匹配到有效的护理计划，生成任务数: 0", taskDate);
            return 0;
        }

        int generatedCount = 0;

        for (CarePlan plan : activePlans) {
            // 查询计划下启用的明细项
            List<CarePlanItem> items = carePlanItemMapper.selectList(
                    new LambdaQueryWrapper<CarePlanItem>()
                            .eq(CarePlanItem::getPlanId, plan.getId())
                            .eq(CarePlanItem::getEnabled, 1)
            );

            for (CarePlanItem item : items) {
                // 校验频次是否命中当前日期
                if (!isFrequencyMatched(item, plan.getStartDate(), taskDate)) {
                    continue;
                }

                // 构造稳定幂等键：计划项ID + 任务日期 + 时间窗口开始时间
                String timeStartStr = item.getTimeWindowStart() != null ? item.getTimeWindowStart().toString() : "00:00:00";
                String idempotencyKey = String.format("CPI_%d_%s_%s", item.getId(), taskDate.toString(), timeStartStr);

                // 2. 检查幂等性：同一计划项在同执行周期内只能生成一条任务
                Long count = careTaskMapper.selectCount(
                        new LambdaQueryWrapper<CareTask>()
                                .eq(CareTask::getOrgId, plan.getOrgId())
                                .eq(CareTask::getIdempotencyKey, idempotencyKey)
                );
                if (count > 0) {
                    log.debug("护理任务生成幂等抑制: 明细项 [{}] 日期 [{}] 任务已存在，忽略重复创建", item.getId(), taskDate);
                    continue;
                }

                String taskNo = generateTaskNo();
                LocalTime scheduledTime = item.getTimeWindowStart() != null ? item.getTimeWindowStart() : LocalTime.of(8, 0);
                LocalDateTime scheduledAt = taskDate.atTime(scheduledTime);

                CareTask task = CareTask.builder()
                        .orgId(plan.getOrgId())
                        .taskNo(taskNo)
                        .planId(plan.getId())
                        .planItemId(item.getId())
                        .elderId(plan.getElderId())
                        .taskDate(taskDate)
                        .scheduledAt(scheduledAt)
                        .assigneeId(item.getAssigneeId())
                        .status("PENDING")
                        .idempotencyKey(idempotencyKey)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                try {
                    careTaskMapper.insert(task);
                    generatedCount++;
                } catch (DuplicateKeyException ex) {
                    // 数据库唯一索引 uk_care_task_idem 终极兜底，并发场景下保证绝对不产生重复任务
                    log.warn("并发任务生成被唯一索引 uk_care_task_idem 拦截: key={}", idempotencyKey);
                }
            }
        }

        log.info("护理任务批处理完成: 日期={}, 机构ID={}, 新生成任务数={}", taskDate, currentOrgId, generatedCount);

        sysOperationLogService.recordLog(
                "CARE_TASK",
                "GENERATE",
                "CARE_TASK",
                0L,
                "/api/v1/care/tasks/generate",
                "127.0.0.1",
                String.format("生成护理任务: 日期=%s, 新增任务数=%d", taskDate, generatedCount)
        );

        return generatedCount;
    }

    @Override
    public PageResult<CareTaskVO> pageTasks(CareTaskQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<CareTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, CareTask::getOrgId, currentOrgId);

        if (StringUtils.hasText(query.getTaskNo())) {
            wrapper.like(CareTask::getTaskNo, query.getTaskNo().trim());
        }
        if (query.getPlanId() != null) {
            wrapper.eq(CareTask::getPlanId, query.getPlanId());
        }
        if (query.getElderId() != null) {
            wrapper.eq(CareTask::getElderId, query.getElderId());
        } else if (StringUtils.hasText(query.getElderName())) {
            LambdaQueryWrapper<Elder> elderWrapper = new LambdaQueryWrapper<>();
            elderWrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(Elder::getName, query.getElderName().trim());
            List<Elder> matchedElders = elderMapper.selectList(elderWrapper);
            if (matchedElders.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(CareTask::getElderId, elderIds);
        }

        if (query.getAssigneeId() != null) {
            wrapper.eq(CareTask::getAssigneeId, query.getAssigneeId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CareTask::getStatus, query.getStatus());
        }
        if (query.getTaskDate() != null) {
            wrapper.eq(CareTask::getTaskDate, query.getTaskDate());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(CareTask::getTaskDate, query.getStartDate());
        }
        if (query.getEndDate() != null) {
            wrapper.le(CareTask::getTaskDate, query.getEndDate());
        }

        wrapper.orderByAsc(CareTask::getScheduledAt).orderByDesc(CareTask::getId);

        Page<CareTask> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<CareTask> pageResult = careTaskMapper.selectPage(pageParam, wrapper);

        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return PageResult.of(Collections.emptyList(), pageResult.getTotal(), query.getPageNum(), query.getPageSize());
        }

        List<CareTaskVO> vos = enrichTasks(pageResult.getRecords());
        return PageResult.of(vos, pageResult.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public CareTaskSummaryVO getTaskSummary(LocalDate taskDate) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LocalDate queryDate = taskDate != null ? taskDate : LocalDate.now();

        LambdaQueryWrapper<CareTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, CareTask::getOrgId, currentOrgId)
                .eq(CareTask::getTaskDate, queryDate);

        List<CareTask> tasks = careTaskMapper.selectList(wrapper);

        int total = tasks.size();
        int pending = 0;
        int inProgress = 0;
        int completed = 0;
        int missed = 0;
        int cancelled = 0;

        for (CareTask t : tasks) {
            String s = t.getStatus();
            if ("PENDING".equals(s)) pending++;
            else if ("IN_PROGRESS".equals(s)) inProgress++;
            else if ("COMPLETED".equals(s)) completed++;
            else if ("MISSED".equals(s)) missed++;
            else if ("CANCELLED".equals(s)) cancelled++;
        }

        return CareTaskSummaryVO.builder()
                .totalTasks(total)
                .pendingCount(pending)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .missedCount(missed)
                .cancelledCount(cancelled)
                .build();
    }

    @Override
    public CareTaskVO getTaskDetail(Long id) {
        CareTask task = checkOrgAccess(id);
        List<CareTaskVO> vos = enrichTasks(Collections.singletonList(task));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(Long id, CareTaskAssignDTO dto) {
        CareTask task = careTaskMapper.selectByIdForUpdate(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理任务不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的护理任务");
        }

        SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指定的护理人员不存在");
        }
        if (currentOrgId != null && assignee.getOrgId() != null && !currentOrgId.equals(assignee.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员");
        }

        task.setAssigneeId(dto.getAssigneeId());
        task.setUpdatedAt(LocalDateTime.now());
        careTaskMapper.updateById(task);

        sysOperationLogService.recordLog(
                "CARE_TASK",
                "ASSIGN",
                "CARE_TASK",
                task.getId(),
                "/api/v1/care/tasks/" + id + "/assign",
                "127.0.0.1",
                String.format("指派任务 [%s] 给责任人 [%s]", task.getTaskNo(), assignee.getRealName())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeTaskAction(Long id, CareTaskActionDTO dto) {
        CareTask task = careTaskMapper.selectByIdForUpdate(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理任务不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();
        if (currentOrgId != null && !currentOrgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的护理任务");
        }

        String action = dto.getAction().toUpperCase().trim();
        String currentStatus = task.getStatus();

        if ("START".equals(action)) {
            if (!"PENDING".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.CONFLICT, "只有【待执行】状态的任务才能开始执行");
            }
            task.setStatus("IN_PROGRESS");
            if (task.getAssigneeId() == null && currentUserId != null) {
                task.setAssigneeId(currentUserId);
            }
        } else if ("COMPLETE".equals(action)) {
            if (!"PENDING".equals(currentStatus) && !"IN_PROGRESS".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.CONFLICT, "当前任务状态不允许标记完成");
            }
            task.setStatus("COMPLETED");

            // 写入执行记录（审计留痕，不可无痕篡改）
            CareTaskRecord record = CareTaskRecord.builder()
                    .orgId(task.getOrgId())
                    .taskId(task.getId())
                    .executorId(currentUserId != null ? currentUserId : 1L)
                    .executedAt(LocalDateTime.now())
                    .resultCode(StringUtils.hasText(dto.getResultCode()) ? dto.getResultCode() : "NORMAL")
                    .resultDesc(dto.getResultDesc())
                    .attachmentJson(dto.getAttachmentJson())
                    .createdAt(LocalDateTime.now())
                    .build();
            careTaskRecordMapper.insert(record);

        } else if ("MISS".equals(action)) {
            if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.CONFLICT, "已完成或已取消任务不可标记为漏执行");
            }
            task.setStatus("MISSED");

            CareTaskRecord record = CareTaskRecord.builder()
                    .orgId(task.getOrgId())
                    .taskId(task.getId())
                    .executorId(currentUserId != null ? currentUserId : 1L)
                    .executedAt(LocalDateTime.now())
                    .resultCode("MISSED")
                    .resultDesc(dto.getResultDesc())
                    .createdAt(LocalDateTime.now())
                    .build();
            careTaskRecordMapper.insert(record);

        } else if ("CANCEL".equals(action)) {
            if ("COMPLETED".equals(currentStatus)) {
                throw new BusinessException(ErrorCode.CONFLICT, "已完成的任务禁止取消");
            }
            task.setStatus("CANCELLED");

            CareTaskRecord record = CareTaskRecord.builder()
                    .orgId(task.getOrgId())
                    .taskId(task.getId())
                    .executorId(currentUserId != null ? currentUserId : 1L)
                    .executedAt(LocalDateTime.now())
                    .resultCode("CANCELLED")
                    .resultDesc(dto.getResultDesc())
                    .createdAt(LocalDateTime.now())
                    .build();
            careTaskRecordMapper.insert(record);

        } else {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未知任务流转动作: " + action);
        }

        task.setUpdatedAt(LocalDateTime.now());
        careTaskMapper.updateById(task);

        sysOperationLogService.recordLog(
                "CARE_TASK",
                action,
                "CARE_TASK",
                task.getId(),
                "/api/v1/care/tasks/" + id + "/action",
                "127.0.0.1",
                String.format("任务 [%s] 执行操作 [%s]，状态流转：%s -> %s",
                        task.getTaskNo(), action, currentStatus, task.getStatus())
        );
    }

    @Override
    public List<CareTaskRecordVO> listTaskRecords(Long taskId) {
        CareTask task = checkOrgAccess(taskId);

        List<CareTaskRecord> records = careTaskRecordMapper.selectList(
                new LambdaQueryWrapper<CareTaskRecord>()
                        .eq(CareTaskRecord::getTaskId, taskId)
                        .orderByAsc(CareTaskRecord::getId)
        );

        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        Set<Long> userIds = records.stream().map(CareTaskRecord::getExecutorId).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return records.stream().map(r -> {
            SysUser user = userMap.get(r.getExecutorId());
            return CareTaskRecordVO.builder()
                    .id(r.getId())
                    .taskId(r.getTaskId())
                    .executorId(r.getExecutorId())
                    .executorName(user != null ? user.getRealName() : "系统自动")
                    .executedAt(r.getExecutedAt())
                    .resultCode(r.getResultCode())
                    .resultDesc(r.getResultDesc())
                    .attachmentJson(r.getAttachmentJson())
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private boolean isFrequencyMatched(CarePlanItem item, LocalDate startDate, LocalDate taskDate) {
        String type = item.getFrequencyType();
        if (!StringUtils.hasText(type) || "DAILY".equalsIgnoreCase(type)) {
            return true;
        }
        String val = item.getFrequencyValue();
        if ("WEEKLY".equalsIgnoreCase(type)) {
            if (!StringUtils.hasText(val)) {
                return true;
            }
            int dayOfWeek = taskDate.getDayOfWeek().getValue();
            String[] parts = val.split(",");
            for (String p : parts) {
                try {
                    if (Integer.parseInt(p.trim()) == dayOfWeek) {
                        return true;
                    }
                } catch (NumberFormatException ignored) {}
            }
            return false;
        } else if ("INTERVAL_DAYS".equalsIgnoreCase(type)) {
            try {
                int interval = Integer.parseInt(val.trim());
                if (interval <= 0) return true;
                long diff = ChronoUnit.DAYS.between(startDate, taskDate);
                return diff >= 0 && diff % interval == 0;
            } catch (NumberFormatException e) {
                return true;
            }
        }
        return true;
    }

    private CareTask checkOrgAccess(Long id) {
        CareTask task = careTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理任务不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(task.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的护理任务");
        }
        return task;
    }

    private List<CareTaskVO> enrichTasks(List<CareTask> tasks) {
        if (CollectionUtils.isEmpty(tasks)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = tasks.stream().map(CareTask::getElderId).collect(Collectors.toSet());
        Set<Long> planIds = tasks.stream().map(CareTask::getPlanId).collect(Collectors.toSet());
        Set<Long> planItemIds = tasks.stream().map(CareTask::getPlanItemId).collect(Collectors.toSet());
        Set<Long> assigneeIds = tasks.stream().map(CareTask::getAssigneeId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, CarePlan> planMap = planIds.isEmpty() ? Collections.emptyMap() :
                carePlanMapper.selectBatchIds(planIds).stream().collect(Collectors.toMap(CarePlan::getId, p -> p));

        Map<Long, CarePlanItem> planItemMap = planItemIds.isEmpty() ? Collections.emptyMap() :
                carePlanItemMapper.selectBatchIds(planItemIds).stream().collect(Collectors.toMap(CarePlanItem::getId, pi -> pi));

        Set<Long> careItemIds = planItemMap.values().stream().map(CarePlanItem::getCareItemId).collect(Collectors.toSet());
        Map<Long, CareItem> careItemMap = careItemIds.isEmpty() ? Collections.emptyMap() :
                careItemMapper.selectBatchIds(careItemIds).stream().collect(Collectors.toMap(CareItem::getId, ci -> ci));

        Map<Long, SysUser> userMap = assigneeIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(assigneeIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Map<Long, String> bedInfoMap = buildElderBedInfoMap(elderIds);

        return tasks.stream().map(t -> {
            Elder elder = elderMap.get(t.getElderId());
            CarePlan plan = planMap.get(t.getPlanId());
            CarePlanItem planItem = planItemMap.get(t.getPlanItemId());
            CareItem careItem = planItem != null ? careItemMap.get(planItem.getCareItemId()) : null;
            SysUser assignee = t.getAssigneeId() != null ? userMap.get(t.getAssigneeId()) : null;

            return CareTaskVO.builder()
                    .id(t.getId())
                    .orgId(t.getOrgId())
                    .taskNo(t.getTaskNo())
                    .planId(t.getPlanId())
                    .planNo(plan != null ? plan.getPlanNo() : null)
                    .planItemId(t.getPlanItemId())
                    .careItemId(careItem != null ? careItem.getId() : null)
                    .careItemCode(careItem != null ? careItem.getItemCode() : null)
                    .careItemName(careItem != null ? careItem.getItemName() : null)
                    .category(careItem != null ? careItem.getCategory() : null)
                    .elderId(t.getElderId())
                    .elderName(elder != null ? elder.getName() : null)
                    .elderNo(elder != null ? elder.getElderNo() : null)
                    .bedInfo(elder != null ? bedInfoMap.get(elder.getId()) : null)
                    .taskDate(t.getTaskDate())
                    .scheduledAt(t.getScheduledAt())
                    .assigneeId(t.getAssigneeId())
                    .assigneeName(assignee != null ? assignee.getRealName() : null)
                    .status(t.getStatus())
                    .idempotencyKey(t.getIdempotencyKey())
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
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

    private String generateTaskNo() {
        return "CT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
