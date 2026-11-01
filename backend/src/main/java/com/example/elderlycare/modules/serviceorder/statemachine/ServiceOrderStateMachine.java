package com.example.elderlycare.modules.serviceorder.statemachine;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.exception.BusinessException;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 服务工单状态机核心流转规则控制器
 * 严格执行 PRD 规定的工单流转规则，防止状态非法跃迁，保证审计一致性
 */
public final class ServiceOrderStateMachine {

    private static final Map<ServiceOrderAction, Set<ServiceOrderStatus>> VALID_SOURCE_STATES = new EnumMap<>(ServiceOrderAction.class);

    static {
        // 派单动作允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.ASSIGN,
                EnumSet.of(ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderStatus.ASSIGNED, ServiceOrderStatus.REJECTED, ServiceOrderStatus.RETURNED));

        // 接单确认允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.ACCEPT,
                EnumSet.of(ServiceOrderStatus.ASSIGNED));

        // 开始执行允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.START,
                EnumSet.of(ServiceOrderStatus.ACCEPTED));

        // 完成执行允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.COMPLETE,
                EnumSet.of(ServiceOrderStatus.IN_PROGRESS));

        // 验收通过允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.CONFIRM,
                EnumSet.of(ServiceOrderStatus.PENDING_CONFIRM));

        // 拒单允许的初始状态
        VALID_SOURCE_STATES.put(ServiceOrderAction.REJECT,
                EnumSet.of(ServiceOrderStatus.ASSIGNED));

        // 退回允许的初始状态（接单后退回，或验收不通过退回）
        VALID_SOURCE_STATES.put(ServiceOrderAction.RETURN,
                EnumSet.of(ServiceOrderStatus.ACCEPTED, ServiceOrderStatus.PENDING_CONFIRM));

        // 取消允许的初始状态（已完成和已取消工单禁止取消）
        VALID_SOURCE_STATES.put(ServiceOrderAction.CANCEL,
                EnumSet.of(ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderStatus.ASSIGNED,
                        ServiceOrderStatus.ACCEPTED, ServiceOrderStatus.IN_PROGRESS,
                        ServiceOrderStatus.PENDING_CONFIRM, ServiceOrderStatus.REJECTED,
                        ServiceOrderStatus.RETURNED));
    }

    private ServiceOrderStateMachine() {
    }

    /**
     * 根据当前状态、动作和服务项目验收配置计算下一个状态
     *
     * @param current     当前状态
     * @param action      触发动作
     * @param needConfirm 服务项目是否需要验收确认
     * @return 目标新状态
     */
    public static ServiceOrderStatus getNextStatus(ServiceOrderStatus current, ServiceOrderAction action, boolean needConfirm) {
        if (current == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工单当前状态不能为空");
        }
        if (action == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "操作动作不能为空");
        }

        Set<ServiceOrderStatus> validSources = VALID_SOURCE_STATES.get(action);
        if (validSources == null || !validSources.contains(current)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    String.format("工单当前处于 [%s] 状态，不允许执行 [%s] 操作", current.getDescription(), action.getDescription())
            );
        }

        switch (action) {
            case ASSIGN:
                return ServiceOrderStatus.ASSIGNED;
            case ACCEPT:
                return ServiceOrderStatus.ACCEPTED;
            case START:
                return ServiceOrderStatus.IN_PROGRESS;
            case COMPLETE:
                // 如果服务项目配置为需要确认验收，则进入 PENDING_CONFIRM，否则直接进入 COMPLETED
                return needConfirm ? ServiceOrderStatus.PENDING_CONFIRM : ServiceOrderStatus.COMPLETED;
            case CONFIRM:
                return ServiceOrderStatus.COMPLETED;
            case REJECT:
                return ServiceOrderStatus.REJECTED;
            case RETURN:
                return ServiceOrderStatus.RETURNED;
            case CANCEL:
                return ServiceOrderStatus.CANCELLED;
            default:
                throw new BusinessException(ErrorCode.CONFLICT, "未知工单流转动作: " + action);
        }
    }
}
