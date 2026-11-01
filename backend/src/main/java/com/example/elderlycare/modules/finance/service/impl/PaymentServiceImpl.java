package com.example.elderlycare.modules.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.Building;
import com.example.elderlycare.modules.admission.entity.Room;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.finance.dto.PaymentCreateDTO;
import com.example.elderlycare.modules.finance.dto.PaymentQueryDTO;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.entity.PaymentRecord;
import com.example.elderlycare.modules.finance.entity.RefundRecord;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.finance.mapper.PaymentRecordMapper;
import com.example.elderlycare.modules.finance.mapper.RefundRecordMapper;
import com.example.elderlycare.modules.finance.service.BillingService;
import com.example.elderlycare.modules.finance.service.PaymentService;
import com.example.elderlycare.modules.finance.vo.PaymentRecordVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final BillingMapper billingMapper;
    private final BillingService billingService;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long registerPayment(PaymentCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        String rawIdemKey = dto.getIdempotencyKey().trim();

        // 1. 支付幂等先验检查：若同一机构下该幂等键已存在，直接返回已有支付记录主键，避免重复入账与计费
        LambdaQueryWrapper<PaymentRecord> idemWrapper = new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getIdempotencyKey, rawIdemKey);
        if (currentOrgId != null) {
            idemWrapper.eq(PaymentRecord::getOrgId, currentOrgId);
        }
        PaymentRecord existingRecord = paymentRecordMapper.selectOne(idemWrapper);
        if (existingRecord != null) {
            log.warn("检测到重复支付请求，触发幂等拦截: orgId={}, idempotencyKey={}, existingPaymentId={}",
                    currentOrgId, rawIdemKey, existingRecord.getId());
            return existingRecord.getId();
        }

        // 2. 排他行级锁锁定目标账单，保证高并发下账单金额与状态变动的原子性
        Billing billing = billingMapper.selectByIdForUpdate(dto.getBillingId());
        if (billing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联账单不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(billing.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权操作其他机构账单");
        }
        if ("CANCELLED".equalsIgnoreCase(billing.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该账单已作废，无法继续缴费");
        }
        if ("PAID".equalsIgnoreCase(billing.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该账单已全部结清，无需再次缴费");
        }

        // 3. 校验本次支付金额不能超出当前待缴差额
        BigDecimal unpaidAmount = billing.getTotalAmount().subtract(billing.getPaidAmount());
        if (unpaidAmount.compareTo(BigDecimal.ZERO) < 0) {
            unpaidAmount = BigDecimal.ZERO;
        }
        if (dto.getAmount().compareTo(unpaidAmount) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("缴费金额 ¥%s 超出账单待缴清差额 ¥%s", dto.getAmount(), unpaidAmount));
        }

        // 4. 构建并写入支付流水记录
        String paymentNo = generatePaymentNo();
        PaymentRecord paymentRecord = PaymentRecord.builder()
                .orgId(billing.getOrgId())
                .paymentNo(paymentNo)
                .billingId(billing.getId())
                .amount(dto.getAmount().setScale(2, RoundingMode.HALF_UP))
                .paymentMethod(dto.getPaymentMethod().trim().toUpperCase())
                .transactionNo(StringUtils.hasText(dto.getTransactionNo()) ? dto.getTransactionNo().trim() : null)
                .idempotencyKey(rawIdemKey)
                .status("SUCCESS")
                .paidAt(LocalDateTime.now())
                .operatorId(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            paymentRecordMapper.insert(paymentRecord);
        } catch (DuplicateKeyException e) {
            log.warn("数据库唯一索引捕获并发重复支付: idempotencyKey={}", rawIdemKey);
            PaymentRecord concurrentRecord = paymentRecordMapper.selectOne(
                    new LambdaQueryWrapper<PaymentRecord>()
                            .eq(PaymentRecord::getIdempotencyKey, rawIdemKey)
                            .last("LIMIT 1")
            );
            if (concurrentRecord != null) {
                return concurrentRecord.getId();
            }
            throw e;
        }

        // 5. 触发重算账单累计实收金额与状态（PAID / PARTIAL）
        billingService.updateBillingPaidAmountAndStatus(billing.getId());

        // 6. 审计日志登记
        sysOperationLogService.recordLog(
                "FINANCE_PAYMENT",
                "CREATE",
                "PAYMENT",
                paymentRecord.getId(),
                "/api/v1/finance/payments",
                "127.0.0.1",
                String.format("登记缴费入账成功: 流水号=%s, 账单号=%s, 金额=¥%s, 渠道=%s, 幂等键=%s",
                        paymentRecord.getPaymentNo(), billing.getBillNo(), paymentRecord.getAmount(),
                        paymentRecord.getPaymentMethod(), rawIdemKey)
        );

        return paymentRecord.getId();
    }

    @Override
    public BigDecimal getRefundableAmount(Long paymentId) {
        PaymentRecord payment = paymentRecordMapper.selectById(paymentId);
        if (payment == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收款记录不存在");
        }
        checkPaymentAccess(payment);

        if (!"SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 统计该收款项下所有审核中或已成功的退款占用额
        List<RefundRecord> refunds = refundRecordMapper.selectList(
                new LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getPaymentId, paymentId)
                        .in(RefundRecord::getStatus, Arrays.asList("PENDING", "APPROVED", "SUCCESS"))
        );

        BigDecimal refundedAmount = refunds.stream()
                .map(RefundRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal refundable = payment.getAmount().subtract(refundedAmount);
        if (refundable.compareTo(BigDecimal.ZERO) < 0) {
            refundable = BigDecimal.ZERO;
        }
        return refundable.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public PaymentRecordVO getPaymentDetail(Long id) {
        PaymentRecord record = paymentRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "收款记录不存在");
        }
        checkPaymentAccess(record);

        List<PaymentRecordVO> voList = convertToPaymentVOList(Collections.singletonList(record));
        return voList.get(0);
    }

    @Override
    public PageResult<PaymentRecordVO> pagePayments(PaymentQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<PaymentRecord> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(PaymentRecord::getOrgId, currentOrgId);
        }
        if (query.getBillingId() != null) {
            qw.eq(PaymentRecord::getBillingId, query.getBillingId());
        }
        if (StringUtils.hasText(query.getPaymentMethod())) {
            qw.eq(PaymentRecord::getPaymentMethod, query.getPaymentMethod().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(PaymentRecord::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(PaymentRecord::getPaymentNo, kw)
                    .or().like(PaymentRecord::getTransactionNo, kw)
                    .or().like(PaymentRecord::getIdempotencyKey, kw));
        }

        qw.orderByDesc(PaymentRecord::getPaidAt).orderByDesc(PaymentRecord::getId);

        Page<PaymentRecord> page = paymentRecordMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), qw);

        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<PaymentRecordVO> voList = convertToPaymentVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    private void checkPaymentAccess(PaymentRecord record) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(record.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构财务流水");
        }
    }

    private String generatePaymentNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "PAY" + dateStr + rand;
    }

    private List<PaymentRecordVO> convertToPaymentVOList(List<PaymentRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyList();
        }

        Set<Long> billingIds = records.stream().map(PaymentRecord::getBillingId).collect(Collectors.toSet());
        Map<Long, Billing> billingMap = billingIds.isEmpty() ? Collections.emptyMap() :
                billingMapper.selectBatchIds(billingIds).stream().collect(Collectors.toMap(Billing::getId, b -> b));

        Set<Long> elderIds = billingMap.values().stream().map(Billing::getElderId).collect(Collectors.toSet());
        Map<Long, Elder> elderMap = elderIds.isEmpty() ? Collections.emptyMap() :
                elderMapper.selectBatchIds(elderIds).stream().collect(Collectors.toMap(Elder::getId, e -> e));

        Map<Long, String> elderBedMap = new HashMap<>();
        if (!elderIds.isEmpty()) {
            List<Admission> activeAdmissions = admissionMapper.selectList(
                    new LambdaQueryWrapper<Admission>()
                            .in(Admission::getElderId, elderIds)
                            .eq(Admission::getStatus, "CHECKED_IN")
            );
            if (!CollectionUtils.isEmpty(activeAdmissions)) {
                Set<Long> bedIds = activeAdmissions.stream().map(Admission::getBedId).filter(Objects::nonNull).collect(Collectors.toSet());
                if (!bedIds.isEmpty()) {
                    Map<Long, Bed> bedMap = bedMapper.selectBatchIds(bedIds).stream().collect(Collectors.toMap(Bed::getId, b -> b));
                    Set<Long> roomIds = bedMap.values().stream().map(Bed::getRoomId).filter(Objects::nonNull).collect(Collectors.toSet());
                    Map<Long, Room> roomMap = roomIds.isEmpty() ? Collections.emptyMap() :
                            roomMapper.selectBatchIds(roomIds).stream().collect(Collectors.toMap(Room::getId, r -> r));
                    Set<Long> buildingIds = roomMap.values().stream().map(Room::getBuildingId).filter(Objects::nonNull).collect(Collectors.toSet());
                    Map<Long, Building> buildingMap = buildingIds.isEmpty() ? Collections.emptyMap() :
                            buildingMapper.selectBatchIds(buildingIds).stream().collect(Collectors.toMap(Building::getId, b -> b));

                    for (Admission adm : activeAdmissions) {
                        Bed bed = bedMap.get(adm.getBedId());
                        if (bed != null) {
                            Room room = roomMap.get(bed.getRoomId());
                            Building building = room != null ? buildingMap.get(room.getBuildingId()) : null;
                            String bName = building != null ? building.getBuildingName() : "";
                            String rName = room != null ? room.getRoomNumber() + "室" : "";
                            String bedStr = bed.getBedNumber() + "床";
                            elderBedMap.put(adm.getElderId(), String.join(" ", bName, rName, bedStr).trim());
                        }
                    }
                }
            }
        }

        Set<Long> operatorIds = records.stream().map(PaymentRecord::getOperatorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = operatorIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(operatorIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Set<Long> paymentIds = records.stream().map(PaymentRecord::getId).collect(Collectors.toSet());
        List<RefundRecord> allRefunds = refundRecordMapper.selectList(
                new LambdaQueryWrapper<RefundRecord>()
                        .in(RefundRecord::getPaymentId, paymentIds)
                        .in(RefundRecord::getStatus, Arrays.asList("PENDING", "APPROVED", "SUCCESS"))
        );
        Map<Long, BigDecimal> refundSumMap = new HashMap<>();
        for (RefundRecord rf : allRefunds) {
            refundSumMap.merge(rf.getPaymentId(), rf.getAmount(), BigDecimal::add);
        }

        return records.stream().map(r -> {
            Billing billing = billingMap.get(r.getBillingId());
            Elder elder = billing != null ? elderMap.get(billing.getElderId()) : null;
            SysUser op = r.getOperatorId() != null ? userMap.get(r.getOperatorId()) : null;

            BigDecimal usedRefund = refundSumMap.getOrDefault(r.getId(), BigDecimal.ZERO);
            BigDecimal refundable = r.getAmount().subtract(usedRefund);
            if (refundable.compareTo(BigDecimal.ZERO) < 0) {
                refundable = BigDecimal.ZERO;
            }

            return PaymentRecordVO.builder()
                    .id(r.getId())
                    .orgId(r.getOrgId())
                    .paymentNo(r.getPaymentNo())
                    .billingId(r.getBillingId())
                    .billNo(billing != null ? billing.getBillNo() : "-")
                    .elderId(elder != null ? elder.getId() : null)
                    .elderName(elder != null ? elder.getName() : "未知长者")
                    .bedInfo(elder != null ? elderBedMap.getOrDefault(elder.getId(), "-") : "-")
                    .amount(r.getAmount())
                    .paymentMethod(r.getPaymentMethod())
                    .paymentMethodName(getPaymentMethodName(r.getPaymentMethod()))
                    .transactionNo(r.getTransactionNo())
                    .idempotencyKey(r.getIdempotencyKey())
                    .status(r.getStatus())
                    .statusName(getPaymentStatusName(r.getStatus()))
                    .paidAt(r.getPaidAt())
                    .operatorId(r.getOperatorId())
                    .operatorName(op != null ? op.getRealName() : "系统自动")
                    .createdAt(r.getCreatedAt())
                    .refundableAmount(refundable.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }).collect(Collectors.toList());
    }

    private String getPaymentMethodName(String method) {
        if (method == null) return "-";
        return switch (method.toUpperCase()) {
            case "CASH" -> "现金支付";
            case "BANK_TRANSFER" -> "银行转账";
            case "ALIPAY" -> "支付宝";
            case "WECHAT_PAY" -> "微信支付";
            case "POS" -> "POS刷卡";
            case "MOCK_ONLINE" -> "模拟在线支付";
            default -> method;
        };
    }

    private String getPaymentStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "SUCCESS" -> "收款成功";
            case "CANCELLED" -> "已撤销";
            default -> status;
        };
    }
}
