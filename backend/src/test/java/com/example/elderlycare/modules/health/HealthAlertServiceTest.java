package com.example.elderlycare.modules.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.health.dto.HealthAlertAssignDTO;
import com.example.elderlycare.modules.health.dto.HealthAlertHandleDTO;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import com.example.elderlycare.modules.health.entity.HealthAlertRule;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.health.mapper.HealthAlertRuleMapper;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.service.impl.HealthAlertServiceImpl;
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
@DisplayName("健康预警规则引擎、去重窗口与处置流程测试")
class HealthAlertServiceTest {

    @Mock
    private HealthAlertMapper healthAlertMapper;

    @Mock
    private HealthAlertRuleMapper healthAlertRuleMapper;

    @Mock
    private HealthMetricMapper healthMetricMapper;

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
    private HealthAlertServiceImpl healthAlertService;

    private static final Long ORG_ID = 1L;
    private static final Long ELDER_ID = 100L;
    private static final Long METRIC_ID = 1L;
    private static final Long RULE_ID = 10L;
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
    @DisplayName("规则匹配成功并触发预警: 超过上限阈值且窗口内无重复告警")
    void testEvaluateAndTriggerAlert_TriggeredWhenThresholdExceeded() {
        HealthRecord record = HealthRecord.builder()
                .id(501L)
                .orgId(ORG_ID)
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("168.000"))
                .build();

        HealthAlertRule rule = HealthAlertRule.builder()
                .id(RULE_ID)
                .orgId(ORG_ID)
                .metricId(METRIC_ID)
                .ruleName("高血压超标预警")
                .conditionType("GT")
                .maxValue(new BigDecimal("160.000"))
                .severity("HIGH")
                .dedupMinutes(30)
                .enabled(1)
                .build();

        when(healthAlertRuleMapper.selectList(any())).thenReturn(List.of(rule));
        // 去重窗口内无告警
        when(healthAlertMapper.selectCount(any())).thenReturn(0L);

        healthAlertService.evaluateAndTriggerAlert(record);

        ArgumentCaptor<HealthAlert> alertCaptor = ArgumentCaptor.forClass(HealthAlert.class);
        verify(healthAlertMapper).insert(alertCaptor.capture());
        HealthAlert createdAlert = alertCaptor.getValue();

        assertEquals(ORG_ID, createdAlert.getOrgId());
        assertEquals(ELDER_ID, createdAlert.getElderId());
        assertEquals(record.getId(), createdAlert.getHealthRecordId());
        assertEquals(RULE_ID, createdAlert.getRuleId());
        assertEquals("HIGH", createdAlert.getSeverity());
        assertEquals(new BigDecimal("168.000"), createdAlert.getAlertValue());
        assertEquals("OPEN", createdAlert.getStatus());
        assertTrue(createdAlert.getAlertNo().startsWith("ALT"));
    }

    @Test
    @DisplayName("重复预警去重抑制: 去重窗口内已存在同规则告警，禁止重复生成")
    void testEvaluateAndTriggerAlert_DeduplicatedWithinWindow() {
        HealthRecord record = HealthRecord.builder()
                .id(502L)
                .orgId(ORG_ID)
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("172.000"))
                .build();

        HealthAlertRule rule = HealthAlertRule.builder()
                .id(RULE_ID)
                .orgId(ORG_ID)
                .metricId(METRIC_ID)
                .ruleName("高血压超标预警")
                .conditionType("GT")
                .maxValue(new BigDecimal("160.000"))
                .severity("HIGH")
                .dedupMinutes(30)
                .enabled(1)
                .build();

        when(healthAlertRuleMapper.selectList(any())).thenReturn(List.of(rule));
        // 模拟去重窗口内已有一条告警
        when(healthAlertMapper.selectCount(any())).thenReturn(1L);

        healthAlertService.evaluateAndTriggerAlert(record);

        // 验证没有插入新的 health_alert
        verify(healthAlertMapper, never()).insert(any(HealthAlert.class));
    }

    @Test
    @DisplayName("未达到阈值不触发预警")
    void testEvaluateAndTriggerAlert_ConditionNotExceeded() {
        HealthRecord record = HealthRecord.builder()
                .id(503L)
                .orgId(ORG_ID)
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("135.000"))
                .build();

        HealthAlertRule rule = HealthAlertRule.builder()
                .id(RULE_ID)
                .orgId(ORG_ID)
                .metricId(METRIC_ID)
                .ruleName("高血压超标预警")
                .conditionType("GT")
                .maxValue(new BigDecimal("160.000"))
                .severity("HIGH")
                .dedupMinutes(30)
                .enabled(1)
                .build();

        when(healthAlertRuleMapper.selectList(any())).thenReturn(List.of(rule));

        healthAlertService.evaluateAndTriggerAlert(record);

        verify(healthAlertMapper, never()).selectCount(any());
        verify(healthAlertMapper, never()).insert(any(HealthAlert.class));
    }

    @Test
    @DisplayName("指派预警责任人: OPEN 状态流转为 PROCESSING")
    void testAssignAlert_Success() {
        Long alertId = 801L;
        HealthAlert alert = HealthAlert.builder()
                .id(alertId)
                .orgId(ORG_ID)
                .alertNo("ALT202611010001")
                .status("OPEN")
                .build();
        when(healthAlertMapper.selectByIdForUpdate(alertId)).thenReturn(alert);

        SysUser assignee = SysUser.builder().id(CAREGIVER_ID).orgId(ORG_ID).realName("李护士").build();
        when(sysUserMapper.selectById(CAREGIVER_ID)).thenReturn(assignee);

        HealthAlertAssignDTO dto = HealthAlertAssignDTO.builder().assigneeId(CAREGIVER_ID).build();
        healthAlertService.assignAlert(alertId, dto);

        assertEquals("PROCESSING", alert.getStatus());
        assertEquals(CAREGIVER_ID, alert.getAssigneeId());
        verify(healthAlertMapper).updateById(alert);
    }

    @Test
    @DisplayName("处置预警闭环: 处置状态更新为 RESOLVED，并保存处置说明")
    void testHandleAlert_Success() {
        Long alertId = 801L;
        HealthAlert alert = HealthAlert.builder()
                .id(alertId)
                .orgId(ORG_ID)
                .alertNo("ALT202611010001")
                .status("PROCESSING")
                .build();
        when(healthAlertMapper.selectByIdForUpdate(alertId)).thenReturn(alert);

        HealthAlertHandleDTO dto = HealthAlertHandleDTO.builder()
                .status("RESOLVED")
                .handleResult("已复测体征，遵医嘱予以降压处置后数值已平稳。")
                .build();

        healthAlertService.handleAlert(alertId, dto);

        assertEquals("RESOLVED", alert.getStatus());
        assertEquals("已复测体征，遵医嘱予以降压处置后数值已平稳。", alert.getHandleResult());
        assertNotNull(alert.getHandledAt());
        verify(healthAlertMapper).updateById(alert);
    }
}
