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
import com.example.elderlycare.modules.finance.dto.BillingCreateDTO;
import com.example.elderlycare.modules.finance.dto.BillingItemDTO;
import com.example.elderlycare.modules.finance.dto.BillingQueryDTO;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.entity.BillingItem;
import com.example.elderlycare.modules.finance.entity.PaymentRecord;
import com.example.elderlycare.modules.finance.entity.RefundRecord;
import com.example.elderlycare.modules.finance.mapper.BillingItemMapper;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.finance.mapper.PaymentRecordMapper;
import com.example.elderlycare.modules.finance.mapper.RefundRecordMapper;
import com.example.elderlycare.modules.finance.service.BillingService;
import com.example.elderlycare.modules.finance.vo.BillingItemVO;
import com.example.elderlycare.modules.finance.vo.BillingVO;
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
public class BillingServiceImpl implements BillingService {

    private final BillingMapper billingMapper;
    private final BillingItemMapper billingItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBilling(BillingCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        Elder elder = elderMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关联长者不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权为其他机构长者生成账单");
        }
        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();

        Long admissionId = dto.getAdmissionId();
        if (admissionId == null) {
            Admission activeAdm = admissionMapper.selectOne(
                    new LambdaQueryWrapper<Admission>()
                            .eq(Admission::getElderId, elder.getId())
                            .eq(Admission::getStatus, "CHECKED_IN")
                            .last("LIMIT 1")
            );
            if (activeAdm != null) {
                admissionId = activeAdm.getId();
            }
        }

        // 计算账单明细并合计总金额，全程严格使用 BigDecimal
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BillingItem> billingItems = new ArrayList<>();

        for (BillingItemDTO itemDto : dto.getItems()) {
            BigDecimal lineAmount = itemDto.getQuantity()
                    .multiply(itemDto.getUnitPrice())
                    .setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(lineAmount);

            BillingItem item = BillingItem.builder()
                    .feeItemId(itemDto.getFeeItemId())
                    .itemNameSnapshot(itemDto.getItemNameSnapshot().trim())
                    .periodStart(itemDto.getPeriodStart())
                    .periodEnd(itemDto.getPeriodEnd())
                    .quantity(itemDto.getQuantity().setScale(2, RoundingMode.HALF_UP))
                    .unitPrice(itemDto.getUnitPrice().setScale(2, RoundingMode.HALF_UP))
                    .amount(lineAmount)
                    .build();
            billingItems.add(item);
        }

        String billNo = generateBillNo();
        String initialStatus = "UNPAID";
        if (dto.getDueDate() != null && LocalDate.now().isAfter(dto.getDueDate())) {
            initialStatus = "OVERDUE";
        }

        Billing billing = Billing.builder()
                .orgId(orgId)
                .billNo(billNo)
                .elderId(elder.getId())
                .admissionId(admissionId)
                .billMonth(dto.getBillMonth().trim())
                .billDate(dto.getBillDate())
                .dueDate(dto.getDueDate())
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .status(initialStatus)
                .remark(dto.getRemark())
                .createdBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        billingMapper.insert(billing);

        for (BillingItem bi : billingItems) {
            bi.setBillingId(billing.getId());
            billingItemMapper.insert(bi);
        }

        sysOperationLogService.recordLog(
                "FINANCE_BILLING",
                "CREATE",
                "BILLING",
                billing.getId(),
                "/api/v1/finance/billings",
                "127.0.0.1",
                String.format("生成月度账单: 单号=%s, 长者=%s, 月份=%s, 总金额=%s",
                        billNo, elder.getName(), billing.getBillMonth(), totalAmount)
        );

