package com.example.elderlycare.modules.health;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.health.dto.HealthRecordCreateDTO;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.mapper.HealthRecordMapper;
import com.example.elderlycare.modules.health.service.HealthAlertService;
import com.example.elderlycare.modules.health.service.impl.HealthRecordServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("长者健康体征记录服务与预警联动测试")
class HealthRecordServiceTest {

    @Mock
    private HealthRecordMapper healthRecordMapper;

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
    private HealthAlertService healthAlertService;

    @Mock
    private SysOperationLogService sysOperationLogService;

    @InjectMocks
    private HealthRecordServiceImpl healthRecordService;

    private static final Long ORG_ID = 1L;
    private static final Long OTHER_ORG_ID = 2L;
    private static final Long ELDER_ID = 100L;
    private static final Long METRIC_ID = 1L;
    private static final Long RECORDER_ID = 5L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(ORG_ID);
        OrgContextHolder.setUserId(RECORDER_ID);
        OrgContextHolder.setUsername("test_recorder");
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("录入健康体征成功: 快照单位并联动触发预警评估")
    void testCreateRecord_Success_WithTrigger() {
        Elder elder = Elder.builder().id(ELDER_ID).orgId(ORG_ID).name("张建国").build();
        when(elderMapper.selectById(ELDER_ID)).thenReturn(elder);

        HealthMetric metric = HealthMetric.builder()
                .id(METRIC_ID)
                .orgId(ORG_ID)
                .metricCode("SBP")
                .metricName("收缩压")
                .unit("mmHg")
                .status("ACTIVE")
                .build();
        when(healthMetricMapper.selectById(METRIC_ID)).thenReturn(metric);

        when(healthRecordMapper.insert(any(HealthRecord.class))).thenAnswer(invocation -> {
            HealthRecord r = invocation.getArgument(0);
            r.setId(901L);
            return 1;
        });

        HealthRecordCreateDTO dto = HealthRecordCreateDTO.builder()
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("165.000"))
                .measuredAt(LocalDateTime.now())
                .remark("晨起测压")
                .build();

        Long recordId = healthRecordService.createRecord(dto);
        assertEquals(901L, recordId);

        ArgumentCaptor<HealthRecord> recordCaptor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordMapper).insert(recordCaptor.capture());
        HealthRecord saved = recordCaptor.getValue();
        assertEquals("mmHg", saved.getUnitSnapshot());
        assertEquals(new BigDecimal("165.000"), saved.getMetricValue());
        assertEquals(RECORDER_ID, saved.getRecorderId());

        // 验证联动触发了告警评估
        verify(healthAlertService).evaluateAndTriggerAlert(saved);
    }

    @Test
    @DisplayName("录入体征校验: 测量时间不得超前当前时间")
    void testCreateRecord_ThrowsException_WhenFutureTime() {
        Elder elder = Elder.builder().id(ELDER_ID).orgId(ORG_ID).name("张建国").build();
        when(elderMapper.selectById(ELDER_ID)).thenReturn(elder);

        HealthMetric metric = HealthMetric.builder()
                .id(METRIC_ID)
                .orgId(ORG_ID)
                .status("ACTIVE")
                .build();
        when(healthMetricMapper.selectById(METRIC_ID)).thenReturn(metric);

        HealthRecordCreateDTO dto = HealthRecordCreateDTO.builder()
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("120.000"))
                .measuredAt(LocalDateTime.now().plusHours(2)) // 未来时间
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> healthRecordService.createRecord(dto));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(healthRecordMapper, never()).insert(any(HealthRecord.class));
    }

    @Test
    @DisplayName("录入体征校验: 跨机构长者抛出 ORG_ACCESS_DENIED 异常")
    void testCreateRecord_ThrowsException_WhenCrossOrgElder() {
        Elder elder = Elder.builder().id(ELDER_ID).orgId(OTHER_ORG_ID).name("他机构长者").build();
        when(elderMapper.selectById(ELDER_ID)).thenReturn(elder);

        HealthRecordCreateDTO dto = HealthRecordCreateDTO.builder()
                .elderId(ELDER_ID)
                .metricId(METRIC_ID)
                .metricValue(new BigDecimal("120.000"))
                .measuredAt(LocalDateTime.now())
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> healthRecordService.createRecord(dto));
        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        verify(healthRecordMapper, never()).insert(any(HealthRecord.class));
    }
}
