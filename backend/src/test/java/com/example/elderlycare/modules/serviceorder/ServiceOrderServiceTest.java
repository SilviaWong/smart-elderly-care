package com.example.elderlycare.modules.serviceorder;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderActionDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderAssignDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderCreateDTO;
import com.example.elderlycare.modules.serviceorder.entity.ServiceItem;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrderLog;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceItemMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderLogMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import com.example.elderlycare.modules.serviceorder.service.impl.ServiceOrderServiceImpl;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderAction;
import com.example.elderlycare.modules.serviceorder.statemachine.ServiceOrderStatus;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("服务工单服务层业务逻辑与状态机联动测试")
class ServiceOrderServiceTest {

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private ServiceOrderLogMapper serviceOrderLogMapper;

    @Mock
    private ServiceItemMapper serviceItemMapper;

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private AdmissionMapper admissionMapper;

    @Mock
    private BedMapper bedMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private BuildingMapper buildingMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOperationLogService sysOperationLogService;

    @InjectMocks
    private ServiceOrderServiceImpl serviceOrderService;

    private static final Long ORG_ID = 1L;
    private static final Long OTHER_ORG_ID = 2L;
    private static final Long OPERATOR_ID = 10L;
    private static final Long CAREGIVER_ID = 20L;
    private static final Long OTHER_CAREGIVER_ID = 30L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(ORG_ID);
        OrgContextHolder.setUserId(OPERATOR_ID);
        OrgContextHolder.setUsername("test_operator");
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("创建工单（无指派人）: 初始状态为 PENDING_ASSIGN，自动关联在住床位与生成首条操作日志")
    void testCreateOrder_WithoutAssignee_PendingAssign() {
        Long elderId = 100L;
        Long itemId = 200L;

        Elder elder = Elder.builder().id(elderId).orgId(ORG_ID).name("张大爷").build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).itemName("助浴").status("ACTIVE").build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        Admission admission = Admission.builder().id(300L).elderId(elderId).status("CHECKED_IN").build();
        when(admissionMapper.selectOne(any())).thenReturn(admission);

        when(serviceOrderMapper.insert(any(ServiceOrder.class))).thenAnswer(invocation -> {
            ServiceOrder o = invocation.getArgument(0);
            o.setId(1001L);
            return 1;
        });

        ServiceOrderCreateDTO dto = ServiceOrderCreateDTO.builder()
                .elderId(elderId)
                .serviceItemId(itemId)
                .requestDesc("常规助浴")
                .priority("NORMAL")
                .build();

        Long orderId = serviceOrderService.createOrder(dto);
        assertNotNull(orderId);

        ArgumentCaptor<ServiceOrder> orderCaptor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(serviceOrderMapper).insert(orderCaptor.capture());
        ServiceOrder savedOrder = orderCaptor.getValue();
        assertEquals(ServiceOrderStatus.PENDING_ASSIGN.name(), savedOrder.getStatus());
        assertEquals(admission.getId(), savedOrder.getAdmissionId());
        assertNull(savedOrder.getAssigneeId());