        return billing.getId();
    }

    @Override
    public BillingVO getBillingDetail(Long id) {
        Billing billing = billingMapper.selectById(id);
        if (billing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账单记录不存在");
        }
        checkBillingAccess(billing);

        List<BillingVO> voList = convertToBillingVOList(Collections.singletonList(billing));
        return voList.get(0);
    }

    @Override
    public PageResult<BillingVO> pageBillings(BillingQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Billing> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(Billing::getOrgId, currentOrgId);
        }
        if (query.getElderId() != null) {
            qw.eq(Billing::getElderId, query.getElderId());
        }
        if (StringUtils.hasText(query.getBillMonth())) {
            qw.eq(Billing::getBillMonth, query.getBillMonth().trim());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(Billing::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(Billing::getBillNo, kw).or().like(Billing::getRemark, kw));
        }

        qw.orderByDesc(Billing::getBillMonth).orderByDesc(Billing::getId);

        Page<Billing> page = billingMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<BillingVO> voList = convertToBillingVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelBilling(Long id) {
        Billing billing = billingMapper.selectByIdForUpdate(id);
        if (billing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "账单记录不存在");
        }
        checkBillingAccess(billing);

        if ("CANCELLED".equalsIgnoreCase(billing.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该账单已作废，无需重复操作");
        }
        if (billing.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "账单已产生有效收款记录，不可直接作废；请先退款平账后处理");
        }

        billing.setStatus("CANCELLED");
        billingMapper.updateById(billing);

        sysOperationLogService.recordLog(
                "FINANCE_BILLING",
                "CANCEL",
                "BILLING",
                billing.getId(),
                "/api/v1/finance/billings/" + id + "/cancel",
                "127.0.0.1",
                String.format("作废账单: 单号=%s, 长者ID=%d", billing.getBillNo(), billing.getElderId())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillingPaidAmountAndStatus(Long billingId) {
        Billing billing = billingMapper.selectByIdForUpdate(billingId);
        if (billing == null) {
            return;
        }

        // 统计所有成功收款总额
        List<PaymentRecord> payments = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getBillingId, billingId)
                        .eq(PaymentRecord::getStatus, "SUCCESS")
        );
        BigDecimal totalPayments = payments.stream()
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 统计所有成功退款总额
        BigDecimal totalRefunds = BigDecimal.ZERO;
        if (!CollectionUtils.isEmpty(payments)) {
            Set<Long> paymentIds = payments.stream().map(PaymentRecord::getId).collect(Collectors.toSet());
            List<RefundRecord> refunds = refundRecordMapper.selectList(
                    new LambdaQueryWrapper<RefundRecord>()
                            .in(RefundRecord::getPaymentId, paymentIds)
                            .eq(RefundRecord::getStatus, "SUCCESS")
            );
            totalRefunds = refunds.stream()
                    .map(RefundRecord::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal netPaid = totalPayments.subtract(totalRefunds);
        if (netPaid.compareTo(BigDecimal.ZERO) < 0) {
            netPaid = BigDecimal.ZERO;
        }
        netPaid = netPaid.setScale(2, RoundingMode.HALF_UP);

        billing.setPaidAmount(netPaid);

        if (!"CANCELLED".equalsIgnoreCase(billing.getStatus())) {
            if (netPaid.compareTo(billing.getTotalAmount()) >= 0) {
                billing.setStatus("PAID");
            } else if (netPaid.compareTo(BigDecimal.ZERO) > 0) {
                billing.setStatus("PARTIAL");
            } else {
                if (billing.getDueDate() != null && LocalDate.now().isAfter(billing.getDueDate())) {
                    billing.setStatus("OVERDUE");
                } else {
                    billing.setStatus("UNPAID");
                }
            }
        }

        billingMapper.updateById(billing);
    }

    private void checkBillingAccess(Billing billing) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(billing.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构账单");
        }
    }

    private String generateBillNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "BIL" + dateStr + rand;
    }

    private List<BillingVO> convertToBillingVOList(List<Billing> billings) {
        if (CollectionUtils.isEmpty(billings)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = billings.stream().map(Billing::getElderId).collect(Collectors.toSet());
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

        Set<Long> userIds = billings.stream().map(Billing::getCreatedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        Set<Long> billingIds = billings.stream().map(Billing::getId).collect(Collectors.toSet());
        List<BillingItem> allItems = billingItemMapper.selectList(
                new LambdaQueryWrapper<BillingItem>().in(BillingItem::getBillingId, billingIds)
        );
        Map<Long, List<BillingItemVO>> itemMap = new HashMap<>();
        for (BillingItem bi : allItems) {
            itemMap.computeIfAbsent(bi.getBillingId(), k -> new ArrayList<>())
                    .add(BillingItemVO.builder()
                            .id(bi.getId())
                            .billingId(bi.getBillingId())
                            .feeItemId(bi.getFeeItemId())
                            .itemNameSnapshot(bi.getItemNameSnapshot())
                            .periodStart(bi.getPeriodStart())
                            .periodEnd(bi.getPeriodEnd())
                            .quantity(bi.getQuantity())
                            .unitPrice(bi.getUnitPrice())
                            .amount(bi.getAmount())
                            .build());
        }

        LocalDate today = LocalDate.now();

        return billings.stream().map(b -> {
            Elder el = elderMap.get(b.getElderId());
            SysUser creator = b.getCreatedBy() != null ? userMap.get(b.getCreatedBy()) : null;

            BigDecimal unpaid = b.getTotalAmount().subtract(b.getPaidAmount());
            if (unpaid.compareTo(BigDecimal.ZERO) < 0) {
                unpaid = BigDecimal.ZERO;
            }

            boolean isOverdue = b.getDueDate() != null && today.isAfter(b.getDueDate()) && !"PAID".equalsIgnoreCase(b.getStatus()) && !"CANCELLED".equalsIgnoreCase(b.getStatus());

            return BillingVO.builder()
                    .id(b.getId())
                    .orgId(b.getOrgId())
                    .billNo(b.getBillNo())
                    .elderId(b.getElderId())
                    .elderName(el != null ? el.getName() : "未知长者")
                    .elderNo(el != null ? el.getElderNo() : "-")
                    .bedInfo(elderBedMap.getOrDefault(b.getElderId(), "-"))
                    .admissionId(b.getAdmissionId())
                    .billMonth(b.getBillMonth())
                    .billDate(b.getBillDate())
                    .dueDate(b.getDueDate())
                    .totalAmount(b.getTotalAmount())
                    .paidAmount(b.getPaidAmount())
                    .unpaidAmount(unpaid)
                    .status(b.getStatus())
                    .statusName(getBillingStatusName(b.getStatus()))
                    .remark(b.getRemark())
                    .createdBy(b.getCreatedBy())
                    .createdByName(creator != null ? creator.getRealName() : "系统自动")
                    .createdAt(b.getCreatedAt())
                    .isOverdue(isOverdue)
                    .items(itemMap.getOrDefault(b.getId(), Collections.emptyList()))
                    .build();
        }).collect(Collectors.toList());
    }

    private String getBillingStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "DRAFT" -> "草稿";
            case "UNPAID" -> "待支付";
            case "PARTIAL" -> "部分支付";
            case "PAID" -> "已付清";
            case "OVERDUE" -> "已逾期";
            case "CANCELLED" -> "已作废";
            default -> status;
        };
    }
}
