package com.example.elderlycare.modules.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.care.entity.CareTask;
import com.example.elderlycare.modules.care.mapper.CareTaskMapper;
import com.example.elderlycare.modules.finance.entity.Billing;
import com.example.elderlycare.modules.finance.mapper.BillingMapper;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import com.example.elderlycare.modules.health.mapper.HealthAlertMapper;
import com.example.elderlycare.modules.iot.entity.DeviceData;
import com.example.elderlycare.modules.iot.mapper.DeviceDataMapper;
import com.example.elderlycare.modules.report.service.DashboardService;
import com.example.elderlycare.modules.report.vo.DashboardStatsVO;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import com.example.elderlycare.modules.risk.mapper.RiskEventMapper;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import com.example.elderlycare.modules.serviceorder.mapper.ServiceOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final ServiceOrderMapper serviceOrderMapper;
    private final CareTaskMapper careTaskMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final BillingMapper billingMapper;
    private final RiskEventMapper riskEventMapper;
    private final DeviceDataMapper deviceDataMapper;

    @Override
    public DashboardStatsVO getDashboardStats() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        // 1. 在住长者人数 (Admission status = CHECKED_IN)
        LambdaQueryWrapper<Admission> admQw = new LambdaQueryWrapper<Admission>()
                .eq(Admission::getStatus, "CHECKED_IN");
        if (currentOrgId != null) {
            admQw.eq(Admission::getOrgId, currentOrgId);
        }
        Long activeElderCount = admissionMapper.selectCount(admQw);

        // 2. 床位总数与占用率
        LambdaQueryWrapper<Bed> bedTotalQw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            bedTotalQw.eq(Bed::getOrgId, currentOrgId);
        }
        Long totalBedCount = bedMapper.selectCount(bedTotalQw);

        LambdaQueryWrapper<Bed> bedOccQw = new LambdaQueryWrapper<Bed>()
                .eq(Bed::getStatus, "OCCUPIED");
        if (currentOrgId != null) {
            bedOccQw.eq(Bed::getOrgId, currentOrgId);
        }
        Long occupiedBedCount = bedMapper.selectCount(bedOccQw);

        BigDecimal bedOccupancyRate = BigDecimal.ZERO;
        if (totalBedCount != null && totalBedCount > 0 && occupiedBedCount != null) {
            bedOccupancyRate = BigDecimal.valueOf(occupiedBedCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalBedCount), 2, RoundingMode.HALF_UP);
        }

        // 3. 待处理服务工单 (PENDING_ASSIGN, ASSIGNED, IN_PROGRESS)
        LambdaQueryWrapper<ServiceOrder> orderQw = new LambdaQueryWrapper<ServiceOrder>()
                .in(ServiceOrder::getStatus, Arrays.asList("PENDING_ASSIGN", "ASSIGNED", "IN_PROGRESS"));
        if (currentOrgId != null) {
            orderQw.eq(ServiceOrder::getOrgId, currentOrgId);
        }
        Long pendingOrderCount = serviceOrderMapper.selectCount(orderQw);

        // 4. 今日护理任务完成率
        LambdaQueryWrapper<CareTask> careTotalQw = new LambdaQueryWrapper<CareTask>()
                .eq(CareTask::getTaskDate, today);
        if (currentOrgId != null) {
            careTotalQw.eq(CareTask::getOrgId, currentOrgId);
        }
        Long todayCareTasks = careTaskMapper.selectCount(careTotalQw);

        LambdaQueryWrapper<CareTask> careCompQw = new LambdaQueryWrapper<CareTask>()
                .eq(CareTask::getTaskDate, today)
                .eq(CareTask::getStatus, "COMPLETED");
        if (currentOrgId != null) {
            careCompQw.eq(CareTask::getOrgId, currentOrgId);
        }
        Long todayCompletedCare = careTaskMapper.selectCount(careCompQw);

        BigDecimal careCompletionRate = BigDecimal.valueOf(100.00).setScale(2, RoundingMode.HALF_UP);
        if (todayCareTasks != null && todayCareTasks > 0 && todayCompletedCare != null) {
            careCompletionRate = BigDecimal.valueOf(todayCompletedCare)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(todayCareTasks), 2, RoundingMode.HALF_UP);
        }

        // 5. 待处理健康体征预警数 (PENDING, PROCESSING)
        LambdaQueryWrapper<HealthAlert> alertQw = new LambdaQueryWrapper<HealthAlert>()
                .in(HealthAlert::getStatus, Arrays.asList("PENDING", "PROCESSING"));
        if (currentOrgId != null) {
            alertQw.eq(HealthAlert::getOrgId, currentOrgId);
        }
        Long healthAlertCount = healthAlertMapper.selectCount(alertQw);

        // 6. 当月财务实收总额 (账单 bill_month 为当月)
        String currentMonth = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        LambdaQueryWrapper<Billing> billQw = new LambdaQueryWrapper<Billing>()
                .eq(Billing::getBillMonth, currentMonth)
                .ne(Billing::getStatus, "CANCELLED");
        if (currentOrgId != null) {
            billQw.eq(Billing::getOrgId, currentOrgId);
        }
        List<Billing> billings = billingMapper.selectList(billQw);
        BigDecimal currentMonthRevenue = billings.stream()
                .map(Billing::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // 7. 待处置安全风险事件数 (非 CLOSED 状态)
        LambdaQueryWrapper<RiskEvent> riskQw = new LambdaQueryWrapper<RiskEvent>()
                .ne(RiskEvent::getStatus, "CLOSED");
        if (currentOrgId != null) {
            riskQw.eq(RiskEvent::getOrgId, currentOrgId);
        }
        List<RiskEvent> openRiskEvents = riskEventMapper.selectList(riskQw);
        long openRiskEventCount = openRiskEvents.size();

        Map<String, Long> riskDistribution = new LinkedHashMap<>();
        riskDistribution.put("CRITICAL", openRiskEvents.stream().filter(r -> "CRITICAL".equalsIgnoreCase(r.getRiskLevel())).count());
        riskDistribution.put("HIGH", openRiskEvents.stream().filter(r -> "HIGH".equalsIgnoreCase(r.getRiskLevel())).count());
        riskDistribution.put("MEDIUM", openRiskEvents.stream().filter(r -> "MEDIUM".equalsIgnoreCase(r.getRiskLevel())).count());
        riskDistribution.put("LOW", openRiskEvents.stream().filter(r -> "LOW".equalsIgnoreCase(r.getRiskLevel())).count());

        // 8. 今日物联网设备遥测点采集总数
        LambdaQueryWrapper<DeviceData> dataQw = new LambdaQueryWrapper<DeviceData>()
                .ge(DeviceData::getReceivedAt, todayStart);
        if (currentOrgId != null) {
            dataQw.eq(DeviceData::getOrgId, currentOrgId);
        }
        Long todayTelemetryCount = deviceDataMapper.selectCount(dataQw);

        // 9. 近 7 日入住/退住流转趋势
        List<DashboardStatsVO.DailyTrendItem> admissionTrend = new ArrayList<>();
        // 10. 近 7 日护理任务趋势
        List<DashboardStatsVO.CareTaskTrendItem> careTaskTrend = new ArrayList<>();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String dStr = d.format(dtf);

            // 入住数与退住数
            LambdaQueryWrapper<Admission> checkInQw = new LambdaQueryWrapper<Admission>()
                    .eq(Admission::getAdmissionDate, d);
            if (currentOrgId != null) checkInQw.eq(Admission::getOrgId, currentOrgId);
            Long inCount = admissionMapper.selectCount(checkInQw);

            LambdaQueryWrapper<Admission> dischargeQw = new LambdaQueryWrapper<Admission>()
                    .eq(Admission::getActualDischargeDate, d);
            if (currentOrgId != null) dischargeQw.eq(Admission::getOrgId, currentOrgId);
            Long outCount = admissionMapper.selectCount(dischargeQw);

            admissionTrend.add(DashboardStatsVO.DailyTrendItem.builder()
                    .date(dStr)
                    .checkInCount(inCount != null ? inCount : 0L)
                    .dischargeCount(outCount != null ? outCount : 0L)
                    .build());

            // 护理任务
            LambdaQueryWrapper<CareTask> ctTotal = new LambdaQueryWrapper<CareTask>().eq(CareTask::getTaskDate, d);
            if (currentOrgId != null) ctTotal.eq(CareTask::getOrgId, currentOrgId);
            Long ctTot = careTaskMapper.selectCount(ctTotal);

            LambdaQueryWrapper<CareTask> ctDone = new LambdaQueryWrapper<CareTask>().eq(CareTask::getTaskDate, d).eq(CareTask::getStatus, "COMPLETED");
            if (currentOrgId != null) ctDone.eq(CareTask::getOrgId, currentOrgId);
            Long ctDn = careTaskMapper.selectCount(ctDone);

            careTaskTrend.add(DashboardStatsVO.CareTaskTrendItem.builder()
                    .date(dStr)
                    .totalCount(ctTot != null ? ctTot : 0L)
                    .completedCount(ctDn != null ? ctDn : 0L)
                    .build());
        }

        return DashboardStatsVO.builder()
                .activeElderCount(activeElderCount != null ? activeElderCount : 0L)
                .totalBedCount(totalBedCount != null ? totalBedCount : 0L)
                .occupiedBedCount(occupiedBedCount != null ? occupiedBedCount : 0L)
                .bedOccupancyRate(bedOccupancyRate)
                .pendingOrderCount(pendingOrderCount != null ? pendingOrderCount : 0L)
                .careCompletionRate(careCompletionRate)
                .healthAlertCount(healthAlertCount != null ? healthAlertCount : 0L)
                .currentMonthRevenue(currentMonthRevenue)
                .openRiskEventCount(openRiskEventCount)
                .todayTelemetryCount(todayTelemetryCount != null ? todayTelemetryCount : 0L)
                .admissionTrend(admissionTrend)
                .careTaskTrend(careTaskTrend)
                .riskDistribution(riskDistribution)
                .build();
    }
}
