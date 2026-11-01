package com.example.elderlycare.modules.iot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.mapper.HealthRecordMapper;
import com.example.elderlycare.modules.iot.adapter.DefaultHttpIotAdapter;
import com.example.elderlycare.modules.iot.adapter.HuaweiHealthIotAdapter;
import com.example.elderlycare.modules.iot.adapter.IotAdapterFactory;
import com.example.elderlycare.modules.iot.dto.DeviceBindDTO;
import com.example.elderlycare.modules.iot.dto.DeviceDTO;
import com.example.elderlycare.modules.iot.dto.DeviceReportDTO;
import com.example.elderlycare.modules.iot.dto.DeviceUnbindDTO;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import com.example.elderlycare.modules.iot.entity.DeviceData;
import com.example.elderlycare.modules.iot.entity.DeviceEvent;
import com.example.elderlycare.modules.iot.mapper.DeviceBindingMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceEventMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.iot.service.impl.DeviceEventServiceImpl;
import com.example.elderlycare.modules.iot.service.impl.DeviceServiceImpl;
import com.example.elderlycare.modules.iot.service.impl.IotTelemetryServiceImpl;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IotServiceTest {

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DeviceBindingMapper deviceBindingMapper;

    @Mock
    private DeviceDataMapper deviceDataMapper;

    @Mock
    private DeviceEventMapper deviceEventMapper;

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private BedMapper bedMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private BuildingMapper buildingMapper;

    @Mock
    private HealthMetricMapper healthMetricMapper;

    @Mock
    private HealthRecordMapper healthRecordMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOperationLogService sysOperationLogService;

    private ObjectMapper objectMapper;
    private IotAdapterFactory iotAdapterFactory;
    private DeviceServiceImpl deviceService;
    private IotTelemetryServiceImpl telemetryService;
    private DeviceEventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(100L);

        objectMapper = new ObjectMapper();
        DefaultHttpIotAdapter defaultAdapter = new DefaultHttpIotAdapter(objectMapper);
        HuaweiHealthIotAdapter huaweiAdapter = new HuaweiHealthIotAdapter(objectMapper);
        iotAdapterFactory = new IotAdapterFactory(Arrays.asList(defaultAdapter, huaweiAdapter), defaultAdapter);

        deviceService = new DeviceServiceImpl(
                deviceMapper,
                deviceBindingMapper,
                deviceDataMapper,
                deviceEventMapper,
                elderMapper,
                bedMapper,
                roomMapper,
                buildingMapper,
                sysOperationLogService
        );

        telemetryService = new IotTelemetryServiceImpl(
                deviceMapper,
                deviceBindingMapper,
                deviceDataMapper,
                deviceEventMapper,
                elderMapper,
                iotAdapterFactory,
                healthMetricMapper,
                healthRecordMapper,
                objectMapper
        );

        eventService = new DeviceEventServiceImpl(
                deviceEventMapper,
                deviceMapper,
                deviceBindingMapper,
                elderMapper,
                sysUserMapper,
                sysOperationLogService
        );
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("测试设备创建与编码排重校验")
    void testCreateDevice_Success() {
        when(deviceMapper.selectCount(any())).thenReturn(0L);
        when(deviceMapper.insert(any(Device.class))).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(101L);
            return 1;
        });

        DeviceDTO dto = DeviceDTO.builder()
                .deviceCode("DEV_TEST_001")
                .deviceName("智能心率血压手表")
                .deviceType("SMART_WATCH")
                .vendor("HUAWEI")
                .model("B6")
                .status("ACTIVE")
                .build();

        Long id = deviceService.createDevice(dto);
        assertNotNull(id);
        assertEquals(101L, id);

        verify(deviceMapper).insert(argThat(d ->
                "DEV_TEST_001".equals(d.getDeviceCode()) &&
                        "ACTIVE".equals(d.getStatus())
        ));
    }

    @Test
    @DisplayName("测试设备排他绑定：已处于绑定状态的设备禁止再次绑定")
    void testBindDevice_AlreadyBound_ThrowsException() {
        Device device = Device.builder().id(10L).orgId(1L).deviceCode("DEV_WATCH_10").status("ACTIVE").build();
        when(deviceMapper.selectByIdForUpdate(10L)).thenReturn(device);

        DeviceBinding activeBinding = DeviceBinding.builder()
                .id(20L)
                .deviceId(10L)
                .elderId(1L)
                .status("BOUND")
                .build();
        when(deviceBindingMapper.selectActiveByDeviceIdForUpdate(10L)).thenReturn(activeBinding);

        DeviceBindDTO bindDTO = DeviceBindDTO.builder()
                .deviceId(10L)
                .elderId(2L)
                .bindType("ELDER")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> deviceService.bindDevice(bindDTO));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("当前已处于绑定占用状态"));
    }

    @Test
    @DisplayName("测试设备解绑：更新状态与解绑时间并完整保留历史追溯")
    void testUnbindDevice_Success() {
        Device device = Device.builder().id(10L).orgId(1L).deviceCode("DEV_WATCH_10").build();
        when(deviceMapper.selectByIdForUpdate(10L)).thenReturn(device);

        DeviceBinding activeBinding = DeviceBinding.builder()
                .id(20L)
                .deviceId(10L)
                .elderId(1L)
                .status("BOUND")
                .boundAt(LocalDateTime.now().minusDays(5))
                .build();
        when(deviceBindingMapper.selectActiveByDeviceIdForUpdate(10L)).thenReturn(activeBinding);

        DeviceUnbindDTO unbindDTO = DeviceUnbindDTO.builder()
                .deviceId(10L)
                .remark("老人转院治疗，退回随身手环")
                .build();

        deviceService.unbindDevice(unbindDTO);

        verify(deviceBindingMapper).updateById(argThat(b ->
                "UNBOUND".equals(b.getStatus()) && b.getUnboundAt() != null
        ));
    }

    @Test
    @DisplayName("测试模拟设备数据上报可重复调用与设备在线状态刷新")
    void testReportTelemetry_RepeatableCall_Success() {
        Device device = Device.builder()
                .id(1L)
                .orgId(1L)
                .deviceCode("DEV_WATCH_001")
                .vendor("DEFAULT")
                .status("ACTIVE")
                .build();
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(deviceBindingMapper.selectOne(any())).thenReturn(null);

        DeviceReportDTO reportDTO = DeviceReportDTO.builder()
                .deviceCode("DEV_WATCH_001")
                .protocol("STANDARD")
                .metrics(Collections.singletonList(
                        DeviceReportDTO.MetricItemDTO.builder()
                                .metricCode("HEART_RATE")
                                .metricValue(new BigDecimal("75.00"))
                                .unit("bpm")
                                .build()
                ))
                .build();

        // 第一次调用
        telemetryService.reportTelemetry(reportDTO);
        // 第二次重复调用
        telemetryService.reportTelemetry(reportDTO);

        // 验证设备最后在线时间被刷新了 2 次
        verify(deviceMapper, times(2)).updateById(any(Device.class));
        // 验证写入了 2 条采集遥测记录
        verify(deviceDataMapper, times(2)).insert(any(DeviceData.class));
    }

    @Test
    @DisplayName("测试异常指标自动触发设备事件：心率过高自动生成 ABNORMAL_METRIC 告警")
    void testReportTelemetry_AbnormalMetric_GeneratesEvent() {
        Device device = Device.builder()
                .id(1L)
                .orgId(1L)
                .deviceCode("DEV_WATCH_001")
                .vendor("DEFAULT")
                .status("ACTIVE")
                .build();
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(deviceBindingMapper.selectOne(any())).thenReturn(null);

        // 上报极高心率 145 bpm (正常范围 50-100/120)
        DeviceReportDTO reportDTO = DeviceReportDTO.builder()
                .deviceCode("DEV_WATCH_001")
                .protocol("STANDARD")
                .metrics(Collections.singletonList(
                        DeviceReportDTO.MetricItemDTO.builder()
                                .metricCode("HEART_RATE")
                                .metricValue(new BigDecimal("145.00"))
                                .unit("bpm")
                                .build()
                ))
                .build();

        telemetryService.reportTelemetry(reportDTO);

        // 验证触发了 DeviceEvent 写入，且事件类型为 ABNORMAL_METRIC，级别 HIGH
        verify(deviceEventMapper).insert(argThat(ev ->
                "ABNORMAL_METRIC".equals(ev.getEventType()) &&
                        "HIGH".equals(ev.getSeverity()) &&
                        "OPEN".equals(ev.getEventStatus())
        ));
    }

    @Test
    @DisplayName("测试低电量与一键 SOS 触发设备事件")
    void testReportTelemetry_LowBatteryAndSOS_GeneratesEvents() {
        Device device = Device.builder()
                .id(2L)
                .orgId(1L)
                .deviceCode("DEV_SOS_001")
                .vendor("DEFAULT")
                .status("ACTIVE")
                .build();
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(deviceBindingMapper.selectOne(any())).thenReturn(null);

        // 上报低电量 10% 并且触发 SOS 紧急求助
        DeviceReportDTO reportDTO = DeviceReportDTO.builder()
                .deviceCode("DEV_SOS_001")
                .protocol("STANDARD")
                .battery(new BigDecimal("10"))
                .sos(true)
                .build();

        telemetryService.reportTelemetry(reportDTO);

        // 验证分别生成了 LOW_BATTERY 和 SOS 两个事件
        verify(deviceEventMapper).insert(argThat(ev -> "LOW_BATTERY".equals(ev.getEventType())));
        verify(deviceEventMapper).insert(argThat(ev -> "SOS".equals(ev.getEventType()) && "CRITICAL".equals(ev.getSeverity())));
    }

    @Test
    @DisplayName("测试厂商协议适配器隔离：华为私有 JSON 格式被正确解析规整")
    void testHuaweiAdapter_Normalization() {
        Device device = Device.builder()
                .id(3L)
                .orgId(1L)
                .deviceCode("DEV_HW_001")
                .vendor("HUAWEI")
                .status("ACTIVE")
                .build();
        when(deviceMapper.selectOne(any())).thenReturn(device);
        when(deviceBindingMapper.selectOne(any())).thenReturn(null);

        String huaweiJson = "{\"services\": [{\"service_id\": \"vital_signs\", \"properties\": {\"heart_rate\": 78, \"spo2\": 99, \"battery_pct\": 85}}]}";

        DeviceReportDTO reportDTO = DeviceReportDTO.builder()
                .deviceCode("DEV_HW_001")
                .protocol("HUAWEI_HEALTH")
                .rawPayload(huaweiJson)
                .build();

        telemetryService.reportTelemetry(reportDTO);

        // 验证华为协议被解析为 HEART_RATE, SPO2, BATTERY 三条标准指标
        verify(deviceDataMapper).insert(argThat(dd -> "HEART_RATE".equals(dd.getMetricCode()) && dd.getMetricValue().compareTo(new BigDecimal("78")) == 0));
        verify(deviceDataMapper).insert(argThat(dd -> "SPO2".equals(dd.getMetricCode()) && dd.getMetricValue().compareTo(new BigDecimal("99")) == 0));
        verify(deviceDataMapper).insert(argThat(dd -> "BATTERY".equals(dd.getMetricCode()) && dd.getMetricValue().compareTo(new BigDecimal("85")) == 0));
    }
}
