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
import com.example.elderlycare.modules.care.dto.CarePlanCreateDTO;
import com.example.elderlycare.modules.care.dto.CarePlanItemDTO;
import com.example.elderlycare.modules.care.dto.CarePlanQueryDTO;
import com.example.elderlycare.modules.care.entity.CareItem;
import com.example.elderlycare.modules.care.entity.CareLevel;
import com.example.elderlycare.modules.care.entity.CarePlan;
import com.example.elderlycare.modules.care.entity.CarePlanItem;
import com.example.elderlycare.modules.care.mapper.CareItemMapper;
import com.example.elderlycare.modules.care.mapper.CareLevelMapper;
import com.example.elderlycare.modules.care.mapper.CarePlanItemMapper;
import com.example.elderlycare.modules.care.mapper.CarePlanMapper;
import com.example.elderlycare.modules.care.service.CarePlanService;
import com.example.elderlycare.modules.care.vo.CarePlanDetailVO;
import com.example.elderlycare.modules.care.vo.CarePlanItemVO;
import com.example.elderlycare.modules.care.vo.CarePlanVO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarePlanServiceImpl implements CarePlanService {

    private final CarePlanMapper carePlanMapper;
    private final CarePlanItemMapper carePlanItemMapper;
    private final CareLevelMapper careLevelMapper;
    private final CareItemMapper careItemMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public PageResult<CarePlanVO> pagePlans(CarePlanQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<CarePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, CarePlan::getOrgId, currentOrgId);

        if (StringUtils.hasText(query.getPlanNo())) {
            wrapper.like(CarePlan::getPlanNo, query.getPlanNo().trim());
        }
        if (query.getElderId() != null) {
            wrapper.eq(CarePlan::getElderId, query.getElderId());
        } else if (StringUtils.hasText(query.getElderName())) {
            LambdaQueryWrapper<Elder> elderWrapper = new LambdaQueryWrapper<>();
            elderWrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(Elder::getName, query.getElderName().trim());
            List<Elder> matchedElders = elderMapper.selectList(elderWrapper);
            if (matchedElders.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(CarePlan::getElderId, elderIds);
        }

        if (query.getCareLevelId() != null) {
            wrapper.eq(CarePlan::getCareLevelId, query.getCareLevelId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(CarePlan::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(CarePlan::getId);

        Page<CarePlan> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<CarePlan> pageResult = carePlanMapper.selectPage(pageParam, wrapper);

        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return PageResult.of(Collections.emptyList(), pageResult.getTotal(), query.getPageNum(), query.getPageSize());
        }

        List<CarePlanVO> vos = enrichPlans(pageResult.getRecords());
        return PageResult.of(vos, pageResult.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public CarePlanDetailVO getPlanDetail(Long id) {
        CarePlan plan = checkOrgAccess(id);
        List<CarePlanVO> planVOs = enrichPlans(Collections.singletonList(plan));
        CarePlanVO planVO = planVOs.isEmpty() ? null : planVOs.get(0);

        List<CarePlanItem> items = carePlanItemMapper.selectList(
                new LambdaQueryWrapper<CarePlanItem>()
                        .eq(CarePlanItem::getPlanId, id)
                        .orderByAsc(CarePlanItem::getId)
        );

        List<CarePlanItemVO> itemVOs = enrichPlanItems(items);

        return CarePlanDetailVO.builder()
                .plan(planVO)
                .items(itemVOs)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(CarePlanCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        Elder elder = elderMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务长者不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权为其他机构长者创建护理计划");
        }

        CareLevel careLevel = careLevelMapper.selectById(dto.getCareLevelId());
        if (careLevel == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理等级不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(careLevel.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权使用其他机构的护理等级");
        }

        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();
        String planNo = generatePlanNo();

        CarePlan plan = CarePlan.builder()
                .orgId(orgId)
                .planNo(planNo)
                .elderId(dto.getElderId())
                .careLevelId(dto.getCareLevelId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status("ACTIVE")
                .creatorId(currentUserId != null ? currentUserId : 1L)
                .remark(dto.getRemark())
                .createdAt(LocalDateTime.now())
                .build();

        carePlanMapper.insert(plan);

        // 插入计划明细项目
        for (CarePlanItemDTO itemDTO : dto.getItems()) {
            CareItem item = careItemMapper.selectById(itemDTO.getCareItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理项目 ID=" + itemDTO.getCareItemId() + " 不存在");
            }

            CarePlanItem planItem = CarePlanItem.builder()
                    .orgId(orgId)
                    .planId(plan.getId())
                    .careItemId(itemDTO.getCareItemId())
                    .frequencyType(StringUtils.hasText(itemDTO.getFrequencyType()) ? itemDTO.getFrequencyType() : "DAILY")
                    .frequencyValue(StringUtils.hasText(itemDTO.getFrequencyValue()) ? itemDTO.getFrequencyValue() : "1")
                    .timeWindowStart(itemDTO.getTimeWindowStart())
                    .timeWindowEnd(itemDTO.getTimeWindowEnd())
                    .assigneeId(itemDTO.getAssigneeId())
                    .enabled(itemDTO.getEnabled() != null ? itemDTO.getEnabled() : 1)
                    .createdAt(LocalDateTime.now())
                    .build();

            carePlanItemMapper.insert(planItem);
        }

        sysOperationLogService.recordLog(
                "CARE_PLAN",
                "CREATE",
                "CARE_PLAN",
                plan.getId(),
                "/api/v1/care/plans",
                "127.0.0.1",
                String.format("创建护理计划: 单号=%s, 长者=%s, 等级=%s, 明细项=%d",
                        planNo, elder.getName(), careLevel.getLevelName(), dto.getItems().size())
        );

        return plan.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlan(Long id, CarePlanCreateDTO dto) {
        CarePlan plan = carePlanMapper.selectByIdForUpdate(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理计划不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(plan.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权修改其他机构的护理计划");
        }

        if (dto.getCareLevelId() != null) {
            CareLevel careLevel = careLevelMapper.selectById(dto.getCareLevelId());
            if (careLevel == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理等级不存在");
            }
            plan.setCareLevelId(dto.getCareLevelId());
        }

        if (dto.getStartDate() != null) {
            plan.setStartDate(dto.getStartDate());
        }
        plan.setEndDate(dto.getEndDate());
        plan.setRemark(dto.getRemark());

        carePlanMapper.updateById(plan);

        // 重置/同步计划明细
        carePlanItemMapper.delete(new LambdaQueryWrapper<CarePlanItem>().eq(CarePlanItem::getPlanId, id));

        for (CarePlanItemDTO itemDTO : dto.getItems()) {
            CarePlanItem planItem = CarePlanItem.builder()
                    .orgId(plan.getOrgId())
                    .planId(plan.getId())
                    .careItemId(itemDTO.getCareItemId())
                    .frequencyType(StringUtils.hasText(itemDTO.getFrequencyType()) ? itemDTO.getFrequencyType() : "DAILY")
                    .frequencyValue(StringUtils.hasText(itemDTO.getFrequencyValue()) ? itemDTO.getFrequencyValue() : "1")
                    .timeWindowStart(itemDTO.getTimeWindowStart())
                    .timeWindowEnd(itemDTO.getTimeWindowEnd())
                    .assigneeId(itemDTO.getAssigneeId())
                    .enabled(itemDTO.getEnabled() != null ? itemDTO.getEnabled() : 1)
                    .createdAt(LocalDateTime.now())
                    .build();
            carePlanItemMapper.insert(planItem);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void togglePlanStatus(Long id, String status) {
        CarePlan plan = checkOrgAccess(id);
        plan.setStatus(status);
        carePlanMapper.updateById(plan);
    }

    private CarePlan checkOrgAccess(Long id) {
        CarePlan plan = carePlanMapper.selectById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "护理计划不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(plan.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的护理计划");
        }
        return plan;
    }

    private List<CarePlanVO> enrichPlans(List<CarePlan> plans) {
        if (CollectionUtils.isEmpty(plans)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = plans.stream().map(CarePlan::getElderId).collect(Collectors.toSet());
        Set<Long> levelIds = plans.stream().map(CarePlan::getCareLevelId).collect(Collectors.toSet());
        Set<Long> creatorIds = plans.stream().map(CarePlan::getCreatorId).collect(Collectors.toSet());
        Set<Long> planIds = plans.stream().map(CarePlan::getId).collect(Collectors.toSet());

        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, CareLevel> levelMap = levelIds.isEmpty() ? Collections.emptyMap() :
                careLevelMapper.selectBatchIds(levelIds).stream().collect(Collectors.toMap(CareLevel::getId, l -> l));

        Map<Long, SysUser> userMap = creatorIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(creatorIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        // 统计各计划明细数
        Map<Long, Long> itemCountMap = new HashMap<>();
        for (Long pid : planIds) {
            Long count = carePlanItemMapper.selectCount(new LambdaQueryWrapper<CarePlanItem>().eq(CarePlanItem::getPlanId, pid));
            itemCountMap.put(pid, count);
        }

        Map<Long, String> bedInfoMap = buildElderBedInfoMap(elderIds);

        return plans.stream().map(p -> {
            Elder elder = elderMap.get(p.getElderId());
            CareLevel level = levelMap.get(p.getCareLevelId());
            SysUser creator = userMap.get(p.getCreatorId());

            return CarePlanVO.builder()
                    .id(p.getId())
                    .orgId(p.getOrgId())
                    .planNo(p.getPlanNo())
                    .elderId(p.getElderId())
                    .elderName(elder != null ? elder.getName() : null)
                    .elderNo(elder != null ? elder.getElderNo() : null)
                    .bedInfo(elder != null ? bedInfoMap.get(elder.getId()) : null)
                    .careLevelId(p.getCareLevelId())
                    .careLevelName(level != null ? level.getLevelName() : null)
                    .careLevelCode(level != null ? level.getLevelCode() : null)
                    .startDate(p.getStartDate())
                    .endDate(p.getEndDate())
                    .status(p.getStatus())
                    .creatorId(p.getCreatorId())
                    .creatorName(creator != null ? creator.getRealName() : "系统自动")
                    .remark(p.getRemark())
                    .itemCount(itemCountMap.getOrDefault(p.getId(), 0L).intValue())
                    .createdAt(p.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private List<CarePlanItemVO> enrichPlanItems(List<CarePlanItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Collections.emptyList();
        }

        Set<Long> careItemIds = items.stream().map(CarePlanItem::getCareItemId).collect(Collectors.toSet());
        Set<Long> assigneeIds = items.stream().map(CarePlanItem::getAssigneeId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, CareItem> careItemMap = careItemIds.isEmpty() ? Collections.emptyMap() :
                careItemMapper.selectBatchIds(careItemIds).stream().collect(Collectors.toMap(CareItem::getId, i -> i));

        Map<Long, SysUser> userMap = assigneeIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(assigneeIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return items.stream().map(i -> {
            CareItem item = careItemMap.get(i.getCareItemId());
            SysUser assignee = i.getAssigneeId() != null ? userMap.get(i.getAssigneeId()) : null;

            return CarePlanItemVO.builder()
                    .id(i.getId())
                    .planId(i.getPlanId())
                    .careItemId(i.getCareItemId())
                    .itemCode(item != null ? item.getItemCode() : null)
                    .itemName(item != null ? item.getItemName() : null)
                    .category(item != null ? item.getCategory() : null)
                    .frequencyType(i.getFrequencyType())
                    .frequencyValue(i.getFrequencyValue())
                    .timeWindowStart(i.getTimeWindowStart())
                    .timeWindowEnd(i.getTimeWindowEnd())
                    .assigneeId(i.getAssigneeId())
                    .assigneeName(assignee != null ? assignee.getRealName() : null)
                    .enabled(i.getEnabled())
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

    private String generatePlanNo() {
        return "CP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
