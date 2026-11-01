package com.example.elderlycare.modules.admission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.common.util.DesensitizeUtil;
import com.example.elderlycare.modules.admission.dto.AdmissionCheckInDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionDischargeDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionQueryDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionTransferDTO;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.BedChangeLog;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedChangeLogMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.admission.service.AdmissionService;
import com.example.elderlycare.modules.admission.vo.AdmissionPrecheckVO;
import com.example.elderlycare.modules.admission.vo.AdmissionVO;
import com.example.elderlycare.modules.admission.vo.BedChangeLogVO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderService;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionServiceImpl implements AdmissionService {

    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final BedChangeLogMapper bedChangeLogMapper;
    private final BuildingMapper buildingMapper;
    private final RoomMapper roomMapper;
    private final ElderMapper elderMapper;
    private final ElderService elderService;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long checkIn(AdmissionCheckInDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        log.info("开始办理入住: orgId={}, elderId={}, bedId={}", currentOrgId, dto.getElderId(), dto.getBedId());

        // 1. 校验长者档案归属及状态
        Elder elder = elderService.getElderEntityById(dto.getElderId());
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的长者");
        }
        if ("ARCHIVED".equals(elder.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "长者档案已被归档，无法办理入住");
        }

        // 2. 校验长者是否已有有效在住记录（同一长者在同一机构只能有一条 ACTIVE 入住记录）
        Long activeAdmissionCount = admissionMapper.selectCount(
                new LambdaQueryWrapper<Admission>()
                        .eq(currentOrgId != null, Admission::getOrgId, currentOrgId)
                        .eq(Admission::getElderId, dto.getElderId())
                        .eq(Admission::getStatus, "ACTIVE")
        );
        if (activeAdmissionCount > 0 || "IN_HOUSE".equals(elder.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该长者当前已有生效中的在住记录，不可重复办理入住");
        }

        // 3. 校验日期
        if (dto.getPlannedDischargeDate() != null && dto.getPlannedDischargeDate().isBefore(dto.getAdmissionDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划退住日期不得早于入住日期");
        }

        // 4. 并发关键路径：使用悲观排他行锁锁定床位行记录 (SELECT ... FOR UPDATE)
        Bed bed = bedMapper.selectByIdForUpdate(dto.getBedId());
        if (bed == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目标床位不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(bed.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权分配其他机构的床位");
        }
        if (!"AVAILABLE".equals(bed.getStatus())) {
            log.warn("床位状态已被修改或占用: bedId={}, currentStatus={}", bed.getId(), bed.getStatus());
            throw new BusinessException(ErrorCode.CONFLICT, "目标床位不可用或已被他人抢占（当前状态: " + bed.getStatus() + "）");
        }

        // 5. 生成入住单号
        String admissionNo = generateAdmissionNo();

        // 6. 插入入住记录
        Admission admission = Admission.builder()
                .orgId(currentOrgId != null ? currentOrgId : bed.getOrgId())
                .admissionNo(admissionNo)
                .elderId(elder.getId())
                .bedId(bed.getId())
                .contractNo(dto.getContractNo())
                .admissionDate(dto.getAdmissionDate())
                .plannedDischargeDate(dto.getPlannedDischargeDate())
                .careLevelCode(dto.getCareLevelCode())
                .status("ACTIVE")
                .depositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : BigDecimal.ZERO)
                .operatorId(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        admissionMapper.insert(admission);

        // 7. 更新床位状态为已占用 (OCCUPIED) 并自增版本号
        bed.setStatus("OCCUPIED");
        bed.setVersion(bed.getVersion() + 1);
        bed.setUpdatedAt(LocalDateTime.now());
        bedMapper.updateById(bed);

        // 8. 更新长者状态为在住 (IN_HOUSE)
        elderService.updateElderStatus(elder.getId(), "IN_HOUSE");

        // 9. 记录床位变更日志
        BedChangeLog changeLog = BedChangeLog.builder()
                .orgId(admission.getOrgId())
                .admissionId(admission.getId())
                .elderId(elder.getId())
                .fromBedId(null)
                .toBedId(bed.getId())
                .changeType("CHECK_IN")
                .changeReason(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : "办理入住登记")
                .changedBy(currentUserId)
                .changedAt(LocalDateTime.now())
                .build();
        bedChangeLogMapper.insert(changeLog);

        // 10. 记录系统操作审计日志
        sysOperationLogService.recordLog(
                "ADMISSION",
                "CHECK_IN",
                "ADMISSION",
                admission.getId(),
                "/api/v1/admissions/check-in",
                "127.0.0.1",
                String.format("办理长者入住: 长者=%s, 床位=%s, 单号=%s", elder.getName(), bed.getBedNo(), admissionNo)
        );

        log.info("长者入住办理成功: admissionId={}, admissionNo={}", admission.getId(), admissionNo);
        return admission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferBed(Long admissionId, AdmissionTransferDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        log.info("开始办理调床: orgId={}, admissionId={}, newBedId={}", currentOrgId, admissionId, dto.getNewBedId());

        // 1. 校验入住记录
        Admission admission = admissionMapper.selectById(admissionId);
        if (admission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入住记录不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(admission.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的入住记录");
        }
        if (!"ACTIVE".equals(admission.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "非在住状态的入住记录无法办理调床");
        }

        Long oldBedId = admission.getBedId();
        Long newBedId = dto.getNewBedId();
        if (oldBedId.equals(newBedId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "目标床位不能与当前床位相同");
        }

        // 2. 升序加锁以彻底消除并发死锁风险 (Deadlock Prevention by Consistent Global Lock Ordering)
        Long firstLockId = Math.min(oldBedId, newBedId);
        Long secondLockId = Math.max(oldBedId, newBedId);

        Bed firstBed = bedMapper.selectByIdForUpdate(firstLockId);
        Bed secondBed = bedMapper.selectByIdForUpdate(secondLockId);

        Bed oldBed = oldBedId.equals(firstLockId) ? firstBed : secondBed;
        Bed newBed = newBedId.equals(firstLockId) ? firstBed : secondBed;

        if (oldBed == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "原床位不存在");
        }
        if (newBed == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "目标新床位不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(newBed.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权调至其他机构的床位");
        }
        if (!"AVAILABLE".equals(newBed.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "目标新床位已被占用或不可用（状态: " + newBed.getStatus() + "）");
        }

        // 3. 原子更新旧床位与新床位状态
        oldBed.setStatus("AVAILABLE");
        oldBed.setVersion(oldBed.getVersion() + 1);
        oldBed.setUpdatedAt(LocalDateTime.now());
        bedMapper.updateById(oldBed);

        newBed.setStatus("OCCUPIED");
        newBed.setVersion(newBed.getVersion() + 1);
        newBed.setUpdatedAt(LocalDateTime.now());
        bedMapper.updateById(newBed);

        // 4. 更新入住记录关联的床位
        admission.setBedId(newBed.getId());
        admission.setUpdatedAt(LocalDateTime.now());
        admissionMapper.updateById(admission);

        // 5. 写入床位变动日志
        BedChangeLog changeLog = BedChangeLog.builder()
                .orgId(admission.getOrgId())
                .admissionId(admission.getId())
                .elderId(admission.getElderId())
                .fromBedId(oldBed.getId())
                .toBedId(newBed.getId())
                .changeType("TRANSFER")
                .changeReason(StringUtils.hasText(dto.getReason()) ? dto.getReason() : "长者房间/床位调整")
                .changedBy(currentUserId)
                .changedAt(LocalDateTime.now())
                .build();
        bedChangeLogMapper.insert(changeLog);

        // 6. 审计日志
        sysOperationLogService.recordLog(
                "ADMISSION",
                "TRANSFER",
                "ADMISSION",
                admission.getId(),
                "/api/v1/admissions/" + admissionId + "/transfer",
                "127.0.0.1",
                String.format("办理调床: 原床位=%s, 新床位=%s, 原因=%s", oldBed.getBedNo(), newBed.getBedNo(), dto.getReason())
        );

        log.info("调床办理成功: admissionId={}, fromBedId={}, toBedId={}", admissionId, oldBedId, newBedId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void discharge(Long admissionId, AdmissionDischargeDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        log.info("开始办理退住: orgId={}, admissionId={}, date={}", currentOrgId, admissionId, dto.getActualDischargeDate());

        // 1. 校验入住记录
        Admission admission = admissionMapper.selectById(admissionId);
        if (admission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入住记录不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(admission.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的入住记录");
        }
        if (!"ACTIVE".equals(admission.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "非在住状态的入住记录无法办理退住");
        }

        // 2. 校验退住日期不得早于入住日期
        if (dto.getActualDischargeDate().isBefore(admission.getAdmissionDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际退住日期不得早于入住日期 (" + admission.getAdmissionDate() + ")");
        }

        // 3. 执行退住前置检查
        AdmissionPrecheckVO precheck = checkDischargePreconditions(admissionId);
        if (!precheck.getCanDischarge() && !Boolean.TRUE.equals(dto.getForceDischarge())) {
            String reasons = String.join("; ", precheck.getBlockingReasons());
            throw new BusinessException(ErrorCode.CONFLICT, "退住前置检查未通过: " + reasons + "（如需强制退住请确认授权）");
        }

        // 4. 行级排他锁锁定床位
        Bed bed = bedMapper.selectByIdForUpdate(admission.getBedId());
        if (bed != null) {
            bed.setStatus("AVAILABLE");
            bed.setVersion(bed.getVersion() + 1);
            bed.setUpdatedAt(LocalDateTime.now());
            bedMapper.updateById(bed);
        }

        // 5. 更新入住记录状态为已退住 (DISCHARGED)
        admission.setStatus("DISCHARGED");
        admission.setActualDischargeDate(dto.getActualDischargeDate());
        admission.setDischargeReason(dto.getDischargeReason());
        admission.setUpdatedAt(LocalDateTime.now());
        admissionMapper.updateById(admission);

        // 6. 更新长者状态为已退住 (DISCHARGED)
        elderService.updateElderStatus(admission.getElderId(), "DISCHARGED");

        // 7. 写入床位变动日志
        BedChangeLog changeLog = BedChangeLog.builder()
                .orgId(admission.getOrgId())
                .admissionId(admission.getId())
                .elderId(admission.getElderId())
                .fromBedId(admission.getBedId())
                .toBedId(null)
                .changeType("DISCHARGE")
                .changeReason(StringUtils.hasText(dto.getDischargeReason()) ? dto.getDischargeReason() : "办理正常退住")
                .changedBy(currentUserId)
                .changedAt(LocalDateTime.now())
                .build();
        bedChangeLogMapper.insert(changeLog);

        // 8. 审计日志
        sysOperationLogService.recordLog(
                "ADMISSION",
                "DISCHARGE",
                "ADMISSION",
                admission.getId(),
                "/api/v1/admissions/" + admissionId + "/discharge",
                "127.0.0.1",
                String.format("办理退住: 入住单号=%s, 退住日期=%s, 原因=%s",
                        admission.getAdmissionNo(), dto.getActualDischargeDate(), dto.getDischargeReason())
        );

        log.info("退住办理完成: admissionId={}, admissionNo={}", admissionId, admission.getAdmissionNo());
    }

    @Override
    public AdmissionPrecheckVO checkDischargePreconditions(Long admissionId) {
        Admission admission = admissionMapper.selectById(admissionId);
        if (admission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入住记录不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(admission.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的入住记录");
        }

        List<String> warnings = new ArrayList<>();
        List<String> blockingReasons = new ArrayList<>();

        if (!"ACTIVE".equals(admission.getStatus())) {
            blockingReasons.add("该记录当前状态为 " + admission.getStatus() + "，不可重复退住");
        }

        // 提示押金结算
        if (admission.getDepositAmount() != null && admission.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
            warnings.add("长者当前登记有押金 ¥" + admission.getDepositAmount() + "，退住时请核对押金退款或财务结算单");
        }

        boolean canDischarge = blockingReasons.isEmpty();
        return AdmissionPrecheckVO.builder()
                .canDischarge(canDischarge)
                .warnings(warnings)
                .blockingReasons(blockingReasons)
                .build();
    }

    @Override
    public PageResult<AdmissionVO> pageAdmissions(AdmissionQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Admission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, Admission::getOrgId, currentOrgId);
        wrapper.eq(StringUtils.hasText(query.getAdmissionNo()), Admission::getAdmissionNo, query.getAdmissionNo());
        wrapper.eq(query.getElderId() != null, Admission::getElderId, query.getElderId());
        wrapper.eq(StringUtils.hasText(query.getStatus()), Admission::getStatus, query.getStatus());
        wrapper.ge(query.getStartDate() != null, Admission::getAdmissionDate, query.getStartDate());
        wrapper.le(query.getEndDate() != null, Admission::getAdmissionDate, query.getEndDate());

        // 如果按老人姓名或老人编号过滤
        if (StringUtils.hasText(query.getElderName()) || StringUtils.hasText(query.getElderNo())) {
            LambdaQueryWrapper<Elder> elderQuery = new LambdaQueryWrapper<Elder>()
                    .eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(StringUtils.hasText(query.getElderName()), Elder::getName, query.getElderName())
                    .eq(StringUtils.hasText(query.getElderNo()), Elder::getElderNo, query.getElderNo());
            List<Elder> matchedElders = elderMapper.selectList(elderQuery);
            if (matchedElders.isEmpty()) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(Admission::getElderId, elderIds);
        }

        // 如果按床位号或楼栋过滤
        if (StringUtils.hasText(query.getBedNo()) || query.getBuildingId() != null) {
            LambdaQueryWrapper<Bed> bedQuery = new LambdaQueryWrapper<Bed>()
                    .eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
                    .like(StringUtils.hasText(query.getBedNo()), Bed::getBedNo, query.getBedNo());
            if (query.getBuildingId() != null) {
                List<Room> rooms = roomMapper.selectList(
                        new LambdaQueryWrapper<Room>()
                                .eq(currentOrgId != null, Room::getOrgId, currentOrgId)
                                .eq(Room::getBuildingId, query.getBuildingId())
                );
                if (rooms.isEmpty()) {
                    return PageResult.empty(query.getPageNum(), query.getPageSize());
                }
                List<Long> roomIds = rooms.stream().map(Room::getId).collect(Collectors.toList());
                bedQuery.in(Bed::getRoomId, roomIds);
            }
            List<Bed> matchedBeds = bedMapper.selectList(bedQuery);
            if (matchedBeds.isEmpty()) {
                return PageResult.empty(query.getPageNum(), query.getPageSize());
            }
            List<Long> bedIds = matchedBeds.stream().map(Bed::getId).collect(Collectors.toList());
            wrapper.in(Admission::getBedId, bedIds);
        }

        wrapper.orderByDesc(Admission::getId);

        Page<Admission> page = admissionMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper
        );

        if (page.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }

        List<AdmissionVO> list = convertToVOList(page.getRecords());
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdmissionVO getAdmissionDetail(Long id) {
        Admission admission = admissionMapper.selectById(id);
        if (admission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入住记录不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(admission.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的入住记录");
        }

        return convertToVOList(Collections.singletonList(admission)).get(0);
    }

    @Override
    public List<BedChangeLogVO> listChangeLogs(Long admissionId) {
        Admission admission = admissionMapper.selectById(admissionId);
        if (admission == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "入住记录不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(admission.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的变动日志");
        }

        List<BedChangeLog> logs = bedChangeLogMapper.selectList(
                new LambdaQueryWrapper<BedChangeLog>()
                        .eq(BedChangeLog::getAdmissionId, admissionId)
                        .orderByDesc(BedChangeLog::getChangedAt)
        );

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        Elder elder = elderMapper.selectById(admission.getElderId());
        String elderName = elder != null ? elder.getName() : "--";

        List<Bed> beds = bedMapper.selectList(
                new LambdaQueryWrapper<Bed>().eq(currentOrgId != null, Bed::getOrgId, currentOrgId)
        );
        Map<Long, String> bedNoMap = beds.stream().collect(Collectors.toMap(Bed::getId, Bed::getBedNo));

        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(currentOrgId != null, SysUser::getOrgId, currentOrgId)
        );
        Map<Long, String> userNameMap = users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        return logs.stream().map(logItem -> BedChangeLogVO.builder()
                .id(logItem.getId())
                .admissionId(logItem.getAdmissionId())
                .elderId(logItem.getElderId())
                .elderName(elderName)
                .fromBedId(logItem.getFromBedId())
                .fromBedNo(logItem.getFromBedId() != null ? bedNoMap.getOrDefault(logItem.getFromBedId(), "--") : "--")
                .toBedId(logItem.getToBedId())
                .toBedNo(logItem.getToBedId() != null ? bedNoMap.getOrDefault(logItem.getToBedId(), "--") : "--")
                .changeType(logItem.getChangeType())
                .changeReason(logItem.getChangeReason())
                .changedByName(logItem.getChangedBy() != null ? userNameMap.getOrDefault(logItem.getChangedBy(), "系统操作员") : "系统")
                .changedAt(logItem.getChangedAt())
                .build()
        ).collect(Collectors.toList());
    }

    private List<AdmissionVO> convertToVOList(List<Admission> admissions) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        List<Long> elderIds = admissions.stream().map(Admission::getElderId).distinct().collect(Collectors.toList());
        Map<Long, Elder> elderMap = elderMapper.selectBatchIds(elderIds).stream()
                .collect(Collectors.toMap(Elder::getId, e -> e));

        List<Long> bedIds = admissions.stream().map(Admission::getBedId).distinct().collect(Collectors.toList());
        Map<Long, Bed> bedMap = bedMapper.selectBatchIds(bedIds).stream()
                .collect(Collectors.toMap(Bed::getId, b -> b));

        List<Room> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<Room>().eq(currentOrgId != null, Room::getOrgId, currentOrgId)
        );
        Map<Long, Room> roomMap = rooms.stream().collect(Collectors.toMap(Room::getId, r -> r));

        List<Building> buildings = buildingMapper.selectList(
                new LambdaQueryWrapper<Building>().eq(currentOrgId != null, Building::getOrgId, currentOrgId)
        );
        Map<Long, Building> buildingMap = buildings.stream().collect(Collectors.toMap(Building::getId, b -> b));

        List<SysUser> users = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(currentOrgId != null, SysUser::getOrgId, currentOrgId)
        );
        Map<Long, String> userNameMap = users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName));

        return admissions.stream().map(adm -> {
            Elder elder = elderMap.get(adm.getElderId());
            Bed bed = bedMap.get(adm.getBedId());
            Room room = (bed != null) ? roomMap.get(bed.getRoomId()) : null;
            Building bld = (room != null) ? buildingMap.get(room.getBuildingId()) : null;

            Integer age = null;
            if (elder != null && elder.getBirthDate() != null) {
                age = Period.between(elder.getBirthDate(), LocalDate.now()).getYears();
            }

            return AdmissionVO.builder()
                    .id(adm.getId())
                    .orgId(adm.getOrgId())
                    .admissionNo(adm.getAdmissionNo())
                    .elderId(adm.getElderId())
                    .elderName(elder != null ? elder.getName() : "--")
                    .elderNo(elder != null ? elder.getElderNo() : "--")
                    .elderGender(elder != null ? elder.getGender() : "--")
                    .elderAge(age)
                    .elderPhone(elder != null ? DesensitizeUtil.maskPhone(elder.getPhone()) : "--")
                    .bedId(adm.getBedId())
                    .bedNo(bed != null ? bed.getBedNo() : "--")
                    .roomId(room != null ? room.getId() : null)
                    .roomNo(room != null ? room.getRoomNo() : "--")
                    .buildingId(bld != null ? bld.getId() : null)
                    .buildingName(bld != null ? bld.getBuildingName() : "--")
                    .contractNo(adm.getContractNo())
                    .admissionDate(adm.getAdmissionDate())
                    .plannedDischargeDate(adm.getPlannedDischargeDate())
                    .actualDischargeDate(adm.getActualDischargeDate())
                    .careLevelCode(adm.getCareLevelCode())
                    .status(adm.getStatus())
                    .depositAmount(adm.getDepositAmount())
                    .dischargeReason(adm.getDischargeReason())
                    .operatorName(adm.getOperatorId() != null ? userNameMap.getOrDefault(adm.getOperatorId(), "系统管理员") : "系统")
                    .createdAt(adm.getCreatedAt())
                    .updatedAt(adm.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String generateAdmissionNo() {
        // 格式：ADM + 年月日 + 4位序号
        String datePart = LocalDate.now().toString().replace("-", "");
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ADM" + datePart + randomPart;
    }
}
