package com.example.elderlycare.modules.serviceorder.service.impl;

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
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderActionDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderAssignDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderCreateDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderQueryDTO;
import com.example.elderlycare.modules.serviceorder.entity.ServiceItem;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrderLog;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceItemMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderLogMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import com.example.elderlycare.modules.serviceorder.service.ServiceOrderService;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderAction;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderStatus;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderStateMachine;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderLogVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderSummaryVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderVO;
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
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderMapper serviceOrderMapper;
    private final ServiceOrderLogMapper serviceOrderLogMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public PageResult<ServiceOrderVO> pageOrders(ServiceOrderQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, ServiceOrder::getOrgId, currentOrgId);

        if (StringUtils.hasText(query.getOrderNo())) {
            wrapper.like(ServiceOrder::getOrderNo, query.getOrderNo().trim());
        }
        if (query.getElderId() != null) {
            wrapper.eq(ServiceOrder::getElderId, query.getElderId());
        } else if (StringUtils.hasText(query.getElderName())) {
            LambdaQueryWrapper<Elder> elderWrapper = new LambdaQueryWrapper<>();
            elderWrapper.eq(currentOrgId != null, Elder::getOrgId, currentOrgId)
                    .like(Elder::getName, query.getElderName().trim());
            List<Elder> matchedElders = elderMapper.selectList(elderWrapper);
            if (matchedElders.isEmpty()) {
                return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
            }
            List<Long> elderIds = matchedElders.stream().map(Elder::getId).collect(Collectors.toList());
            wrapper.in(ServiceOrder::getElderId, elderIds);
        }

        if (query.getServiceItemId() != null) {
            wrapper.eq(ServiceOrder::getServiceItemId, query.getServiceItemId());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(ServiceOrder::getAssigneeId, query.getAssigneeId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(ServiceOrder::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getPriority())) {
            wrapper.eq(ServiceOrder::getPriority, query.getPriority());
        }
        if (query.getStartDate() != null) {
            wrapper.ge(ServiceOrder::getExpectedStartAt, query.getStartDate().atStartOfDay());
        }
        if (query.getEndDate() != null) {
            wrapper.le(ServiceOrder::getExpectedStartAt, query.getEndDate().atTime(23, 59, 59));
        }

        wrapper.orderByDesc(ServiceOrder::getId);

        Page<ServiceOrder> pageParam = new Page<>(query.getPageNum(), query.getPageSize());
        Page<ServiceOrder> pageResult = serviceOrderMapper.selectPage(pageParam, wrapper);

        if (CollectionUtils.isEmpty(pageResult.getRecords())) {
            return PageResult.of(Collections.emptyList(), pageResult.getTotal(), query.getPageNum(), query.getPageSize());
        }

        List<ServiceOrderVO> voList = enrichOrders(pageResult.getRecords());
        return PageResult.of(voList, pageResult.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public ServiceOrderVO getOrderDetail(Long id) {
        ServiceOrder order = checkOrgAccess(id);
        List<ServiceOrderVO> vos = enrichOrders(Collections.singletonList(order));
        return vos.isEmpty() ? null : vos.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(ServiceOrderCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();

        Elder elder = elderMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联的长者不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权为其他机构的长者创建工单");
        }
        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();

        ServiceItem item = serviceItemMapper.selectById(dto.getServiceItemId());
        if (item == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务项目不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(item.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权使用其他机构的服务项目");
        }
        if (!"ACTIVE".equalsIgnoreCase(item.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该服务项目未启用，无法创建工单");
        }

        Long admissionId = dto.getAdmissionId();
        if (admissionId == null) {
            Admission activeAdmission = admissionMapper.selectOne(
                    new LambdaQueryWrapper<Admission>()
                            .eq(Admission::getElderId, elder.getId())
                            .eq(Admission::getStatus, "CHECKED_IN")
                            .last("LIMIT 1")
            );
            if (activeAdmission != null) {
                admissionId = activeAdmission.getId();
            }
        }

        // 校验指派人员
        if (dto.getAssigneeId() != null) {
            SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
            if (assignee == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指定的护理人员不存在");
            }
            if (currentOrgId != null && assignee.getOrgId() != null && !currentOrgId.equals(assignee.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员");
            }
        }

        String orderNo = generateOrderNo();
        ServiceOrderStatus initialStatus = dto.getAssigneeId() != null
                ? ServiceOrderStatus.ASSIGNED
                : ServiceOrderStatus.PENDING_ASSIGN;

        ServiceOrder order = ServiceOrder.builder()
                .orgId(orgId)
                .orderNo(orderNo)
                .elderId(dto.getElderId())
                .admissionId(admissionId)
                .serviceItemId(dto.getServiceItemId())
                .sourceType(StringUtils.hasText(dto.getSourceType()) ? dto.getSourceType() : "MANUAL")
                .priority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "NORMAL")
                .expectedStartAt(dto.getExpectedStartAt() != null ? dto.getExpectedStartAt() : LocalDateTime.now())
                .assigneeId(dto.getAssigneeId())
                .creatorId(currentUserId)
                .status(initialStatus.name())
                .requestDesc(dto.getRequestDesc())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        serviceOrderMapper.insert(order);

        // 记录初始工单日志
        ServiceOrderLog logRecord = ServiceOrderLog.builder()
                .orgId(orgId)
                .orderId(order.getId())
                .fromStatus(null)
                .toStatus(initialStatus.name())
                .action(ServiceOrderAction.CREATE.name())
                .operatorId(currentUserId)
                .remark(dto.getAssigneeId() != null ? "创建工单并指派负责人" : "创建工单（待派单）")
                .createdAt(LocalDateTime.now())
                .build();
        serviceOrderLogMapper.insert(logRecord);

        // 记录系统审计日志
        sysOperationLogService.recordLog(
                "SERVICE_ORDER",
                "CREATE",
                "SERVICE_ORDER",
                order.getId(),
                "/api/v1/service-orders",
                "127.0.0.1",
                String.format("创建工单: 单号=%s, 长者=%s, 项目=%s, 初始状态=%s",
                        orderNo, elder.getName(), item.getItemName(), initialStatus.name())
        );

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrder(Long id, ServiceOrderCreateDTO dto) {
        ServiceOrder order = serviceOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务工单不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(order.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权修改其他机构的工单");
        }

        // PRD 约束：已完成或已取消工单不可直接编辑核心内容
        if (ServiceOrderStatus.COMPLETED.name().equals(order.getStatus()) || ServiceOrderStatus.CANCELLED.name().equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已完成或已取消的工单不可修改核心内容");
        }

        if (dto.getServiceItemId() != null) {
            ServiceItem item = serviceItemMapper.selectById(dto.getServiceItemId());
            if (item == null) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务项目不存在");
            }
            order.setServiceItemId(dto.getServiceItemId());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            order.setPriority(dto.getPriority());
        }
        if (dto.getExpectedStartAt() != null) {
            order.setExpectedStartAt(dto.getExpectedStartAt());
        }
        if (dto.getRequestDesc() != null) {
            order.setRequestDesc(dto.getRequestDesc());
        }
        order.setUpdatedAt(LocalDateTime.now());

        serviceOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOrder(Long id, ServiceOrderAssignDTO dto) {
        ServiceOrder order = serviceOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务工单不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();
        if (currentOrgId != null && !currentOrgId.equals(order.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的工单");
        }

        // 校验当前状态是否支持 ASSIGN 动作
        ServiceOrderStatus currentStatus = ServiceOrderStatus.valueOf(order.getStatus());
        ServiceOrderStatus nextStatus = ServiceOrderStateMachine.getNextStatus(currentStatus, ServiceOrderAction.ASSIGN, false);

        SysUser assignee = sysUserMapper.selectById(dto.getAssigneeId());
        if (assignee == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "指派的护理人员不存在");
        }
        if (currentOrgId != null && assignee.getOrgId() != null && !currentOrgId.equals(assignee.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可指派其他机构人员");
        }

        order.setAssigneeId(dto.getAssigneeId());
        order.setStatus(nextStatus.name());
        order.setUpdatedAt(LocalDateTime.now());
        serviceOrderMapper.updateById(order);

        // 写入工单流转记录
        ServiceOrderLog logRecord = ServiceOrderLog.builder()
                .orgId(order.getOrgId())
                .orderId(order.getId())
                .fromStatus(currentStatus.name())
                .toStatus(nextStatus.name())
                .action(ServiceOrderAction.ASSIGN.name())
                .operatorId(currentUserId)
                .remark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark() : "指派护理人员: " + assignee.getRealName())
                .createdAt(LocalDateTime.now())
                .build();
        serviceOrderLogMapper.insert(logRecord);

        // 审计日志
        sysOperationLogService.recordLog(
                "SERVICE_ORDER",
                "ASSIGN",
                "SERVICE_ORDER",
                order.getId(),
                "/api/v1/service-orders/" + id + "/assign",
                "127.0.0.1",
                String.format("指派工单 [%s] 给护理员 [%s]", order.getOrderNo(), assignee.getRealName())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeAction(Long id, ServiceOrderActionDTO dto) {
        ServiceOrder order = serviceOrderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务工单不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId();
        if (currentOrgId != null && !currentOrgId.equals(order.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构的工单");
        }

        ServiceOrderStatus currentStatus = ServiceOrderStatus.valueOf(order.getStatus());

        // 检查服务项目配置是否需要确认验收
        ServiceItem item = serviceItemMapper.selectById(order.getServiceItemId());
        boolean needConfirm = item != null && item.getNeedConfirm() != null && item.getNeedConfirm() == 1;

        // 状态机校验流转合法性
        ServiceOrderStatus nextStatus = ServiceOrderStateMachine.getNextStatus(currentStatus, dto.getAction(), needConfirm);

        // 操作人权限与角色校验
        checkOperatorPermission(order, dto.getAction(), currentUserId);

        // 如果是完成操作，更新执行结果
        if (dto.getAction() == ServiceOrderAction.COMPLETE) {
            if (StringUtils.hasText(dto.getExecutionResult())) {
                order.setExecutionResult(dto.getExecutionResult());
            }
            if (nextStatus == ServiceOrderStatus.COMPLETED) {
                order.setCompletedAt(LocalDateTime.now());
            }
        } else if (dto.getAction() == ServiceOrderAction.CONFIRM) {
            // 验收通过，工单最终完成
            order.setCompletedAt(LocalDateTime.now());
        }

        order.setStatus(nextStatus.name());
        order.setUpdatedAt(LocalDateTime.now());
        serviceOrderMapper.updateById(order);

        // 写入工单流转记录
        ServiceOrderLog logRecord = ServiceOrderLog.builder()
                .orgId(order.getOrgId())
                .orderId(order.getId())
                .fromStatus(currentStatus.name())
                .toStatus(nextStatus.name())
                .action(dto.getAction().name())
                .operatorId(currentUserId)
                .remark(dto.getRemark())
                .createdAt(LocalDateTime.now())
                .build();
        serviceOrderLogMapper.insert(logRecord);

        // 系统审计日志
        sysOperationLogService.recordLog(
                "SERVICE_ORDER",
                dto.getAction().name(),
                "SERVICE_ORDER",
                order.getId(),
                "/api/v1/service-orders/" + id + "/action",
                "127.0.0.1",
                String.format("工单 [%s] 执行操作 [%s]，状态流转：%s -> %s",
                        order.getOrderNo(), dto.getAction().getDescription(), currentStatus.name(), nextStatus.name())
        );
    }

    @Override
    public List<ServiceOrderLogVO> listLogs(Long orderId) {
        ServiceOrder order = checkOrgAccess(orderId);

        List<ServiceOrderLog> logs = serviceOrderLogMapper.selectList(
                new LambdaQueryWrapper<ServiceOrderLog>()
                        .eq(ServiceOrderLog::getOrderId, orderId)
                        .orderByAsc(ServiceOrderLog::getId)
        );

        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }

        Set<Long> userIds = logs.stream()
                .map(ServiceOrderLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return logs.stream().map(l -> ServiceOrderLogVO.builder()
                .id(l.getId())
                .orderId(l.getOrderId())
                .fromStatus(l.getFromStatus())
                .toStatus(l.getToStatus())
                .action(l.getAction())
                .operatorId(l.getOperatorId())
                .operatorName(l.getOperatorId() != null && userMap.containsKey(l.getOperatorId())
                        ? userMap.get(l.getOperatorId()).getRealName() : "系统自动")
                .remark(l.getRemark())
                .createdAt(l.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public ServiceOrderSummaryVO getOrderSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<ServiceOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(currentOrgId != null, ServiceOrder::getOrgId, currentOrgId);
        List<ServiceOrder> orders = serviceOrderMapper.selectList(wrapper);

        int total = orders.size();
        int pendingAssign = 0;
        int assigned = 0;
        int inProgress = 0;
        int pendingConfirm = 0;
        int completed = 0;

        for (ServiceOrder o : orders) {
            String s = o.getStatus();
            if (ServiceOrderStatus.PENDING_ASSIGN.name().equals(s)) {
                pendingAssign++;
            } else if (ServiceOrderStatus.ASSIGNED.name().equals(s)) {
                assigned++;
            } else if (ServiceOrderStatus.ACCEPTED.name().equals(s) || ServiceOrderStatus.IN_PROGRESS.name().equals(s)) {
                inProgress++;
            } else if (ServiceOrderStatus.PENDING_CONFIRM.name().equals(s)) {
                pendingConfirm++;
            } else if (ServiceOrderStatus.COMPLETED.name().equals(s)) {
                completed++;
            }
        }

        return ServiceOrderSummaryVO.builder()
                .totalOrders(total)
                .pendingAssignCount(pendingAssign)
                .assignedCount(assigned)
                .inProgressCount(inProgress)
                .pendingConfirmCount(pendingConfirm)
                .completedCount(completed)
                .build();
    }

    private void checkOperatorPermission(ServiceOrder order, ServiceOrderAction action, Long currentUserId) {
        if (currentUserId == null) {
            return;
        }
        List<String> roles = sysUserMapper.selectRoleCodesByUserId(currentUserId);
        if (roles == null) {
            roles = Collections.emptyList();
        }
        boolean isAdmin = roles.contains("ADMIN") || roles.contains("SUPER_ADMIN") || "admin".equalsIgnoreCase(OrgContextHolder.getUsername());
        boolean isDirector = isAdmin || roles.contains("DIRECTOR") || roles.contains("CARE_DIRECTOR");

        switch (action) {
            case ACCEPT:
            case START:
            case COMPLETE:
                if (!isAdmin && (order.getAssigneeId() == null || !order.getAssigneeId().equals(currentUserId))) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "只有被指派人或管理员才能执行接单/执行/完成操作");
                }
                break;
            case CONFIRM:
                if (currentUserId.equals(order.getAssigneeId()) && !isDirector && !isAdmin) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "服务执行人不能自行验收该工单");
                }
                break;
            case REJECT:
                if (!isAdmin && (order.getAssigneeId() == null || !order.getAssigneeId().equals(currentUserId))) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "只有被指派人或管理员才能拒单");
                }
                break;
            case RETURN:
                if (ServiceOrderStatus.PENDING_CONFIRM.name().equals(order.getStatus())) {
                    if (currentUserId.equals(order.getAssigneeId()) && !isDirector && !isAdmin) {
                        throw new BusinessException(ErrorCode.FORBIDDEN, "验收不通过退回应由主管或创建人操作");
                    }
                }
                break;
            case CANCEL:
                if (!isAdmin && !isDirector && (order.getCreatorId() != null && !order.getCreatorId().equals(currentUserId))) {
                    throw new BusinessException(ErrorCode.FORBIDDEN, "只有创建人、主管或管理员可以取消工单");
                }
                break;
            default:
                break;
        }
    }

    private ServiceOrder checkOrgAccess(Long id) {
        ServiceOrder order = serviceOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "服务工单不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(order.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构的服务工单");
        }
        return order;
    }

    private List<ServiceOrderVO> enrichOrders(List<ServiceOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = orders.stream().map(ServiceOrder::getElderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> itemIds = orders.stream().map(ServiceOrder::getServiceItemId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> admissionIds = orders.stream().map(ServiceOrder::getAdmissionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = new HashSet<>();
        orders.forEach(o -> {
            if (o.getAssigneeId() != null) userIds.add(o.getAssigneeId());
            if (o.getCreatorId() != null) userIds.add(o.getCreatorId());
        });

        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, ServiceItem> itemMap = itemIds.isEmpty() ? Collections.emptyMap() :
                serviceItemMapper.selectBatchIds(itemIds).stream().collect(Collectors.toMap(ServiceItem::getId, i -> i));

        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Map<Long, String> bedInfoMap = buildBedInfoMap(admissionIds);

        return orders.stream().map(order -> {
            Elder elder = elderMap.get(order.getElderId());
            ServiceItem item = itemMap.get(order.getServiceItemId());
            SysUser assignee = order.getAssigneeId() != null ? userMap.get(order.getAssigneeId()) : null;
            SysUser creator = order.getCreatorId() != null ? userMap.get(order.getCreatorId()) : null;

            return ServiceOrderVO.builder()
                    .id(order.getId())
                    .orgId(order.getOrgId())
                    .orderNo(order.getOrderNo())
                    .elderId(order.getElderId())
                    .elderName(elder != null ? elder.getName() : null)
                    .elderNo(elder != null ? elder.getElderNo() : null)
                    .bedInfo(order.getAdmissionId() != null ? bedInfoMap.get(order.getAdmissionId()) : null)
                    .serviceItemId(order.getServiceItemId())
                    .itemName(item != null ? item.getItemName() : null)
                    .itemCode(item != null ? item.getItemCode() : null)
                    .category(item != null ? item.getCategory() : null)
                    .standardPrice(item != null ? item.getStandardPrice() : null)
                    .needConfirm(item != null ? item.getNeedConfirm() : null)
                    .sourceType(order.getSourceType())
                    .priority(order.getPriority())
                    .expectedStartAt(order.getExpectedStartAt())
                    .assigneeId(order.getAssigneeId())
                    .assigneeName(assignee != null ? assignee.getRealName() : null)
                    .creatorId(order.getCreatorId())
                    .creatorName(creator != null ? creator.getRealName() : null)
                    .status(order.getStatus())
                    .requestDesc(order.getRequestDesc())
                    .executionResult(order.getExecutionResult())
                    .completedAt(order.getCompletedAt())
                    .createdAt(order.getCreatedAt())
                    .updatedAt(order.getUpdatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, String> buildBedInfoMap(Set<Long> admissionIds) {
        if (CollectionUtils.isEmpty(admissionIds)) {
            return Collections.emptyMap();
        }
        List<Admission> admissions = admissionMapper.selectBatchIds(admissionIds);
        Set<Long> bedIds = admissions.stream().map(Admission::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (bedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Bed> beds = bedMapper.selectBatchIds(bedIds);
        Map<Long, Bed> bedMap = beds.stream().collect(Collectors.toMap(Bed::getId, b -> b));

        Set<Long> roomIds = beds.stream().map(Bed::getRoomId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Room> roomMap = roomIds.isEmpty() ? Collections.emptyMap() :
                roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, r -> r));

        Set<Long> buildingIds = roomMap.values().stream().map(Room::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Collections.emptyMap() :
                buildingMapper.selectBatchIds(buildingIds).stream().collect(Collectors.toMap(Building::getId, b -> b));

        Map<Long, String> result = new HashMap<>();
        for (Admission admission : admissions) {
            Bed bed = bedMap.get(admission.getBedId());
            if (bed != null) {
                Room room = roomMap.get(bed.getRoomId());
                Building building = room != null ? buildingMap.get(room.getBuildingId()) : null;
                String desc = (building != null ? building.getBuildingName() + " " : "")
                        + (room != null ? room.getRoomNumber() + "室 " : "")
                        + bed.getBedNo() + "床";
                result.put(admission.getId(), desc);
            }
        }
        return result;
    }

    private String generateOrderNo() {
        return "SO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 999);
    }
}
