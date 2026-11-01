package com.example.elderlycare.modules.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.iot.entity.Device;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.iot.mapper.DeviceMapper;
import com.example.elderlycare.modules.report.dto.ExportTaskCreateDTO;
import com.example.elderlycare.modules.report.dto.ReportQueryDTO;
import com.example.elderlycare.modules.report.entity.ReportExportTask;
import com.example.elderlycare.modules.report.mapper.ReportExportTaskMapper;
import com.example.elderlycare.modules.report.service.impl.DashboardServiceImpl;
import com.example.elderlycare.modules.report.service.impl.ReportServiceImpl;
import com.example.elderlycare.modules.report.vo.BusinessReportResultVO;
import com.example.elderlycare.modules.report.vo.DashboardStatsVO;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportExportTaskMapper exportTaskMapper;

    @Mock
    private AdmissionMapper admissionMapper;

    @Mock
    private BedMapper bedMapper;

    @Mock
    private ServiceOrderMapper serviceOrderMapper;

    @Mock
    private CareTaskMapper careTaskMapper;

    @Mock
    private HealthAlertMapper healthAlertMapper;

    @Mock
    private BillingMapper billingMapper;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private RiskEventMapper riskEventMapper;

    @Mock
    private DeviceDataMapper deviceDataMapper;

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOperationLogService sysOperationLogService;

    private DashboardServiceImpl dashboardService;
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(100L);

        dashboardService = new DashboardServiceImpl(
                admissionMapper,
                bedMapper,
                serviceOrderMapper,
                careTaskMapper,
                healthAlertMapper,
                billingMapper,
                riskEventMapper,
                deviceDataMapper
        );

        reportService = new ReportServiceImpl(
                exportTaskMapper,
                admissionMapper,
                bedMapper,
                serviceOrderMapper,
                careTaskMapper,
                healthAlertMapper,
                billingMapper,
                deviceMapper,
                riskEventMapper,
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
    @DisplayName("测试运营看板指标聚合：在住人数、床位使用率、工单、护理、预警、收入准确计算")
    void testGetDashboardStats_Success() {
        when(admissionMapper.selectCount(any())).thenReturn(25L);
        when(bedMapper.selectCount(any()))
                .thenReturn(50L) // 总床位 50
                .thenReturn(25L); // 占用床位 25 -> 占用率 50.00%
        when(serviceOrderMapper.selectCount(any())).thenReturn(8L);
        when(careTaskMapper.selectCount(any()))
                .thenReturn(40L) // 今日任务 40
                .thenReturn(36L); // 今日完成 36 -> 完成率 90.00%
        when(healthAlertMapper.selectCount(any())).thenReturn(3L);

        Billing b1 = Billing.builder().paidAmount(new BigDecimal("12000.00")).status("PAID").build();
        Billing b2 = Billing.builder().paidAmount(new BigDecimal("8500.50")).status("PARTIAL").build();
        when(billingMapper.selectList(any())).thenReturn(Arrays.asList(b1, b2));

        RiskEvent r1 = RiskEvent.builder().riskLevel("CRITICAL").status("REPORTED").build();
        RiskEvent r2 = RiskEvent.builder().riskLevel("HIGH").status("PROCESSING").build();
        when(riskEventMapper.selectList(any())).thenReturn(Arrays.asList(r1, r2));

        when(deviceDataMapper.selectCount(any())).thenReturn(380L);

        DashboardStatsVO stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(25L, stats.getActiveElderCount());
        assertEquals(50L, stats.getTotalBedCount());
        assertEquals(25L, stats.getOccupiedBedCount());
        assertEquals(new BigDecimal("50.00"), stats.getBedOccupancyRate());
        assertEquals(8L, stats.getPendingOrderCount());
        assertEquals(new BigDecimal("90.00"), stats.getCareCompletionRate());
        assertEquals(3L, stats.getHealthAlertCount());
        assertEquals(new BigDecimal("20500.50"), stats.getCurrentMonthRevenue());
        assertEquals(2L, stats.getOpenRiskEventCount());
        assertEquals(380L, stats.getTodayTelemetryCount());
        assertEquals(7, stats.getAdmissionTrend().size());
        assertEquals(7, stats.getCareTaskTrend().size());
    }

    @Test
    @DisplayName("测试异步导出任务创建与任务单号生成")
    void testCreateExportTask_Success() {
        when(exportTaskMapper.insert(any(ReportExportTask.class))).thenAnswer(inv -> {
            ReportExportTask t = inv.getArgument(0);
            t.setId(101L);
            return 1;
        });

        ExportTaskCreateDTO dto = ExportTaskCreateDTO.builder()
                .reportType("FINANCE_BILLING")
                .customFileName("2026年10月度财务账单报表.csv")
                .filterJson("{\"billMonth\": \"2026-10\"}")
                .build();

        Long taskId = reportService.createExportTask(dto);

        assertNotNull(taskId);
        assertEquals(101L, taskId);

        verify(exportTaskMapper).insert(argThat(t ->
                "FINANCE_BILLING".equals(t.getReportType()) &&
                        "PENDING".equals(t.getStatus()) &&
                        t.getTaskNo().startsWith("EXP") &&
                        t.getExpireAt() != null
        ));
    }

    @Test
    @DisplayName("测试跨机构导出文件下载隔离保护")
    void testDownloadExportFile_CrossOrg_ThrowsException() {
        ReportExportTask task = ReportExportTask.builder()
                .id(88L)
                .orgId(2L) // 机构2，当前上下文为机构1
                .status("SUCCESS")
                .fileName("test.csv")
                .expireAt(LocalDateTime.now().plusDays(3))
                .build();
        when(exportTaskMapper.selectById(88L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.downloadExportFile(88L));
        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("测试过期导出文件下载拦截")
    void testDownloadExportFile_Expired_ThrowsException() {
        ReportExportTask task = ReportExportTask.builder()
                .id(88L)
                .orgId(1L)
                .status("SUCCESS")
                .fileName("expired_report.csv")
                .expireAt(LocalDateTime.now().minusDays(1)) // 1 天前已过期
                .build();
        when(exportTaskMapper.selectById(88L)).thenReturn(task);

        BusinessException ex = assertThrows(BusinessException.class, () -> reportService.downloadExportFile(88L));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("超过 7 天保存有效期"));
    }

    @Test
    @DisplayName("测试业务报表查询构建（入住报表）")
    void testQueryBusinessReport_Admission() {
        Admission a1 = Admission.builder()
                .id(1L)
                .admissionNo("ADM20261001001")
                .elderId(10L)
                .admissionDate(LocalDate.of(2026, 10, 1))
                .status("CHECKED_IN")
                .depositAmount(new BigDecimal("5000.00"))
                .build();

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admission> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        page.setRecords(Collections.singletonList(a1));
        page.setTotal(1);

        when(admissionMapper.selectPage(any(), any())).thenReturn(page);

        Elder elder = new Elder();
        elder.setId(10L);
        elder.setName("张三爷爷");
        when(elderMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(elder));

        ReportQueryDTO query = ReportQueryDTO.builder()
                .reportType("ADMISSION")
                .pageNum(1)
                .pageSize(10)
                .build();

        BusinessReportResultVO result = reportService.queryBusinessReport(query);

        assertNotNull(result);
        assertEquals("ADMISSION", result.getReportType());
        assertFalse(result.getColumns().isEmpty());
        assertEquals(1, result.getData().getList().size());
        assertEquals("张三爷爷", result.getData().getList().get(0).get("elderName"));
    }
}
