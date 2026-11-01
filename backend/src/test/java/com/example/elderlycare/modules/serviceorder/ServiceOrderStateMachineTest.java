package com.example.elderlycare.modules.serviceorder;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderAction;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderStatus;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderStateMachine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("服务工单状态机核心流转规则测试")
class ServiceOrderStateMachineTest {

    @Test
    @DisplayName("免验收服务流转完整闭环: PENDING_ASSIGN -> ASSIGNED -> ACCEPTED -> IN_PROGRESS -> COMPLETED")
    void testDirectCompletionFlow() {
        // 1. 派单
        ServiceOrderStatus s1 = ServiceOrderStateMachine.getNextStatus(
                ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderAction.ASSIGN, false);
        assertEquals(ServiceOrderStatus.ASSIGNED, s1);

        // 2. 接单
        ServiceOrderStatus s2 = ServiceOrderStateMachine.getNextStatus(
                s1, ServiceOrderAction.ACCEPT, false);
        assertEquals(ServiceOrderStatus.ACCEPTED, s2);

        // 3. 开始执行
        ServiceOrderStatus s3 = ServiceOrderStateMachine.getNextStatus(
                s2, ServiceOrderAction.START, false);
        assertEquals(ServiceOrderStatus.IN_PROGRESS, s3);

        // 4. 完成（needConfirm = false 直接流转至 COMPLETED）
        ServiceOrderStatus s4 = ServiceOrderStateMachine.getNextStatus(
                s3, ServiceOrderAction.COMPLETE, false);
        assertEquals(ServiceOrderStatus.COMPLETED, s4);
    }

    @Test
    @DisplayName("需验收服务流转分支: IN_PROGRESS -> PENDING_CONFIRM -> COMPLETED")
    void testNeedConfirmCompletionFlow() {
        // 需验收服务：COMPLETE 操作进入 PENDING_CONFIRM
        ServiceOrderStatus s1 = ServiceOrderStateMachine.getNextStatus(
                ServiceOrderStatus.IN_PROGRESS, ServiceOrderAction.COMPLETE, true);
        assertEquals(ServiceOrderStatus.PENDING_CONFIRM, s1);

        // 验收通过：进入 COMPLETED
        ServiceOrderStatus s2 = ServiceOrderStateMachine.getNextStatus(
                s1, ServiceOrderAction.CONFIRM, true);
        assertEquals(ServiceOrderStatus.COMPLETED, s2);
    }

    @Test
    @DisplayName("拒单与重派流转: ASSIGNED -> REJECTED -> ASSIGNED")
    void testRejectAndReassignFlow() {
        // 拒单
        ServiceOrderStatus s1 = ServiceOrderStateMachine.getNextStatus(
                ServiceOrderStatus.ASSIGNED, ServiceOrderAction.REJECT, false);
        assertEquals(ServiceOrderStatus.REJECTED, s1);

        // 重派
        ServiceOrderStatus s2 = ServiceOrderStateMachine.getNextStatus(
                s1, ServiceOrderAction.ASSIGN, false);
        assertEquals(ServiceOrderStatus.ASSIGNED, s2);
    }

    @Test
    @DisplayName("退回与重派流转: ACCEPTED / PENDING_CONFIRM -> RETURNED -> ASSIGNED")
    void testReturnFlow() {
        // 接单后退回
        ServiceOrderStatus s1 = ServiceOrderStateMachine.getNextStatus(
                ServiceOrderStatus.ACCEPTED, ServiceOrderAction.RETURN, false);
        assertEquals(ServiceOrderStatus.RETURNED, s1);

        // 验收不通过退回
        ServiceOrderStatus s2 = ServiceOrderStateMachine.getNextStatus(
                ServiceOrderStatus.PENDING_CONFIRM, ServiceOrderAction.RETURN, true);
        assertEquals(ServiceOrderStatus.RETURNED, s2);

        // 退回后重新派单
        ServiceOrderStatus s3 = ServiceOrderStateMachine.getNextStatus(
                s2, ServiceOrderAction.ASSIGN, false);
        assertEquals(ServiceOrderStatus.ASSIGNED, s3);
    }

    @Test
    @DisplayName("取消流转测试: 允许从待派单/已派单/进行中取消")
    void testCancelFlow() {
        assertEquals(ServiceOrderStatus.CANCELLED,
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderAction.CANCEL, false));
        assertEquals(ServiceOrderStatus.CANCELLED,
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.ASSIGNED, ServiceOrderAction.CANCEL, false));
        assertEquals(ServiceOrderStatus.CANCELLED,
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.IN_PROGRESS, ServiceOrderAction.CANCEL, false));
        assertEquals(ServiceOrderStatus.CANCELLED,
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.PENDING_CONFIRM, ServiceOrderAction.CANCEL, true));
    }

    @Test
    @DisplayName("终态工单禁止非法跳转: COMPLETED 与 CANCELLED 不允许任何操作")
    void testTerminalStateIllegalTransitions() {
        // 已完成工单禁止取消
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.COMPLETED, ServiceOrderAction.CANCEL, false));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex1.getCode());

        // 已完成工单禁止重新开始
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.COMPLETED, ServiceOrderAction.START, false));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex2.getCode());

        // 已取消工单禁止派单
        BusinessException ex3 = assertThrows(BusinessException.class, () ->
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.CANCELLED, ServiceOrderAction.ASSIGN, false));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex3.getCode());
    }

    @Test
    @DisplayName("越级非法跃迁校验: 待派单工单禁止直接开始或完成")
    void testIllegalSkipTransitions() {
        // PENDING_ASSIGN 直接 START 非法
        BusinessException ex1 = assertThrows(BusinessException.class, () ->
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderAction.START, false));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex1.getCode());

        // PENDING_ASSIGN 直接 COMPLETE 非法
        BusinessException ex2 = assertThrows(BusinessException.class, () ->
                ServiceOrderStateMachine.getNextStatus(ServiceOrderStatus.PENDING_ASSIGN, ServiceOrderAction.COMPLETE, false));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex2.getCode());
    }
}
