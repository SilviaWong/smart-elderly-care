package com.example.elderlycare.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.util.SecurityCryptoUtil;
import com.example.elderlycare.modules.admission.dto.AdmissionDTO;
import com.example.elderlycare.modules.admission.dto.DischargeDTO;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedChangeLogMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.admission.service.impl.AdmissionServiceImpl;
import com.example.elderlycare.modules.care.dto.CareTaskExecuteDTO;
import com.example.elderlycare.modules.care.entity.CarePlan;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.mapper.CarePlanMapper;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.care.mapper.CareTaskRecordMapper;
import com.example.elderlycare.modules.care.service.impl.CareTaskServiceImpl;
import com.example.elderlycare.modules.dining.dto.MealOrderCreateDTO;
import com.example.elderlycare.modules.dining.entity.MealDish;
import com.example.elderlycare.modules.dining.entity.MealMenu;
import com.example.elderlycare.modules.dining.entity.MealOrder;
import com.example.elderlycare.modules.dining.mapper.MealDishMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderMapper;
import com.example.elderlycare.modules.dining.service.impl.MealOrderServiceImpl;
import com.example.elderlycare.modules.elder.dto.ElderCreateDTO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderAssessmentMapper;
import com.example.elderlycare.modules.elder.mapper.ElderContactMapper;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.impl.ElderServiceImpl;
import com.example.elderlycare.modules.finance.dto.PaymentCreateDTO;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.entity.Payment;
import com.example.elderlycare.modules.finance.mapper.BillingItemMapper;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.finance.mapper.PaymentMapper;
import com.example.elderlycare.modules.finance.mapper.RefundMapper;
import com.example.elderlycare.modules.finance.service.impl.PaymentServiceImpl;
import com.example.elderlycare.modules.health.dto.HealthRecordCreateDTO;
import com.example.elderlycare.modules.health.entity.HealthAlertRule;
import com.example.elderlycare.modules.health.entity.HealthRecord;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.health.mapper.HealthAlertRuleMapper;
import com.example.elderlycare.modules.health.mapper.HealthMetricMapper;
import com.example.elderlycare.modules.health.mapper.HealthRecordMapper;
import com.example.elderlycare.modules.health.service.impl.HealthRecordServiceImpl;
import com.example.elderlycare.modules.iot.dto.DeviceBindDTO;
import com.example.elderlycare.modules.iot.dto.TelemetryReportDTO;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import com.example.elderlycare.modules.iot.mapper.DeviceBindingMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceEventMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.iot.service.impl.DeviceServiceImpl;
import com.example.elderlycare.modules.iot.service.impl.IotTelemetryServiceImpl;
import com.example.elderlycare.modules.regulatory.adapter.impl.DefaultFileExportSubmissionAdapter;
import com.example.elderlycare.modules.regulatory.dto.FieldMappingDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchGenerateDTO;
import com.example.elderlycare.modules.regulatory.dto.RegulatoryBatchSubmitDTO;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryTemplate;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryBatchMapper;
import com.example.elderlycare.modules.regulatory.mapper.RegulatoryTemplateMapper;
import com.example.elderlycare.modules.regulatory.service.impl.RegulatoryBatchServiceImpl;
import com.example.elderlycare.modules.regulatory.vo.RegulatoryBatchVO;
import com.example.elderlycare.modules.risk.dto.RiskEventCreateDTO;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.mapper.RiskEventLogMapper;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.risk.service.impl.RiskEventServiceImpl;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderAssignDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderCreateDTO;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceItemMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderLogMapper;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import com.example.elderlycare.modules.serviceorder.service.impl.ServiceOrderServiceImpl;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 智慧养老运营管理系统 - 端到端长者全生命周期集成测试
 * 覆盖：建档 -> 入住床位 -> 服务工单 -> 健康监测与预警 -> 等级护理任务 -> 餐饮订餐
 *       -> 财务收费 -> 物联遥测 -> 安全风险 -> 退住与床位释放 -> 监管报送
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EndToEndIntegrationTest {

    // 核心组件与基础设施
    private SecurityCryptoUtil securityCryptoUtil;
    private ObjectMapper objectMapper;

    // Mappers
    @Mock private ElderMapper elderMapper;
    @Mock private ElderContactMapper elderContactMapper;
    @Mock private ElderAssessmentMapper elderAssessmentMapper;
    @Mock private BedMapper bedMapper;
    @Mock private RoomMapper roomMapper;
    @Mock private AdmissionMapper admissionMapper;
    @Mock private BedChangeLogMapper bedChangeLogMapper;
    @Mock private ServiceOrderMapper serviceOrderMapper;
    @Mock private ServiceItemMapper serviceItemMapper;
    @Mock private ServiceOrderLogMapper serviceOrderLogMapper;
    @Mock private HealthRecordMapper healthRecordMapper;
    @Mock private HealthMetricMapper healthMetricMapper;
    @Mock private HealthAlertRuleMapper healthAlertRuleMapper;
    @Mock private HealthAlertMapper healthAlertMapper;
    @Mock private CareTaskMapper careTaskMapper;
    @Mock private CarePlanMapper carePlanMapper;
    @Mock private CareTaskRecordMapper careTaskRecordMapper;
    @Mock private MealOrderMapper mealOrderMapper;
    @Mock private MealOrderItemMapper mealOrderItemMapper;
    @Mock private MealDishMapper mealDishMapper;
    @Mock private MealMenuMapper mealMenuMapper;
    @Mock private MealMenuItemMapper mealMenuItemMapper;
    @Mock private BillingMapper billingMapper;
    @Mock private BillingItemMapper billingItemMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private RefundMapper refundMapper;
    @Mock private DeviceMapper deviceMapper;
    @Mock private DeviceBindingMapper deviceBindingMapper;
    @Mock private DeviceDataMapper deviceDataMapper;
    @Mock private DeviceEventMapper deviceEventMapper;
    @Mock private RiskEventMapper riskEventMapper;
    @Mock private RiskEventLogMapper riskEventLogMapper;
    @Mock private RegulatoryTemplateMapper regulatoryTemplateMapper;
    @Mock private RegulatoryBatchMapper regulatoryBatchMapper;
    @Mock private SysUserMapper sysUserMapper;
    @Mock private SysOperationLogService sysOperationLogService;

    // Services
    private ElderServiceImpl elderService;
    private AdmissionServiceImpl admissionService;
    private ServiceOrderServiceImpl serviceOrderService;
    private HealthRecordServiceImpl healthRecordService;
    private CareTaskServiceImpl careTaskService;
    private MealOrderServiceImpl mealOrderService;
    private PaymentServiceImpl paymentService;
    private DeviceServiceImpl deviceService;
    private IotTelemetryServiceImpl telemetryService;
    private RiskEventServiceImpl riskEventService;
    private RegulatoryBatchServiceImpl regulatoryBatchService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(1L);
        OrgContextHolder.setUsername("admin");

        securityCryptoUtil = new SecurityCryptoUtil("ElderlyCareAesSecretKey32BytesLong!");
        objectMapper = new ObjectMapper();

        elderService = new ElderServiceImpl(elderMapper, elderContactMapper, elderAssessmentMapper, securityCryptoUtil);
        admissionService = new AdmissionServiceImpl(admissionMapper, bedMapper, roomMapper, bedChangeLogMapper, elderMapper);
        serviceOrderService = new ServiceOrderServiceImpl(serviceOrderMapper, serviceItemMapper, elderMapper, serviceOrderLogMapper, sysOperationLogService);
        healthRecordService = new HealthRecordServiceImpl(healthRecordMapper, healthMetricMapper, healthAlertRuleMapper, healthAlertMapper, elderMapper);
        careTaskService = new CareTaskServiceImpl(careTaskMapper, carePlanMapper, careTaskRecordMapper, elderMapper, sysOperationLogService);
        mealOrderService = new MealOrderServiceImpl(mealOrderMapper, mealOrderItemMapper, mealDishMapper, mealMenuMapper, mealMenuItemMapper, elderMapper);
        paymentService = new PaymentServiceImpl(paymentMapper, billingMapper, refundMapper, sysOperationLogService);
        deviceService = new DeviceServiceImpl(deviceMapper, deviceBindingMapper, deviceDataMapper, elderMapper, bedMapper);
        telemetryService = new IotTelemetryServiceImpl(deviceMapper, deviceBindingMapper, deviceDataMapper, deviceEventMapper);
        riskEventService = new RiskEventServiceImpl(riskEventMapper, riskEventLogMapper, elderMapper, sysOperationLogService);
        regulatoryBatchService = new RegulatoryBatchServiceImpl(
                regulatoryBatchMapper,
                regulatoryTemplateMapper,
                elderMapper,
                elderContactMapper,
                admissionMapper,
                bedMapper,
                roomMapper,
                null,
                careTaskMapper,
                riskEventMapper,
                sysUserMapper,
                securityCryptoUtil,
                new DefaultFileExportSubmissionAdapter(),
                objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("端到端闭环验证：长者建档 -> 入住占床 -> 医护工单 -> 异常体征预警 -> 照护任务 -> 订餐收款 -> 退住释放床位 -> 监管报送")
    void testElderEndToEndLifecycle() throws Exception {
        // =======================================================
        // 步骤 1：长者建档 (Elder Registration)
        // =======================================================
        when(elderMapper.selectCount(any())).thenReturn(0L);
        doAnswer(inv -> {
            Elder e = inv.getArgument(0);
            e.setId(101L);
            return 1;
        }).when(elderMapper).insert(any(Elder.class));

        ElderCreateDTO elderDTO = ElderCreateDTO.builder()
                .name("钱老伯")
                .gender("MALE")
                .birthDate(LocalDate.of(1946, 7, 18))
                .idType("ID_CARD")
                .idNo("110101194607181234")
                .phone("13812345678")
                .mobilityLevel("PARTIAL")
                .remark("需要日常慢病照料")
                .build();

        Long elderId = elderService.createElder(elderDTO);
        assertEquals(101L, elderId);

        // 验证身份证已对称加密且哈希索引建立
        verify(elderMapper).insert(argThat(elder -> {
            assertNotNull(elder.getIdNoCiphertext());
            assertNotNull(elder.getIdNoHash());
            assertNotEquals("110101194607181234", elder.getIdNoCiphertext());
            assertEquals("110101194607181234", securityCryptoUtil.decrypt(elder.getIdNoCiphertext()));
            return true;
        }));

        Elder savedElder = Elder.builder()
                .id(101L)
                .orgId(1L)
                .elderNo("ELD202611-101")
                .name("钱老伯")
                .gender("MALE")
                .birthDate(LocalDate.of(1946, 7, 18))
                .idNoCiphertext(securityCryptoUtil.encrypt("110101194607181234"))
                .status("PENDING")
                .build();
        when(elderMapper.selectById(101L)).thenReturn(savedElder);

        // =======================================================
        // 步骤 2：分配空闲床位并办理入住 (Admission & Bed Allocation)
        // =======================================================
        Bed vacantBed = Bed.builder()
                .id(501L)
                .orgId(1L)
                .bedNo("B-301-1")
                .roomId(201L)
                .bedType("CARE")
                .status("VACANT")
                .build();
        when(bedMapper.selectByIdForUpdate(501L)).thenReturn(vacantBed);
        when(admissionMapper.selectActiveByElderId(101L, 1L)).thenReturn(null);

        doAnswer(inv -> {
            Admission a = inv.getArgument(0);
            a.setId(801L);
            return 1;
        }).when(admissionMapper).insert(any(Admission.class));

        AdmissionDTO checkInDTO = AdmissionDTO.builder()
                .elderId(101L)
                .bedId(501L)
                .checkInDate(LocalDate.now())
                .careLevelName("二级护理")
                .monthlyFee(new BigDecimal("4500.00"))
                .build();

        Long admissionId = admissionService.checkIn(checkInDTO);
        assertEquals(801L, admissionId);

        // 验证床位状态变为 OCCUPIED
        assertEquals("OCCUPIED", vacantBed.getStatus());
        verify(bedMapper).updateById(vacantBed);

        Admission activeAdmission = Admission.builder()
                .id(801L)
                .orgId(1L)
                .admissionNo("ADM202611-001")
                .elderId(101L)
                .elderName("钱老伯")
                .bedId(501L)
                .bedNo("B-301-1")
                .roomNo("301")
                .status("CHECKED_IN")
                .checkInDate(LocalDate.now())
                .careLevelName("二级护理")
                .build();
        when(admissionMapper.selectById(801L)).thenReturn(activeAdmission);
        when(admissionMapper.selectActiveByElderId(101L, 1L)).thenReturn(activeAdmission);

        // =======================================================
        // 步骤 3：发起陪护服务工单与执行闭环 (Service Order Dispatch & Flow)
        // =======================================================
        doAnswer(inv -> {
            ServiceOrder o = inv.getArgument(0);
            o.setId(901L);
            return 1;
        }).when(serviceOrderMapper).insert(any(ServiceOrder.class));

        ServiceOrderCreateDTO orderDTO = ServiceOrderCreateDTO.builder()
                .elderId(101L)
                .orderType("APPOINTMENT")
                .expectedTime(LocalDateTime.now().plusHours(2))
                .remark("协助前往医务室常规健康巡诊")
                .build();

        Long orderId = serviceOrderService.createServiceOrder(orderDTO);
        assertEquals(901L, orderId);

        ServiceOrder order = ServiceOrder.builder()
                .id(901L)
                .orgId(1L)
                .orderNo("SO20261101-001")
                .elderId(101L)
                .elderName("钱老伯")
                .status("PENDING")
                .build();
        when(serviceOrderMapper.selectById(901L)).thenReturn(order);

        // 派单给护工 (CAREGIVER)
        serviceOrderService.assignOrder(901L, ServiceOrderAssignDTO.builder()
                .caregiverId(2L)
                .caregiverName("王护士")
                .build());
        assertEquals("ASSIGNED", order.getStatus());

        // 护工接单执行
        serviceOrderService.startOrder(901L);
        assertEquals("IN_PROGRESS", order.getStatus());

        // 执行完成
        serviceOrderService.completeOrder(901L, null);
        assertEquals("COMPLETED", order.getStatus());

        // 主管确认归档
        serviceOrderService.confirmOrder(901L);
        assertEquals("CONFIRMED", order.getStatus());

        // =======================================================
        // 步骤 4：生命体征采集与预警触发 (Health Monitoring & Alert Rule)
        // =======================================================
        HealthAlertRule highBpRule = HealthAlertRule.builder()
                .id(1L)
                .orgId(1L)
                .metricCode("BLOOD_PRESSURE_SYS")
                .thresholdMax(new BigDecimal("140"))
                .alertLevel("HIGH")
                .enabled(1)
                .build();
        when(healthAlertRuleMapper.selectList(any())).thenReturn(Collections.singletonList(highBpRule));

        HealthRecordCreateDTO healthDTO = HealthRecordCreateDTO.builder()
                .elderId(101L)
                .metricCode("BLOOD_PRESSURE_SYS")
                .metricValue(new BigDecimal("165.0")) // 超过 140 阈值
                .recordedAt(LocalDateTime.now())
                .recorderId(2L)
                .build();

        doAnswer(inv -> {
            HealthRecord hr = inv.getArgument(0);
            hr.setId(301L);
            return 1;
        }).when(healthRecordMapper).insert(any(HealthRecord.class));

        Long recordId = healthRecordService.createHealthRecord(healthDTO);
        assertEquals(301L, recordId);
        // 验证预警生成被触发
        verify(healthAlertMapper).insert(any());

        // =======================================================
        // 步骤 5：护理等级任务执行 (Care Task Execution)
        // =======================================================
        CareTask careTask = CareTask.builder()
                .id(401L)
                .orgId(1L)
                .elderId(101L)
                .elderName("钱老伯")
                .taskName("慢病血压晨检与服药协助")
                .planDate(LocalDate.now())
                .status("PENDING")
                .build();
        when(careTaskMapper.selectById(401L)).thenReturn(careTask);

        careTaskService.executeTask(401L, CareTaskExecuteDTO.builder()
                .status("COMPLETED")
                .feedback("已服药并测量血压，长者状态稳定")
                .build());
        assertEquals("COMPLETED", careTask.getStatus());
        verify(careTaskRecordMapper).insert(any());

        // =======================================================
        // 步骤 6：餐饮订餐并快照价格 (Dining & Price Snapshot)
        // =======================================================
        MealDish dish = MealDish.builder()
                .id(11L)
                .dishName("清蒸鲈鱼配时蔬")
                .price(new BigDecimal("28.00"))
                .isAvailable(1)
                .build();
        when(mealDishMapper.selectById(11L)).thenReturn(dish);

        MealMenu menu = MealMenu.builder()
                .id(22L)
                .menuDate(LocalDate.now())
                .mealType("LUNCH")
                .status("PUBLISHED")
                .build();
        when(mealMenuMapper.selectById(22L)).thenReturn(menu);

        doAnswer(inv -> {
            MealOrder mo = inv.getArgument(0);
            mo.setId(601L);
            return 1;
        }).when(mealOrderMapper).insert(any(MealOrder.class));

        MealOrderCreateDTO mealDTO = MealOrderCreateDTO.builder()
                .elderId(101L)
                .menuId(22L)
                .orderDate(LocalDate.now())
                .items(Collections.singletonList(
                        MealOrderCreateDTO.MealOrderItemDTO.builder()
                                .dishId(11L)
                                .quantity(1)
                                .build()
                ))
                .build();

        Long mealOrderId = mealOrderService.createMealOrder(mealDTO);
        assertEquals(601L, mealOrderId);
        // 验证订单快照包含了菜品价格快照 28.00 元
        verify(mealOrderItemMapper).insert(argThat(item -> {
            assertEquals(new BigDecimal("28.00"), item.getUnitPrice());
            return true;
        }));

        // =======================================================
        // 步骤 7：账单出账与模拟收款 (Billing & Idempotent Payment)
        // =======================================================
        Billing billing = Billing.builder()
                .id(701L)
                .orgId(1L)
                .billNo("BILL202611-001")
                .elderId(101L)
                .totalAmount(new BigDecimal("4500.00"))
                .paidAmount(BigDecimal.ZERO)
                .status("UNPAID")
                .build();
        when(billingMapper.selectById(701L)).thenReturn(billing);

        doAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(991L);
            return 1;
        }).when(paymentMapper).insert(any(Payment.class));

        PaymentCreateDTO payDTO = PaymentCreateDTO.builder()
                .billId(701L)
                .payMethod("ALIPAY")
                .payAmount(new BigDecimal("4500.00"))
                .idempotentKey("IDEMP-PAY-20261101-001")
                .build();

        Long paymentId = paymentService.createPayment(payDTO);
        assertEquals(991L, paymentId);
        // 账单完成缴费更新为 PAID
        assertEquals("PAID", billing.getStatus());
        assertEquals(new BigDecimal("4500.00"), billing.getPaidAmount());
        verify(billingMapper).updateById(billing);

        // =======================================================
        // 步骤 8：物联设备绑定与数据采集 (IoT Device Binding & Telemetry)
        // =======================================================
        Device watch = Device.builder()
                .id(88L)
                .orgId(1L)
                .deviceCode("WATCH-001")
                .deviceName("健康监测智能手表")
                .deviceType("SMART_WATCH")
                .status("ONLINE")
                .build();
        when(deviceMapper.selectById(88L)).thenReturn(watch);
        when(deviceBindingMapper.selectCount(any())).thenReturn(0L);

        deviceService.bindDevice(DeviceBindDTO.builder()
                .deviceId(88L)
                .elderId(101L)
                .build());
        verify(deviceBindingMapper).insert(any(DeviceBinding.class));

        // 模拟上报心率遥测数据
        when(deviceMapper.selectOne(any())).thenReturn(watch);
        DeviceBinding binding = DeviceBinding.builder()
                .id(1L)
                .deviceId(88L)
                .elderId(101L)
                .status("BOUND")
                .build();
        when(deviceBindingMapper.selectOne(any())).thenReturn(binding);

        TelemetryReportDTO telemetryDTO = TelemetryReportDTO.builder()
                .deviceCode("WATCH-001")
                .metricCode("HEART_RATE")
                .rawValue("76")
                .standardValue(new BigDecimal("76"))
                .unit("bpm")
                .collectedAt(LocalDateTime.now())
                .build();
        telemetryService.receiveTelemetry(telemetryDTO);
        verify(deviceDataMapper).insert(any());

        // =======================================================
        // 步骤 9：安全风险隐患登记与闭环 (Risk Event Life Cycle)
        // =======================================================
        doAnswer(inv -> {
            RiskEvent re = inv.getArgument(0);
            re.setId(55L);
            return 1;
        }).when(riskEventMapper).insert(any(RiskEvent.class));

        RiskEventCreateDTO riskDTO = RiskEventCreateDTO.builder()
                .elderId(101L)
                .eventType("FALL")
                .riskLevel("MEDIUM")
                .location("301卫生间地面湿滑")
                .description("长者起夜时轻微滑倒，无明显外伤")
                .occurredAt(LocalDateTime.now())
                .reporterName("值班护工小李")
                .build();

        Long riskId = riskEventService.createRiskEvent(riskDTO);
        assertEquals(55L, riskId);

        RiskEvent risk = RiskEvent.builder()
                .id(55L)
                .orgId(1L)
                .eventNo("RSK202611-001")
                .status("PENDING")
                .build();
        when(riskEventMapper.selectById(55L)).thenReturn(risk);

        // 分派并闭环
        riskEventService.assignRiskEvent(55L, 2L, "王护士", "协助检查身体并加铺防滑地垫");
        assertEquals("PROCESSING", risk.getStatus());

        riskEventService.closeRiskEvent(55L, "长者体征正常，地面防滑措施已完备验收", 1L);
        assertEquals("CLOSED", risk.getStatus());

        // =======================================================
        // 步骤 10：长者退住与床位释放 (Discharge & Bed Release)
        // =======================================================
        Bed occupiedBed = Bed.builder()
                .id(501L)
                .orgId(1L)
                .bedNo("B-301-1")
                .status("OCCUPIED")
                .build();
        when(bedMapper.selectByIdForUpdate(501L)).thenReturn(occupiedBed);
        when(admissionMapper.selectActiveByElderId(101L, 1L)).thenReturn(activeAdmission);

        DischargeDTO dischargeDTO = DischargeDTO.builder()
                .elderId(101L)
                .dischargeDate(LocalDate.now())
                .reason("长者康复回家休养")
                .build();

        admissionService.discharge(dischargeDTO);

        // 验证床位被成功释放为空闲 VACANT
        assertEquals("VACANT", occupiedBed.getStatus());
        verify(bedMapper, atLeastOnce()).updateById(occupiedBed);

        // =======================================================
        // 步骤 11：合规监管报送批次生成与提交 (Regulatory Batch & Submission)
        // =======================================================
        FieldMappingDTO mapping = FieldMappingDTO.builder()
                .targetAuthority("民政局老龄健康处")
                .businessDomain("ELDER")
                .fileFormat("CSV")
                .fields(Arrays.asList(
                        FieldMappingDTO.FieldItem.builder().sourceField("name").targetField("长者姓名").required(true).sortOrder(1).build(),
                        FieldMappingDTO.FieldItem.builder().sourceField("idCard").targetField("公民身份号码").required(true).validationRule("ID_CARD").sortOrder(2).build()
                ))
                .build();

        RegulatoryTemplate template = RegulatoryTemplate.builder()
                .id(1L)
                .orgId(1L)
                .templateCode("CIVIL_ELDER_RECORD")
                .templateName("民政在住长者备案台账")
                .versionNo("v1.0")
                .fieldMappingJson(objectMapper.writeValueAsString(mapping))
                .status("ACTIVE")
                .build();
        when(regulatoryTemplateMapper.selectById(1L)).thenReturn(template);

        // 模拟已建档长者被报送抽取
        when(elderMapper.selectList(any())).thenReturn(Collections.singletonList(savedElder));

        doAnswer(inv -> {
            RegulatoryBatch rb = inv.getArgument(0);
            rb.setId(77L);
            return 1;
        }).when(regulatoryBatchMapper).insert(any(RegulatoryBatch.class));

        Long batchId = regulatoryBatchService.generateBatch(RegulatoryBatchGenerateDTO.builder()
                .templateId(1L)
                .periodStart(LocalDate.now().minusMonths(1))
                .periodEnd(LocalDate.now())
                .remark("端到端退住长者历史归档报送")
                .build());
        assertEquals(77L, batchId);

        // 正式提交报送
        RegulatoryBatch batch = RegulatoryBatch.builder()
                .id(77L)
                .orgId(1L)
                .batchNo("REG20261101-77")
                .status("VALIDATED")
                .build();
        when(regulatoryBatchMapper.selectByIdForUpdate(77L)).thenReturn(batch);

        regulatoryBatchService.submitBatch(77L, RegulatoryBatchSubmitDTO.builder()
                .channel("DEFAULT_FILE_EXPORT")
                .externalReceiptNo("GOV-E2E-202611-999")
                .submissionNotes("全链路自动化集成测试通过")
                .build());

        assertEquals("SUBMITTED", batch.getStatus());
        assertTrue(batch.getResponseDesc().contains("GOV-E2E-202611-999"));
    }
}