        ArgumentCaptor<ServiceOrderLog> logCaptor = ArgumentCaptor.forClass(ServiceOrderLog.class);
        verify(serviceOrderLogMapper).insert(logCaptor.capture());
        ServiceOrderLog log = logCaptor.getValue();
        assertEquals(ServiceOrderStatus.PENDING_ASSIGN.name(), log.getToStatus());
        assertEquals(ServiceOrderAction.CREATE.name(), log.getAction());
    }

    @Test
    @DisplayName("创建工单（直接指派人员）: 初始状态直接为 ASSIGNED")
    void testCreateOrder_WithAssignee_Assigned() {
        Long elderId = 100L;
        Long itemId = 200L;

        Elder elder = Elder.builder().id(elderId).orgId(ORG_ID).name("李奶奶").build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).itemName("理疗").status("ACTIVE").build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        SysUser assignee = SysUser.builder().id(CAREGIVER_ID).orgId(ORG_ID).realName("李护理员").build();
        when(sysUserMapper.selectById(CAREGIVER_ID)).thenReturn(assignee);

        when(serviceOrderMapper.insert(any(ServiceOrder.class))).thenAnswer(invocation -> {
            ServiceOrder o = invocation.getArgument(0);
            o.setId(1002L);
            return 1;
        });

        ServiceOrderCreateDTO dto = ServiceOrderCreateDTO.builder()
                .elderId(elderId)
                .serviceItemId(itemId)
                .assigneeId(CAREGIVER_ID)
                .build();

        Long orderId = serviceOrderService.createOrder(dto);
        assertEquals(1002L, orderId);

        ArgumentCaptor<ServiceOrder> orderCaptor = ArgumentCaptor.forClass(ServiceOrder.class);
        verify(serviceOrderMapper).insert(orderCaptor.capture());
        assertEquals(ServiceOrderStatus.ASSIGNED.name(), orderCaptor.getValue().getStatus());
        assertEquals(CAREGIVER_ID, orderCaptor.getValue().getAssigneeId());
    }

    @Test
    @DisplayName("创建工单失败: 跨机构长者抛出 ORG_ACCESS_DENIED 异常")
    void testCreateOrder_ThrowsException_WhenCrossOrgElder() {
        Long elderId = 100L;
        Elder elder = Elder.builder().id(elderId).orgId(OTHER_ORG_ID).name("其他机构长者").build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        ServiceOrderCreateDTO dto = ServiceOrderCreateDTO.builder()
                .elderId(elderId)
                .serviceItemId(200L)
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> serviceOrderService.createOrder(dto));
        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        verify(serviceOrderMapper, never()).insert(any(ServiceOrder.class));
    }

    @Test
    @DisplayName("派单成功: PENDING_ASSIGN 流转为 ASSIGNED，并记录流转日志")
    void testAssignOrder_Success() {
        Long orderId = 1001L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .orderNo("SO202611010001")
                .status(ServiceOrderStatus.PENDING_ASSIGN.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        SysUser assignee = SysUser.builder().id(CAREGIVER_ID).orgId(ORG_ID).realName("张护理员").build();
        when(sysUserMapper.selectById(CAREGIVER_ID)).thenReturn(assignee);

        ServiceOrderAssignDTO assignDTO = ServiceOrderAssignDTO.builder()
                .assigneeId(CAREGIVER_ID)
                .remark("主管派单")
                .build();

        serviceOrderService.assignOrder(orderId, assignDTO);

        assertEquals(ServiceOrderStatus.ASSIGNED.name(), order.getStatus());
        assertEquals(CAREGIVER_ID, order.getAssigneeId());
        verify(serviceOrderMapper).updateById(order);

        ArgumentCaptor<ServiceOrderLog> logCaptor = ArgumentCaptor.forClass(ServiceOrderLog.class);
        verify(serviceOrderLogMapper).insert(logCaptor.capture());
        assertEquals(ServiceOrderStatus.PENDING_ASSIGN.name(), logCaptor.getValue().getFromStatus());
        assertEquals(ServiceOrderStatus.ASSIGNED.name(), logCaptor.getValue().getToStatus());
        assertEquals(ServiceOrderAction.ASSIGN.name(), logCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("接单操作校验: 非被指派人且非管理员接单被拒绝")
    void testExecuteAction_Accept_Forbidden_WhenNotAssignee() {
        Long orderId = 1001L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .assigneeId(CAREGIVER_ID)
                .status(ServiceOrderStatus.ASSIGNED.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        // 当前登录用户为 OTHER_CAREGIVER_ID，角色仅为普通护理员
        OrgContextHolder.setUserId(OTHER_CAREGIVER_ID);
        when(sysUserMapper.selectRoleCodesByUserId(OTHER_CAREGIVER_ID)).thenReturn(List.of("CAREGIVER"));

        ServiceOrderActionDTO actionDTO = ServiceOrderActionDTO.builder()
                .action(ServiceOrderAction.ACCEPT)
                .remark("尝试冒名接单")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceOrderService.executeAction(orderId, actionDTO));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        verify(serviceOrderMapper, never()).updateById(any(ServiceOrder.class));
    }

    @Test
    @DisplayName("完成服务（免验收）: IN_PROGRESS 直接流转为 COMPLETED，设置 completedAt 时间")
    void testExecuteAction_Complete_DirectWhenNoConfirm() {
        Long orderId = 1001L;
        Long itemId = 200L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .serviceItemId(itemId)
                .assigneeId(CAREGIVER_ID)
                .status(ServiceOrderStatus.IN_PROGRESS.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).needConfirm(0).build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        OrgContextHolder.setUserId(CAREGIVER_ID);
        when(sysUserMapper.selectRoleCodesByUserId(CAREGIVER_ID)).thenReturn(List.of("CAREGIVER"));

        ServiceOrderActionDTO actionDTO = ServiceOrderActionDTO.builder()
                .action(ServiceOrderAction.COMPLETE)
                .executionResult("晨间翻身顺利完成")
                .build();

        serviceOrderService.executeAction(orderId, actionDTO);

        assertEquals(ServiceOrderStatus.COMPLETED.name(), order.getStatus());
        assertEquals("晨间翻身顺利完成", order.getExecutionResult());
        assertNotNull(order.getCompletedAt());
        verify(serviceOrderMapper).updateById(order);
    }

    @Test
    @DisplayName("完成服务（需验收）: IN_PROGRESS 流转为 PENDING_CONFIRM，不设置 completedAt")
    void testExecuteAction_Complete_BranchNeedConfirm() {
        Long orderId = 1001L;
        Long itemId = 200L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .serviceItemId(itemId)
                .assigneeId(CAREGIVER_ID)
                .status(ServiceOrderStatus.IN_PROGRESS.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        // needConfirm = 1
        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).needConfirm(1).build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        OrgContextHolder.setUserId(CAREGIVER_ID);
        when(sysUserMapper.selectRoleCodesByUserId(CAREGIVER_ID)).thenReturn(List.of("CAREGIVER"));

        ServiceOrderActionDTO actionDTO = ServiceOrderActionDTO.builder()
                .action(ServiceOrderAction.COMPLETE)
                .executionResult("助浴完成，等待主管验收")
                .build();

        serviceOrderService.executeAction(orderId, actionDTO);

        assertEquals(ServiceOrderStatus.PENDING_CONFIRM.name(), order.getStatus());
        assertNull(order.getCompletedAt());
        verify(serviceOrderMapper).updateById(order);
    }

    @Test
    @DisplayName("验收操作权限校验: 普通执行人员禁止自我验收")
    void testExecuteAction_Confirm_Forbidden_WhenSelfConfirm() {
        Long orderId = 1001L;
        Long itemId = 200L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .serviceItemId(itemId)
                .assigneeId(CAREGIVER_ID)
                .status(ServiceOrderStatus.PENDING_CONFIRM.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).needConfirm(1).build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        // 当前操作人就是执行人 CAREGIVER_ID，角色仅为普通护理员
        OrgContextHolder.setUserId(CAREGIVER_ID);
        when(sysUserMapper.selectRoleCodesByUserId(CAREGIVER_ID)).thenReturn(List.of("CAREGIVER"));

        ServiceOrderActionDTO actionDTO = ServiceOrderActionDTO.builder()
                .action(ServiceOrderAction.CONFIRM)
                .remark("尝试自验通过")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceOrderService.executeAction(orderId, actionDTO));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        verify(serviceOrderMapper, never()).updateById(any(ServiceOrder.class));
    }

    @Test
    @DisplayName("主管验收通过: PENDING_CONFIRM 流转为 COMPLETED，设置 completedAt 时间")
    void testExecuteAction_Confirm_Success_WhenDirector() {
        Long orderId = 1001L;
        Long itemId = 200L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .serviceItemId(itemId)
                .assigneeId(CAREGIVER_ID)
                .status(ServiceOrderStatus.PENDING_CONFIRM.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        ServiceItem item = ServiceItem.builder().id(itemId).orgId(ORG_ID).needConfirm(1).build();
        when(serviceItemMapper.selectById(itemId)).thenReturn(item);

        // 主管操作
        OrgContextHolder.setUserId(OPERATOR_ID);
        when(sysUserMapper.selectRoleCodesByUserId(OPERATOR_ID)).thenReturn(List.of("NURSE_DIRECTOR"));

        ServiceOrderActionDTO actionDTO = ServiceOrderActionDTO.builder()
                .action(ServiceOrderAction.CONFIRM)
                .remark("助浴质量符合规范，验收合格")
                .build();

        serviceOrderService.executeAction(orderId, actionDTO);

        assertEquals(ServiceOrderStatus.COMPLETED.name(), order.getStatus());
        assertNotNull(order.getCompletedAt());
        verify(serviceOrderMapper).updateById(order);

        ArgumentCaptor<ServiceOrderLog> logCaptor = ArgumentCaptor.forClass(ServiceOrderLog.class);
        verify(serviceOrderLogMapper).insert(logCaptor.capture());
        assertEquals(ServiceOrderStatus.PENDING_CONFIRM.name(), logCaptor.getValue().getFromStatus());
        assertEquals(ServiceOrderStatus.COMPLETED.name(), logCaptor.getValue().getToStatus());
        assertEquals(ServiceOrderAction.CONFIRM.name(), logCaptor.getValue().getAction());
    }

    @Test
    @DisplayName("已完成工单防篡改校验: COMPLETED 工单修改核心内容抛出 CONFLICT 异常")
    void testUpdateOrder_Conflict_WhenCompleted() {
        Long orderId = 1001L;
        ServiceOrder order = ServiceOrder.builder()
                .id(orderId)
                .orgId(ORG_ID)
                .status(ServiceOrderStatus.COMPLETED.name())
                .build();
        when(serviceOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        ServiceOrderCreateDTO updateDTO = ServiceOrderCreateDTO.builder()
                .priority("URGENT")
                .requestDesc("篡改已完成工单描述")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                serviceOrderService.updateOrder(orderId, updateDTO));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
        verify(serviceOrderMapper, never()).updateById(any(ServiceOrder.class));
    }
}
