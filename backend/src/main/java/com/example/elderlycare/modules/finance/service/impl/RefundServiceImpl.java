package com.example.elderlycare.modules.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.finance.dto.RefundApplyDTO;
import com.example.elderlycare.modules.finance.dto.RefundAuditDTO;
import com.example.elderlycare.modules.finance.dto.RefundQueryDTO;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.entity.PaymentRecord;
import com.example.elderlycare.modules.finance.entity.RefundRecord;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.finance.mapper.PaymentRecordMapper;
import com.example.elderlycare.modules.finance.mapper.RefundRecordMapper;
import com.example.elderlycare.modules.finance.service.BillingService;
import com.example.elderlycare.modules.finance.service.RefundService;
import com.example.elderlycare.modules.finance.vo.FinanceSummaryVO;
import com.example.elderlycare.modules.finance.vo.RefundRecordVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRecordMapper refundRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final BillingMapper billingMapper;
    private final BillingService billingService;
    private final ElderMapper elderMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyRefund(RefundApplyDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        // 1. 排他锁锁定原收款记录，防止并发超额退款
        PaymentRecord payment = paymentRecordMapper.selectByIdForUpdate(dto.getPaymentId());
        if (payment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "原收款记录不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(payment.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权对其他机构收款申请退款");
        }
        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原收款记录状态非成功，无法申请退款");
        }

        // 2. 统计已发生退款额度（包含审批中与已成功的退款额），严格检查是否超额
        List<RefundRecord> occupiedRefunds = refundRecordMapper.selectList(
                new LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getPaymentId, payment.getId())
                        .in(RefundRecord::getStatus, Arrays.asList("PENDING", "APPROVED", "SUCCESS"))
        );

        BigDecimal occupiedAmount = occupiedRefunds.stream()
                .map(RefundRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingRefundable = payment.getAmount().subtract(occupiedAmount);
        if (remainingRefundable.compareTo(BigDecimal.ZERO) < 0) {
            remainingRefundable = BigDecimal.ZERO;
        }

        if (dto.getAmount().compareTo(remainingRefundable) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("申请退款金额 ¥%s 超过原收款最大可退余额 ¥%s", dto.getAmount(), remainingRefundable));
        }

        // 3. 写入退款申请（初始状态 PENDING）
        String refundNo = generateRefundNo();
        RefundRecord refund = RefundRecord.builder()
                .orgId(payment.getOrgId())
                .refundNo(refundNo)
                .paymentId(payment.getId())
                .amount(dto.getAmount().setScale(2, RoundingMode.HALF_UP))
                .reason(dto.getReason().trim())
                .status("PENDING")
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        refundRecordMapper.insert(refund);

        sysOperationLogService.recordLog(
                "FINANCE_REFUND",
                "APPLY",
                "REFUND",
                refund.getId(),
                "/api/v1/finance/refunds",
                "127.0.0.1",
                String.format("提交退款申请: 退款单号=%s, 原收款单号=%s, 金额=¥%s, 原因=%s",
                        refundNo, payment.getPaymentNo(), refund.getAmount(), refund.getReason())
        );

        return refund.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(Long id, RefundAuditDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        // 1. 排他锁锁定退款记录
        RefundRecord refund = refundRecordMapper.selectByIdForUpdate(id);
        if (refund == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "退款记录不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(refund.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权审批其他机构退款申请");
        }
        if (!"PENDING".equalsIgnoreCase(refund.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该退款申请已被审核，无需重复处理");
        }

        PaymentRecord payment = paymentRecordMapper.selectByIdForUpdate(refund.getPaymentId());
        if (payment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联原收款记录不存在");
        }

        if (Boolean.TRUE.equals(dto.getApproved())) {
            // 2. 审批通过：重新复核该收款项累计成功退款总额未超限
            List<RefundRecord> approvedRefunds = refundRecordMapper.selectList(
                    new LambdaQueryWrapper<RefundRecord>()
                            .eq(RefundRecord::getPaymentId, payment.getId())
                            .eq(RefundRecord::getStatus, "SUCCESS")
            );
            BigDecimal successAmount = approvedRefunds.stream()
                    .map(RefundRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (successAmount.add(refund.getAmount()).compareTo(payment.getAmount()) > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "累计成功退款金额超过原收款总额，审批失败");
            }

            refund.setStatus("SUCCESS");
            refund.setApprovedBy(currentUserId);
            refund.setRefundedAt(LocalDateTime.now());
            refundRecordMapper.updateById(refund);

            // 3. 驱动重算账单已实收金额与状态回退（如 PAID -> PARTIAL）
            billingService.updateBillingPaidAmountAndStatus(payment.getBillingId());

            sysOperationLogService.recordLog(
                    "FINANCE_REFUND",
                    "AUDIT_PASS",
                    "REFUND",
                    refund.getId(),
                    "/api/v1/finance/refunds/" + id + "/audit",
                    "127.0.0.1",
                    String.format("退款审核通过并执行入账: 单号=%s, 金额=¥%s, 账单回退重算完成",
                            refund.getRefundNo(), refund.getAmount())
            );
        } else {
            // 驳回退款申请
            refund.setStatus("REJECTED");
            refund.setApprovedBy(currentUserId);
            refundRecordMapper.updateById(refund);

            sysOperationLogService.recordLog(
                    "FINANCE_REFUND",
                    "AUDIT_REJECT",
                    "REFUND",
                    refund.getId(),
                    "/api/v1/finance/refunds/" + id + "/audit",
                    "127.0.0.1",
                    String.format("驳回退款申请: 单号=%s, 原因=%s", refund.getRefundNo(), dto.getRemark())
            );
        }
    }

    @Override
    public RefundRecordVO getRefundDetail(Long id) {
        RefundRecord record = refundRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "退款记录不存在");
        }
        checkRefundAccess(record);

        List<RefundRecordVO> voList = convertToRefundVOList(Collections.singletonList(record));
        return voList.get(0);
    }

    @Override
    public PageResult<RefundRecordVO> pageRefunds(RefundQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<RefundRecord> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(RefundRecord::getOrgId, currentOrgId);
        }
        if (query.getPaymentId() != null) {
            qw.eq(RefundRecord::getPaymentId, query.getPaymentId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(RefundRecord::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(RefundRecord::getRefundNo, kw)
                    .or().like(RefundRecord::getReason, kw));
        }

        qw.orderByDesc(RefundRecord::getCreatedAt).orderByDesc(RefundRecord::getId);

        Page<RefundRecord> page = refundRecordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), qw);

        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<RefundRecordVO> voList = convertToRefundVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public FinanceSummaryVO getFinanceSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();

        // 1. 查询当前机构全部非作废账单
        LambdaQueryWrapper<Billing> bqw = new LambdaQueryWrapper<Billing>()
                .ne(Billing::getStatus, "CANCELLED");
        if (currentOrgId != null) {
            bqw.eq(Billing::getOrgId, currentOrgId);
        }
        List<Billing> billings = billingMapper.selectList(bqw);

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        long unpaidCount = 0L;
        long partialCount = 0L;
        long paidCount = 0L;
        long overdueCount = 0L;

        LocalDate today = LocalDate.now();

        for (Billing b : billings) {
            totalBilled = totalBilled.add(b.getTotalAmount());
            totalPaid = totalPaid.add(b.getPaidAmount());

            String st = b.getStatus() != null ? b.getStatus().toUpperCase() : "UNPAID";
            switch (st) {
                case "PAID" -> paidCount++;
                case "PARTIAL" -> partialCount++;
                case "OVERDUE" -> overdueCount++;
                default -> {
                    if (b.getDueDate() != null && today.isAfter(b.getDueDate())) {
                        overdueCount++;
                    } else {
                        unpaidCount++;
                    }
                }
            }
        }

        BigDecimal totalUnpaid = totalBilled.subtract(totalPaid);
        if (totalUnpaid.compareTo(BigDecimal.ZERO) < 0) {
            totalUnpaid = BigDecimal.ZERO;
        }

        // 2. 统计成功退款总额
        LambdaQueryWrapper<RefundRecord> rqw = new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getStatus, "SUCCESS");
        if (currentOrgId != null) {
            rqw.eq(RefundRecord::getOrgId, currentOrgId);
        }
        List<RefundRecord> successRefunds = refundRecordMapper.selectList(rqw);
        BigDecimal totalRefund = successRefunds.stream()
                .map(RefundRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return FinanceSummaryVO.builder()
                .totalBilledAmount(totalBilled.setScale(2, RoundingMode.HALF_UP))
                .totalPaidAmount(totalPaid.setScale(2, RoundingMode.HALF_UP))
                .totalUnpaidAmount(totalUnpaid.setScale(2, RoundingMode.HALF_UP))
                .unpaidBillCount(unpaidCount)
                .partialBillCount(partialCount)
                .paidBillCount(paidCount)
                .overdueBillCount(overdueCount)
                .totalRefundAmount(totalRefund.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

    private void checkRefundAccess(RefundRecord record) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(record.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构退款记录");
        }
    }

    private String generateRefundNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "REF" + dateStr + rand;
    }

    private List<RefundRecordVO> convertToRefundVOList(List<RefundRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        Set<Long> paymentIds = records.stream().map(RefundRecord::getPaymentId).collect(Collectors.toSet());
        Map<Long, PaymentRecord> paymentMap = paymentIds.isEmpty() ? Collections.emptyMap() :
                paymentRecordMapper.selectBatchIds(paymentIds).stream().collect(Collectors.toMap(PaymentRecord::getId, p -> p));

        Set<Long> billingIds = paymentMap.values().stream().map(PaymentRecord::getBillingId).collect(Collectors.toSet());
        Map<Long, Billing> billingMap = billingIds.isEmpty() ? Collections.emptyMap() :
                billingMapper.selectBatchIds(billingIds).stream().collect(Collectors.toMap(Billing::getId, b -> b));

        Set<Long> elderIds = billingMap.values().stream().map(Billing::getElderId).collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Set<Long> userIds = new HashSet<>();
        records.forEach(r -> {
            if (r.getCreatedBy() != null) userIds.add(r.getCreatedBy());
            if (r.getApprovedBy() != null) userIds.add(r.getApprovedBy());
        });
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        return records.stream().map(r -> {
            PaymentRecord payment = paymentMap.get(r.getPaymentId());
            Billing billing = payment != null ? billingMap.get(payment.getBillingId()) : null;
            Elder elder = billing != null ? elderMap.get(billing.getElderId()) : null;

            SysUser creator = r.getCreatedBy() != null ? userMap.get(r.getCreatedBy()) : null;
            SysUser approver = r.getApprovedBy() != null ? userMap.get(r.getApprovedBy()) : null;

            return RefundRecordVO.builder()
                    .id(r.getId())
                    .orgId(r.getOrgId())
                    .refundNo(r.getRefundNo())
                    .paymentId(r.getPaymentId())
                    .paymentNo(payment != null ? payment.getPaymentNo() : "-")
                    .billingId(payment != null ? payment.getBillingId() : null)
                    .billNo(billing != null ? billing.getBillNo() : "-")
                    .elderId(elder != null ? elder.getId() : null)
                    .elderName(elder != null ? elder.getName() : "未知长者")
                    .amount(r.getAmount())
                    .reason(r.getReason())
                    .status(r.getStatus())
                    .statusName(getRefundStatusName(r.getStatus()))
                    .approvedBy(r.getApprovedBy())
                    .approvedByName(approver != null ? approver.getRealName() : (r.getApprovedBy() != null ? "管理员" : "-"))
                    .refundedAt(r.getRefundedAt())
                    .createdBy(r.getCreatedBy())
                    .createdByName(creator != null ? creator.getRealName() : "系统自动")
                    .createdAt(r.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String getRefundStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "待审批";
            case "APPROVED" -> "已通过";
            case "SUCCESS" -> "退款成功";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }
}
