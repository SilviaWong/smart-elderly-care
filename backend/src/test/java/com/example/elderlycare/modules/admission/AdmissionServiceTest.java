package com.example.elderlycare.modules.admission;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.dto.AdmissionCheckInDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionDischargeDTO;
import com.example.elderlycare.modules.admission.dto.AdmissionTransferDTO;
import com.example.elderlycare.modules.admission.entity.Admission;
import com.example.elderlycare.modules.admission.entity.Bed;
import com.example.elderlycare.modules.admission.entity.BedChangeLog;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedChangeLogMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.admission.service.impl.AdmissionServiceImpl;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
import com.example.elderlycare.modules.elder.service.ElderService;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceTest {

    @Mock
    private AdmissionMapper admissionMapper;

    @Mock
    private BedMapper bedMapper;

    @Mock
    private BedChangeLogMapper bedChangeLogMapper;

    @Mock
    private BuildingMapper buildingMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private ElderMapper elderMapper;

    @Mock
    private ElderService elderService;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysOperationLogService sysOperationLogService;

    @InjectMocks
    private AdmissionServiceImpl admissionService;

    @BeforeEach
    void setUp() {
        OrgContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("办理入住成功：原子更新床位状态为 OCCUPIED、长者状态为 IN_HOUSE、写入入住记录与变动日志")
    void shouldCheckInSuccessfully() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(99L);

        Elder elder = Elder.builder()
                .id(100L)
                .orgId(1L)
                .elderNo("ELD-100")
                .name("王健安")
                .status("PENDING")
                .build();
        when(elderService.getElderEntityById(100L)).thenReturn(elder);
        when(admissionMapper.selectCount(any())).thenReturn(0L);

        Bed bed = Bed.builder()
                .id(200L)
                .orgId(1L)
                .roomId(10L)
                .bedNo("101-01")
                .status("AVAILABLE")
                .version(0)
                .build();
        when(bedMapper.selectByIdForUpdate(200L)).thenReturn(bed);

        AdmissionCheckInDTO dto = AdmissionCheckInDTO.builder()
                .elderId(100L)
                .bedId(200L)
                .admissionDate(LocalDate.now())
                .plannedDischargeDate(LocalDate.now().plusMonths(6))
                .careLevelCode("CARE_LEVEL_2")
                .depositAmount(new BigDecimal("5000.00"))
                .contractNo("CT-2026-001")
                .remark("初次入住")
                .build();

        admissionService.checkIn(dto);

        // 验证床位被独占更新为 OCCUPIED，且版本号自增
        assertEquals("OCCUPIED", bed.getStatus());
        assertEquals(1, bed.getVersion());
        verify(bedMapper).updateById(bed);

        // 验证长者状态更新为 IN_HOUSE
        verify(elderService).updateElderStatus(100L, "IN_HOUSE");

        // 验证入住记录写入
        ArgumentCaptor<Admission> admissionCaptor = ArgumentCaptor.forClass(Admission.class);
        verify(admissionMapper).insert(admissionCaptor.capture());
        Admission inserted = admissionCaptor.getValue();
        assertEquals(100L, inserted.getElderId());
        assertEquals(200L, inserted.getBedId());
        assertEquals("ACTIVE", inserted.getStatus());
        assertTrue(inserted.getAdmissionNo().startsWith("ADM"));

        // 验证床位变动日志写入
        ArgumentCaptor<BedChangeLog> logCaptor = ArgumentCaptor.forClass(BedChangeLog.class);
        verify(bedChangeLogMapper).insert(logCaptor.capture());
        BedChangeLog log = logCaptor.getValue();
        assertEquals("CHECK_IN", log.getChangeType());
        assertNull(log.getFromBedId());
        assertEquals(200L, log.getToBedId());

        // 验证系统审计日志
        verify(sysOperationLogService).recordLog(eq("ADMISSION"), eq("CHECK_IN"), eq("ADMISSION"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("长者已有生效中的入住记录时办理入住必须被拒绝")
    void shouldRejectCheckInWhenElderAlreadyHasActiveAdmission() {
        OrgContextHolder.setOrgId(1L);

        Elder elder = Elder.builder()
                .id(101L)
                .orgId(1L)
                .elderNo("ELD-101")
                .name("张阿姨")
                .status("IN_HOUSE")
                .build();
        when(elderService.getElderEntityById(101L)).thenReturn(elder);
        when(admissionMapper.selectCount(any())).thenReturn(1L);

        AdmissionCheckInDTO dto = AdmissionCheckInDTO.builder()
                .elderId(101L)
                .bedId(200L)
                .admissionDate(LocalDate.now())
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            admissionService.checkIn(dto);
        });

        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已有生效中的在住记录"));
        verify(bedMapper, never()).selectByIdForUpdate(any());
        verify(admissionMapper, never()).insert(any(Admission.class));
    }

    @Test
    @DisplayName("目标床位已被占用、维修或停用时办理入住必须被拦截")
    void shouldRejectCheckInWhenBedIsNotAvailable() {
        OrgContextHolder.setOrgId(1L);

        Elder elder = Elder.builder().id(102L).orgId(1L).status("PENDING").build();
        when(elderService.getElderEntityById(102L)).thenReturn(elder);
        when(admissionMapper.selectCount(any())).thenReturn(0L);

        Bed occupiedBed = Bed.builder()
                .id(201L)
                .orgId(1L)
                .bedNo("101-02")
                .status("MAINTENANCE")
                .version(1)
                .build();
        when(bedMapper.selectByIdForUpdate(201L)).thenReturn(occupiedBed);

        AdmissionCheckInDTO dto = AdmissionCheckInDTO.builder()
                .elderId(102L)
                .bedId(201L)
                .admissionDate(LocalDate.now())
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            admissionService.checkIn(dto);
        });

        assertEquals(ErrorCode.CONFLICT.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("目标床位不可用或已被他人抢占"));
        verify(admissionMapper, never()).insert(any(Admission.class));
    }

    @Test
    @DisplayName("跨机构办理入住必须抛出 ORG_ACCESS_DENIED 异常")
    void shouldRejectCrossOrgCheckIn() {
        OrgContextHolder.setOrgId(1L);

        Elder elder = Elder.builder().id(103L).orgId(1L).status("PENDING").build();
        when(elderService.getElderEntityById(103L)).thenReturn(elder);
        when(admissionMapper.selectCount(any())).thenReturn(0L);

        // 机构 2 的床位
        Bed org2Bed = Bed.builder().id(202L).orgId(2L).status("AVAILABLE").build();
        when(bedMapper.selectByIdForUpdate(202L)).thenReturn(org2Bed);

        AdmissionCheckInDTO dto = AdmissionCheckInDTO.builder()
                .elderId(103L)
                .bedId(202L)
                .admissionDate(LocalDate.now())
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            admissionService.checkIn(dto);
        });

        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        verify(admissionMapper, never()).insert(any(Admission.class));
    }

    @Test
    @DisplayName("办理调床时严格按床位ID升序加锁以防止并发死锁，并原子释放旧床与锁定新床")
    void shouldTransferBedWithDeadlockFreeLockOrdering() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(88L);

        Admission admission = Admission.builder()
                .id(50L)
                .orgId(1L)
                .elderId(100L)
                .bedId(250L) // 当前旧床位 ID 大
                .status("ACTIVE")
                .build();
        when(admissionMapper.selectById(50L)).thenReturn(admission);

        Bed oldBed = Bed.builder().id(250L).orgId(1L).bedNo("201-01").status("OCCUPIED").version(1).build();
        Bed newBed = Bed.builder().id(210L).orgId(1L).bedNo("101-02").status("AVAILABLE").version(0).build();

        // 无论入参顺序如何，加锁必须先 210L 再 250L
        when(bedMapper.selectByIdForUpdate(210L)).thenReturn(newBed);
        when(bedMapper.selectByIdForUpdate(250L)).thenReturn(oldBed);

        AdmissionTransferDTO transferDTO = AdmissionTransferDTO.builder()
                .newBedId(210L)
                .reason("长者要求搬迁到一楼")
                .build();

        admissionService.transferBed(50L, transferDTO);

        // 验证加锁顺序是严格升序 (210L -> 250L)
        InOrder inOrder = inOrder(bedMapper);
        inOrder.verify(bedMapper).selectByIdForUpdate(210L);
        inOrder.verify(bedMapper).selectByIdForUpdate(250L);

        // 验证旧床位释放为 AVAILABLE
        assertEquals("AVAILABLE", oldBed.getStatus());
        assertEquals(2, oldBed.getVersion());
        verify(bedMapper).updateById(oldBed);

        // 验证新床位占用为 OCCUPIED
        assertEquals("OCCUPIED", newBed.getStatus());
        assertEquals(1, newBed.getVersion());
        verify(bedMapper).updateById(newBed);

        // 验证入住记录床位变更
        assertEquals(210L, admission.getBedId());
        verify(admissionMapper).updateById(admission);

        // 验证变动日志
        ArgumentCaptor<BedChangeLog> logCaptor = ArgumentCaptor.forClass(BedChangeLog.class);
        verify(bedChangeLogMapper).insert(logCaptor.capture());
        BedChangeLog log = logCaptor.getValue();
        assertEquals("TRANSFER", log.getChangeType());
        assertEquals(250L, log.getFromBedId());
        assertEquals(210L, log.getToBedId());
    }

    @Test
    @DisplayName("办理退住成功：释放床位、更新入住记录与长者状态为 DISCHARGED 并写日志")
    void shouldDischargeSuccessfully() {
        OrgContextHolder.setOrgId(1L);
        OrgContextHolder.setUserId(88L);

        Admission admission = Admission.builder()
                .id(60L)
                .orgId(1L)
                .admissionNo("ADM20261101001")
                .elderId(100L)
                .bedId(300L)
                .admissionDate(LocalDate.of(2026, 1, 1))
                .status("ACTIVE")
                .depositAmount(BigDecimal.ZERO)
                .build();
        when(admissionMapper.selectById(60L)).thenReturn(admission);

        Bed bed = Bed.builder().id(300L).orgId(1L).bedNo("301-01").status("OCCUPIED").version(3).build();
        when(bedMapper.selectByIdForUpdate(300L)).thenReturn(bed);

        AdmissionDischargeDTO dto = AdmissionDischargeDTO.builder()
                .actualDischargeDate(LocalDate.of(2026, 11, 1))
                .dischargeReason("康复回家")
                .build();

        admissionService.discharge(60L, dto);

        // 验证入住状态更新为 DISCHARGED
        assertEquals("DISCHARGED", admission.getStatus());
        assertEquals(LocalDate.of(2026, 11, 1), admission.getActualDischargeDate());
        verify(admissionMapper).updateById(admission);

        // 验证床位释放为 AVAILABLE
        assertEquals("AVAILABLE", bed.getStatus());
        assertEquals(4, bed.getVersion());
        verify(bedMapper).updateById(bed);

        // 验证长者状态更新为 DISCHARGED
        verify(elderService).updateElderStatus(100L, "DISCHARGED");

        // 验证变动日志
        ArgumentCaptor<BedChangeLog> logCaptor = ArgumentCaptor.forClass(BedChangeLog.class);
        verify(bedChangeLogMapper).insert(logCaptor.capture());
        assertEquals("DISCHARGE", logCaptor.getValue().getChangeType());
        assertEquals(300L, logCaptor.getValue().getFromBedId());
        assertNull(logCaptor.getValue().getToBedId());
    }

    @Test
    @DisplayName("实际退住日期早于入住日期时必须抛出 PARAM_ERROR 异常")
    void shouldRejectDischargeWhenDateIsInvalid() {
        OrgContextHolder.setOrgId(1L);

        Admission admission = Admission.builder()
                .id(61L)
                .orgId(1L)
                .admissionDate(LocalDate.of(2026, 6, 1))
                .status("ACTIVE")
                .build();
        when(admissionMapper.selectById(61L)).thenReturn(admission);

        AdmissionDischargeDTO dto = AdmissionDischargeDTO.builder()
                .actualDischargeDate(LocalDate.of(2026, 5, 1)) // 早于入住日期
                .dischargeReason("提前离开")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            admissionService.discharge(61L, dto);
        });

        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("实际退住日期不得早于入住日期"));
    }
}
