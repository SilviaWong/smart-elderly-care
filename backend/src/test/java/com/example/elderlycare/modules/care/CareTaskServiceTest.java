package com.example.elderlycare.modules.care;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.care.dto.CareTaskActionDTO;
import com.example.elderlycare.modules.care.dto.CareTaskGenerateDTO;
import com.example.elderlycare.modules.care.entity.CarePlan;
import com.example.elderlycare.modules.care.entity.CarePlanItem;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.entity.CareTaskRecord;
import com.example.elderlycare.modules.care.mapper.*;
import com.example.elderlycare.modules.care.service.impl.CareTaskServiceImpl;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("护理任务生成幂等性保障与任务流转执行测试")
class CareTaskServiceTest {

    @Mock
    private CareTaskMapper careTaskMapper;

    @Mock
    private CareTaskRecordMapper careTaskRecordMapper;

    @Mock
    private CarePlanMapper carePlanMapper;

    @Mock
    private CarePlanItemMapper carePlanItemMapper;

    @Mock
    private CareItemMapper careItemMapper;

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
    private CareTaskServiceImpl careTaskService;

    private static final Long ORG_ID = 1L;
    private static final Long ELDER_ID = 100L;
    private static final Long PLAN_ID = 10L;
    private static final Long PLAN_ITEM_ID = 20L;
    private static final Long CAREGIVER_ID = 2L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(ORG_ID);
        OrgContextHolder.setUserId(CAREGIVER_ID);
        OrgContextHolder.setUsername("test_caregiver");
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("首次生成任务: 命中有效计划，生成任务并写入稳定幂等键")
    void testGenerateTasks_FirstRun_Success() {
        LocalDate taskDate = LocalDate.of(2026, 11, 2);

        CarePlan plan = CarePlan.builder()
                .id(PLAN_ID)
                .orgId(ORG_ID)
                .elderId(ELDER_ID)
                .startDate(LocalDate.of(2026, 11, 1))
                .status("ACTIVE")
                .build();
        when(carePlanMapper.selectList(any())).thenReturn(List.of(plan));

        CarePlanItem item = CarePlanItem.builder()
                .id(PLAN_ITEM_ID)
                .planId(PLAN_ID)
                .careItemId(1L)
                .frequencyType("DAILY")
                .timeWindowStart(LocalTime.of(8, 0))
                .assigneeId(CAREGIVER_ID)
                .enabled(1)
                .build();
        when(carePlanItemMapper.selectList(any())).thenReturn(List.of(item));

        // 首次生成，DB中不存在该幂等键
        when(careTaskMapper.selectCount(any())).thenReturn(0L);

        CareTaskGenerateDTO dto = CareTaskGenerateDTO.builder().taskDate(taskDate).build();
        int generated = careTaskService.generateTasks(dto);

        assertEquals(1, generated);

        ArgumentCaptor<CareTask> taskCaptor = ArgumentCaptor.forClass(CareTask.class);
        verify(careTaskMapper).insert(taskCaptor.capture());
        CareTask created = taskCaptor.getValue();

        assertEquals(ORG_ID, created.getOrgId());
        assertEquals(PLAN_ID, created.getPlanId());
        assertEquals(PLAN_ITEM_ID, created.getPlanItemId());
        assertEquals(ELDER_ID, created.getElderId());
        assertEquals(taskDate, created.getTaskDate());
        assertEquals("PENDING", created.getStatus());
        assertEquals("CPI_20_2026-11-02_08:00", created.getIdempotencyKey());
    }

    @Test
    @DisplayName("重复触发生成任务: 稳定幂等键检测已存在，生成数为0且禁止重复插入")
    void testGenerateTasks_SecondRun_IdempotentSuppression() {
        LocalDate taskDate = LocalDate.of(2026, 11, 2);

        CarePlan plan = CarePlan.builder()
                .id(PLAN_ID)
                .orgId(ORG_ID)
                .elderId(ELDER_ID)
                .startDate(LocalDate.of(2026, 11, 1))
                .status("ACTIVE")
                .build();
        when(carePlanMapper.selectList(any())).thenReturn(List.of(plan));

        CarePlanItem item = CarePlanItem.builder()
                .id(PLAN_ITEM_ID)
                .planId(PLAN_ID)
                .careItemId(1L)
                .frequencyType("DAILY")
                .timeWindowStart(LocalTime.of(8, 0))
                .assigneeId(CAREGIVER_ID)
                .enabled(1)
                .build();
        when(carePlanItemMapper.selectList(any())).thenReturn(List.of(item));

        // 模拟已存在任务
        when(careTaskMapper.selectCount(any())).thenReturn(1L);

        CareTaskGenerateDTO dto = CareTaskGenerateDTO.builder().taskDate(taskDate).build();
        int generated = careTaskService.generateTasks(dto);

        assertEquals(0, generated);
        verify(careTaskMapper, never()).insert(any(CareTask.class));
    }

    @Test
    @DisplayName("任务流转执行闭环: PENDING -> START (IN_PROGRESS) -> COMPLETE (COMPLETED) 并保留执行记录")
    void testExecuteTaskAction_StartAndComplete_Success() {
        Long taskId = 9001L;
        CareTask task = CareTask.builder()
                .id(taskId)
                .orgId(ORG_ID)
                .taskNo("CT202611020001")
                .status("PENDING")
                .build();
        when(careTaskMapper.selectByIdForUpdate(taskId)).thenReturn(task);

        // 1. 开始执行
        CareTaskActionDTO startDto = CareTaskActionDTO.builder().action("START").build();
        careTaskService.executeTaskAction(taskId, startDto);
        assertEquals("IN_PROGRESS", task.getStatus());
        verify(careTaskMapper).updateById(task);

        // 2. 完成执行
        CareTaskActionDTO completeDto = CareTaskActionDTO.builder()
                .action("COMPLETE")
                .resultCode("NORMAL")
                .resultDesc("按时协助口腔漱口清洁")
                .build();
        careTaskService.executeTaskAction(taskId, completeDto);
        assertEquals("COMPLETED", task.getStatus());

        ArgumentCaptor<CareTaskRecord> recordCaptor = ArgumentCaptor.forClass(CareTaskRecord.class);
        verify(careTaskRecordMapper).insert(recordCaptor.capture());
        CareTaskRecord record = recordCaptor.getValue();
        assertEquals(taskId, record.getTaskId());
        assertEquals(CAREGIVER_ID, record.getExecutorId());
        assertEquals("NORMAL", record.getResultCode());
        assertEquals("按时协助口腔漱口清洁", record.getResultDesc());
    }

    @Test
    @DisplayName("已完成任务防篡改: COMPLETED 任务禁止取消")
    void testExecuteTaskAction_CannotCancelCompleted() {
        Long taskId = 9002L;
        CareTask task = CareTask.builder()
                .id(taskId)
                .orgId(ORG_ID)
                .status("COMPLETED")
                .build();
        when(careTaskMapper.selectByIdForUpdate(taskId)).thenReturn(task);

        CareTaskActionDTO cancelDto = CareTaskActionDTO.builder()
                .action("CANCEL")
                .resultDesc("试图取消已完成任务")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                careTaskService.executeTaskAction(taskId, cancelDto));
        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
        verify(careTaskRecordMapper, never()).insert(any(CareTaskRecord.class));
    }
}
