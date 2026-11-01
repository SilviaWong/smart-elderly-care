package com.example.elderlycare.modules.risk;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
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
import com.example.elderlycare.modules.risk.service.impl.RiskEventServiceImpl;
import com.example.elderlycare.modules.risk.vo.RiskEventDetailVO;
import com.example.elderlycare.modules.risk.vo.RiskEventSummaryVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEventServiceTest {

    @Mock
    private RiskEventMapper riskEventMapper;

    @Mock
    private RiskEventLogMapper riskEventLogMapper;

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
    private RiskEventServiceImpl riskEventService;

    private final Long orgId = 1L;
    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(orgId);
        OrgContextHolder.setUserId(userId);
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("测试安全风险事件登记上报成功")
    void testReportEvent_success() {
        Long elderId = 10L;
        Elder elder = Elder.builder().id(elderId).orgId(orgId).name("张大爷").elderNo("ELD-001").build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        doAnswer(invocation -> {
            RiskEvent event = invocation.getArgument(0);
            event.setId(888L);
            return 1;
        }).when(riskEventMapper).insert(any(RiskEvent.class));

        RiskEventReportDTO dto = RiskEventReportDTO.builder()
                .elderId(elderId)
                .eventType("FALL")
                .severity("HIGH")
                .occurredAt(LocalDateTime.now().minusHours(1))
                .locationDesc("1号楼2层连廊")
                .description("长者散步时不慎滑倒，自述轻度扭伤")
                .build();

        Long eventId = riskEventService.reportEvent(dto);

        assertEquals(888L, eventId);
        verify(riskEventMapper, times(1)).insert(argThat(event ->
                event.getOrgId().equals(orgId) &&
                event.getEventNo().startsWith("RSK") &&
                "FALL".equals(event.getEventType()) &&
                "HIGH".equals(event.getSeverity()) &&
                "REPORTED".equals(event.getStatus())
        ));

        // 验证初始流转日志记录
        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                log.getRiskEventId().equals(888L) &&
                "REPORT".equals(log.getAction()) &&
                "REPORTED".equals(log.getToStatus())
        ));
    }

    @Test
    @DisplayName("测试指派负责人，事件状态流转至 ASSIGNED 并记录日志")
    void testAssignEvent_success() {
        Long eventId = 1L;
        Long assigneeId = 200L;

        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("REPORTED")
                .severity("MEDIUM")
                .build();

        SysUser assignee = SysUser.builder()
                .id(assigneeId)
                .orgId(orgId)
                .realName("李护士长")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);
        when(sysUserMapper.selectById(assigneeId)).thenReturn(assignee);

        RiskEventAssignDTO dto = RiskEventAssignDTO.builder()
                .assigneeId(assigneeId)
                .remark("请立即携带急救包到场处理")
                .build();

        riskEventService.assignEvent(eventId, dto);

        assertEquals("ASSIGNED", event.getStatus());
        assertEquals(assigneeId, event.getAssigneeId());
        verify(riskEventMapper, times(1)).updateById(event);

        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "ASSIGN".equals(log.getAction()) &&
                "REPORTED".equals(log.getFromStatus()) &&
                "ASSIGNED".equals(log.getToStatus()) &&
                log.getContent().contains("李护士长")
        ));
    }

    @Test
    @DisplayName("测试开始处置及更新进展")
    void testStartProcess_and_updateProgress() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("ASSIGNED")
                .assigneeId(userId)
                .severity("HIGH")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);

        // 1. 开始处置
        RiskEventProcessDTO startDto = RiskEventProcessDTO.builder()
                .content("医护人员到达现场，进行生命体征监测和局部冷敷")
                .build();
        riskEventService.startProcess(eventId, startDto);

        assertEquals("PROCESSING", event.getStatus());
        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "START_PROCESS".equals(log.getAction()) &&
                "PROCESSING".equals(log.getToStatus())
        ));

        // 2. 更新进展
        RiskEventProcessDTO progressDto = RiskEventProcessDTO.builder()
                .content("协作医院拍片完成，未见骨折，已返回房间静卧")
                .build();
        riskEventService.updateProgress(eventId, progressDto);

        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "PROGRESS_UPDATE".equals(log.getAction()) &&
                log.getContent().contains("未见骨折")
        ));
    }

    @Test
    @DisplayName("测试提交复核流转至 PENDING_REVIEW")
    void testSubmitReview_success() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("PROCESSING")
                .severity("HIGH")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);

        RiskEventSubmitReviewDTO dto = RiskEventSubmitReviewDTO.builder()
                .content("处置完毕，长者生命体征正常，家属已签署知情同意书，申请结案复核")
                .build();

        riskEventService.submitReview(eventId, dto);

        assertEquals("PENDING_REVIEW", event.getStatus());
        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "SUBMIT_REVIEW".equals(log.getAction()) &&
                "PENDING_REVIEW".equals(log.getToStatus())
        ));
    }

    @Test
    @DisplayName("测试主管复核通过并结案关闭")
    void testReviewEvent_approve_and_close() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("PENDING_REVIEW")
                .severity("HIGH")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);
        when(sysUserMapper.selectRoleCodesByUserId(userId)).thenReturn(List.of("NURSE_DIRECTOR"));

        RiskEventReviewDTO dto = RiskEventReviewDTO.builder()
                .approved(true)
                .conclusion("现场处置规范，长者无骨折，已加强地面防滑与护栏检查，同意结案")
                .build();

        riskEventService.reviewEvent(eventId, dto);

        assertEquals("CLOSED", event.getStatus());
        assertEquals("现场处置规范，长者无骨折，已加强地面防滑与护栏检查，同意结案", event.getConclusion());
        assertNotNull(event.getClosedAt());

        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "CLOSE".equals(log.getAction()) &&
                "CLOSED".equals(log.getToStatus()) &&
                log.getContent().contains("主管复核通过并结案归档")
        ));
    }

    @Test
    @DisplayName("测试主管复核驳回，退回 PROCESSING 重新整改")
    void testReviewEvent_reject() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("PENDING_REVIEW")
                .severity("HIGH")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);
        when(sysUserMapper.selectRoleCodesByUserId(userId)).thenReturn(List.of("ORG_ADMIN"));

        RiskEventReviewDTO dto = RiskEventReviewDTO.builder()
                .approved(false)
                .rejectReason("未提供长者次日复查记录，请补充后续随访观察记录后再提")
                .build();

        riskEventService.reviewEvent(eventId, dto);

        assertEquals("PROCESSING", event.getStatus());
        verify(riskEventLogMapper, times(1)).insert(argThat(log ->
                "REVIEW_REJECT".equals(log.getAction()) &&
                "PROCESSING".equals(log.getToStatus()) &&
                log.getContent().contains("未提供长者次日复查记录")
        ));
    }

    @Test
    @DisplayName("测试普通护理员直接关闭未复核的高风险事件时被阻断")
    void testCloseEvent_highRisk_requiresSupervisorReview() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("PROCESSING")
                .severity("HIGH")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);
        when(sysUserMapper.selectRoleCodesByUserId(userId)).thenReturn(List.of("CAREGIVER"));

        RiskEventCloseDTO dto = RiskEventCloseDTO.builder()
                .conclusion("直接关闭")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                riskEventService.closeEvent(eventId, dto)
        );

        assertTrue(ex.getMessage().contains("高风险或危急事件必须由机构主管复核"));
    }

    @Test
    @DisplayName("测试结案必须填写处理结论，空白结论抛出异常")
    void testCloseEvent_emptyConclusion_rejected() {
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(orgId)
                .eventNo("RSK202611010001")
                .status("PROCESSING")
                .severity("LOW")
                .build();

        when(riskEventMapper.selectByIdForUpdate(eventId)).thenReturn(event);

        RiskEventCloseDTO dto = RiskEventCloseDTO.builder()
                .conclusion("   ")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                riskEventService.closeEvent(eventId, dto)
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("关闭事件必须填写处理结论"));
    }

    @Test
    @DisplayName("测试跨机构访问被拒绝 (多租户数据隔离)")
    void testOrgIsolation_denied() {
        Long otherOrgId = 99L;
        Long eventId = 1L;
        RiskEvent event = RiskEvent.builder()
                .id(eventId)
                .orgId(otherOrgId)
                .eventNo("RSK-OTHER")
                .status("REPORTED")
                .build();

        when(riskEventMapper.selectById(eventId)).thenReturn(event);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                riskEventService.getEventDetail(eventId)
        );

        assertEquals(ErrorCode.ORG_ACCESS_DENIED, ex.getErrorCode());
    }
}
