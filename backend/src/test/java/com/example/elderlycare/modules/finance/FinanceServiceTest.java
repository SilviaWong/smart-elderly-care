package com.example.elderlycare.modules.finance;

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
import com.example.elderlycare.modules.finance.dto.*;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.entity.BillingItem;
import com.example.elderlycare.modules.finance.entity.PaymentRecord;
import com.example.elderlycare.modules.finance.entity.RefundRecord;
import com.example.elderlycare.modules.finance.mapper.BillingItemMapper;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.finance.mapper.PaymentRecordMapper;
import com.example.elderlycare.modules.finance.mapper.RefundRecordMapper;
import com.example.elderlycare.modules.finance.service.impl.BillingServiceImpl;
import com.example.elderlycare.modules.finance.service.impl.PaymentServiceImpl;
import com.example.elderlycare.modules.finance.service.impl.RefundServiceImpl;
import com.example.elderlycare.modules.finance.vo.FinanceSummaryVO;
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
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock
    private BillingMapper billingMapper;

    @Mock
    private BillingItemMapper billingItemMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private RefundRecordMapper refundRecordMapper;

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
    private BillingServiceImpl billingService;

    private PaymentServiceImpl paymentService;
    private RefundServiceImpl refundService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(100L);

        // 手动组装依赖以支持跨服务方法测试
        paymentService = new PaymentServiceImpl(
                paymentRecordMapper,
                refundRecordMapper,
                billingMapper,
                billingService,
                elderMapper,
                admissionMapper,
                bedMapper,
                roomMapper,
                buildingMapper,
                sysUserMapper,
                sysOperationLogService
        );

        refundService = new RefundServiceImpl(
                refundRecordMapper,
                paymentRecordMapper,
                billingMapper,
                billingService,
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
    @DisplayName("测试账单生成：行项目金额严格四舍五入并正确合计总额")
    void testCreateBilling_Success() {
        Elder elder = new Elder();
        elder.setId(10L);
        elder.setOrgId(1L);
        elder.setName("张爷爷");
        when(elderMapper.selectById(10L)).thenReturn(elder);

        Admission admission = new Admission();
        admission.setId(20L);
        admission.setElderId(10L);
        admission.setStatus("CHECKED_IN");
        when(admissionMapper.selectOne(any())).thenReturn(admission);

        when(billingMapper.insert(any(Billing.class))).thenAnswer(invocation -> {
            Billing b = invocation.getArgument(0);
            b.setId(1001L);
            return 1;
        });
        when(billingItemMapper.insert(any(BillingItem.class))).thenReturn(1);

        BillingItemDTO item1 = BillingItemDTO.builder()
                .feeItemId(1L)
                .itemNameSnapshot("豪华双人间床位费")
                .periodStart(LocalDate.of(2026, 11, 1))
                .periodEnd(LocalDate.of(2026, 11, 30))
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("2200.00"))
                .build();

        BillingItemDTO item2 = BillingItemDTO.builder()
                .feeItemId(2L)
                .itemNameSnapshot("三级护理基础费")
                .periodStart(LocalDate.of(2026, 11, 1))
                .periodEnd(LocalDate.of(2026, 11, 30))
                .quantity(new BigDecimal("1"))
                .unitPrice(new BigDecimal("1500.50"))
                .build();

        BillingCreateDTO createDTO = BillingCreateDTO.builder()
                .elderId(10L)
                .billMonth("2026-11")
                .billDate(LocalDate.of(2026, 11, 1))
                .dueDate(LocalDate.of(2026, 11, 15))
                .remark("11月常规账单")
                .items(Arrays.asList(item1, item2))
                .build();

        Long billingId = billingService.createBilling(createDTO);

        assertNotNull(billingId);
        assertEquals(1001L, billingId);

        // 验证写入的账单总金额是否严格为 2200.00 + 1500.50 = 3700.50
        verify(billingMapper).insert(argThat(b ->
                b.getTotalAmount().compareTo(new BigDecimal("3700.50")) == 0 &&
                        "UNPAID".equals(b.getStatus()) &&
                        "2026-11".equals(b.getBillMonth())
        ));
        verify(billingItemMapper, times(2)).insert(any(BillingItem.class));
    }

    @Test
    @DisplayName("测试账单跨租户生成防护：为其他机构长者生成账单应抛出异常")
    void testCreateBilling_CrossOrgDenied() {
        Elder elder = new Elder();
        elder.setId(10L);
        elder.setOrgId(2L); // 机构2，当前租户上下文为机构1
        when(elderMapper.selectById(10L)).thenReturn(elder);

        BillingCreateDTO createDTO = BillingCreateDTO.builder()
                .elderId(10L)
                .billMonth("2026-11")
                .items(Collections.singletonList(BillingItemDTO.builder()
                        .feeItemId(1L)
                        .itemNameSnapshot("床位费")
                        .quantity(BigDecimal.ONE)
                        .unitPrice(new BigDecimal("1000.00"))
                        .build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.createBilling(createDTO));
        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("测试账单作废保护：已产生有效收款记录的账单禁止直接作废")
    void testCancelBilling_HasPaidAmount_ThrowsException() {
        Billing billing = Billing.builder()
                .id(1001L)
                .orgId(1L)
                .billNo("BIL20261101001")
                .totalAmount(new BigDecimal("3000.00"))
                .paidAmount(new BigDecimal("1000.00")) // 已收1000元
                .status("PARTIAL")
                .build();
        when(billingMapper.selectByIdForUpdate(1001L)).thenReturn(billing);

        BusinessException ex = assertThrows(BusinessException.class, () -> billingService.cancelBilling(1001L));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已产生有效收款记录"));
    }

    @Test
    @DisplayName("测试支付幂等控制：同一幂等键二次调用直接返回已有支付记录ID，不产生重复扣款与写入")
    void testRegisterPayment_Idempotency_ExistingRecordReturned() {
        PaymentRecord existingRecord = PaymentRecord.builder()
                .id(888L)
                .orgId(1L)
                .paymentNo("PAY20261101001")
                .billingId(1001L)
                .amount(new BigDecimal("1000.00"))
                .idempotencyKey("IDEM_REQ_KEY_001")
                .status("SUCCESS")
                .build();

        // 模拟幂等键命中已有记录
        when(paymentRecordMapper.selectOne(any())).thenReturn(existingRecord);

        PaymentCreateDTO dto = PaymentCreateDTO.builder()
                .billingId(1001L)
                .amount(new BigDecimal("1000.00"))
                .paymentMethod("ALIPAY")
                .idempotencyKey("IDEM_REQ_KEY_001")
                .build();

        Long paymentId = paymentService.registerPayment(dto);

        assertEquals(888L, paymentId);
        // 验证没有执行重复的账单行锁与重复插入
        verify(billingMapper, never()).selectByIdForUpdate(any());
        verify(paymentRecordMapper, never()).insert(any());
    }

    @Test
    @DisplayName("测试支付并发幂等控制：数据库唯一键冲突后自动回查并返回已入库记录ID")
    void testRegisterPayment_Idempotency_DuplicateKeyExceptionHandled() {
        // 第一次查询未命中（模拟两个并发线程同时到达）
        when(paymentRecordMapper.selectOne(any()))
                .thenReturn(null) // 第一次 preemptive check 未命中
                .thenReturn(PaymentRecord.builder().id(999L).idempotencyKey("CONCURRENT_KEY").build()); // 第二次回查命中

        Billing billing = Billing.builder()
                .id(1001L)
                .orgId(1L)
                .totalAmount(new BigDecimal("2000.00"))
                .paidAmount(BigDecimal.ZERO)
                .status("UNPAID")
                .build();
        when(billingMapper.selectByIdForUpdate(1001L)).thenReturn(billing);

        // 模拟插入时发生唯一键冲突
        when(paymentRecordMapper.insert(any(PaymentRecord.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'CONCURRENT_KEY' for key 'uk_payment_idem'"));

        PaymentCreateDTO dto = PaymentCreateDTO.builder()
                .billingId(1001L)
                .amount(new BigDecimal("500.00"))
                .paymentMethod("WECHAT_PAY")
                .idempotencyKey("CONCURRENT_KEY")
                .build();

        Long paymentId = paymentService.registerPayment(dto);

        assertEquals(999L, paymentId);
    }

    @Test
    @DisplayName("测试支付金额校验：缴费金额超过待清缴差额时阻断支付")
    void testRegisterPayment_AmountExceedsUnpaid_ThrowsException() {
        when(paymentRecordMapper.selectOne(any())).thenReturn(null);

        Billing billing = Billing.builder()
                .id(1001L)
                .orgId(1L)
                .totalAmount(new BigDecimal("2000.00"))
                .paidAmount(new BigDecimal("1500.00")) // 待缴剩余 500 元
                .status("PARTIAL")
                .build();
        when(billingMapper.selectByIdForUpdate(1001L)).thenReturn(billing);

        PaymentCreateDTO dto = PaymentCreateDTO.builder()
                .billingId(1001L)
                .amount(new BigDecimal("600.00")) // 试图支付 600 元，超出 500 元差额
                .paymentMethod("CASH")
                .idempotencyKey("IDEM_OVERPAY_TEST")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.registerPayment(dto));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("超出账单待缴清差额"));
    }

    @Test
    @DisplayName("测试退款限额拦截：申请退款金额超过原收款可退余额时严格阻断")
    void testApplyRefund_AmountExceedsRefundable_ThrowsException() {
        PaymentRecord payment = PaymentRecord.builder()
                .id(501L)
                .orgId(1L)
                .paymentNo("PAY20261101501")
                .billingId(1001L)
                .amount(new BigDecimal("1000.00"))
                .status("SUCCESS")
                .build();
        when(paymentRecordMapper.selectByIdForUpdate(501L)).thenReturn(payment);

        // 已有一笔 600 元退款在审批中或已成功
        RefundRecord existingRefund = RefundRecord.builder()
                .id(601L)
                .paymentId(501L)
                .amount(new BigDecimal("600.00"))
                .status("SUCCESS")
                .build();
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.singletonList(existingRefund));

        // 此时剩余可退 400 元，申请 500 元退款应被拒绝
        RefundApplyDTO applyDTO = RefundApplyDTO.builder()
                .paymentId(501L)
                .amount(new BigDecimal("500.00"))
                .reason("老人中途请假回家退伙食费")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> refundService.applyRefund(applyDTO));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("超过原收款最大可退余额"));
    }

    @Test
    @DisplayName("测试退款审核通过与账单实收状态自动回退")
    void testAuditRefund_Approved_RollbackBillingStatus() {
        RefundRecord refund = RefundRecord.builder()
                .id(701L)
                .orgId(1L)
                .refundNo("REF20261101701")
                .paymentId(501L)
                .amount(new BigDecimal("500.00"))
                .status("PENDING")
                .build();
        when(refundRecordMapper.selectByIdForUpdate(701L)).thenReturn(refund);

        PaymentRecord payment = PaymentRecord.builder()
                .id(501L)
                .orgId(1L)
                .paymentNo("PAY20261101501")
                .billingId(1001L)
                .amount(new BigDecimal("1000.00"))
                .status("SUCCESS")
                .build();
        when(paymentRecordMapper.selectByIdForUpdate(501L)).thenReturn(payment);

        // 模拟退款审批通过
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundRecordMapper.updateById(any(RefundRecord.class))).thenReturn(1);

        // 账单原本是已结清 PAID，总额 1000，原实收 1000
        Billing billing = Billing.builder()
                .id(1001L)
                .orgId(1L)
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(new BigDecimal("1000.00"))
                .dueDate(LocalDate.of(2026, 12, 1))
                .status("PAID")
                .build();
        when(billingMapper.selectByIdForUpdate(1001L)).thenReturn(billing);

        // 驱动退款生效后，BillingServiceImpl 重新统计收款与退款
        when(paymentRecordMapper.selectList(any())).thenReturn(Collections.singletonList(payment));
        // 退款成功后总退款为 500
        RefundRecord successRefund = RefundRecord.builder()
                .id(701L)
                .paymentId(501L)
                .amount(new BigDecimal("500.00"))
                .status("SUCCESS")
                .build();
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.singletonList(successRefund));

        RefundAuditDTO auditDTO = RefundAuditDTO.builder()
                .approved(true)
                .remark("核实无误，同意退费")
                .build();

        refundService.auditRefund(701L, auditDTO);

        // 验证退款记录状态变更为 SUCCESS
        verify(refundRecordMapper).updateById(argThat(r -> "SUCCESS".equals(r.getStatus())));

        // 验证账单实收金额回退为 1000 - 500 = 500，状态回退为 PARTIAL
        verify(billingMapper).updateById(argThat(b ->
                b.getPaidAmount().compareTo(new BigDecimal("500.00")) == 0 &&
                        "PARTIAL".equals(b.getStatus())
        ));
    }

    @Test
    @DisplayName("测试财务统计看板聚合计算")
    void testGetFinanceSummary() {
        Billing b1 = Billing.builder()
                .id(1L)
                .orgId(1L)
                .totalAmount(new BigDecimal("3000.00"))
                .paidAmount(new BigDecimal("3000.00"))
                .status("PAID")
                .build();

        Billing b2 = Billing.builder()
                .id(2L)
                .orgId(1L)
                .totalAmount(new BigDecimal("4000.00"))
                .paidAmount(new BigDecimal("1500.00"))
                .status("PARTIAL")
                .build();

        Billing b3 = Billing.builder()
                .id(3L)
                .orgId(1L)
                .totalAmount(new BigDecimal("2000.00"))
                .paidAmount(BigDecimal.ZERO)
                .dueDate(LocalDate.now().minusDays(2))
                .status("OVERDUE")
                .build();

        when(billingMapper.selectList(any())).thenReturn(Arrays.asList(b1, b2, b3));

        RefundRecord ref = RefundRecord.builder()
                .id(1L)
                .amount(new BigDecimal("300.00"))
                .status("SUCCESS")
                .build();
        when(refundRecordMapper.selectList(any())).thenReturn(Collections.singletonList(ref));

        FinanceSummaryVO summary = refundService.getFinanceSummary();

        assertNotNull(summary);
        // 总应收 = 3000 + 4000 + 2000 = 9000.00
        assertEquals(new BigDecimal("9000.00"), summary.getTotalBilledAmount());
        // 总实收 = 3000 + 1500 + 0 = 4500.00
        assertEquals(new BigDecimal("4500.00"), summary.getTotalPaidAmount());
        // 待收欠费 = 9000 - 4500 = 4500.00
        assertEquals(new BigDecimal("4500.00"), summary.getTotalUnpaidAmount());
        // 成功退款 = 300.00
        assertEquals(new BigDecimal("300.00"), summary.getTotalRefundAmount());
        assertEquals(1L, summary.getPaidBillCount());
        assertEquals(1L, summary.getPartialBillCount());
        assertEquals(1L, summary.getOverdueBillCount());
    }
}
