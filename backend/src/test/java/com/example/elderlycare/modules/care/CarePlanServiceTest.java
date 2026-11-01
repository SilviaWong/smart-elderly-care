package com.example.elderlycare.modules.care;

import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.care.dto.CarePlanCreateDTO;
import com.example.elderlycare.modules.care.dto.CarePlanItemDTO;
import com.example.elderlycare.modules.care.entity.CareItem;
import com.example.elderlycare.modules.care.entity.CareLevel;
import com.example.elderlycare.modules.care.entity.CarePlan;
import com.example.elderlycare.modules.care.entity.CarePlanItem;
import com.example.elderlycare.modules.care.mapper.CareItemMapper;
import com.example.elderlycare.modules.care.mapper.CareLevelMapper;
import com.example.elderlycare.modules.care.mapper.CarePlanItemMapper;
import com.example.elderlycare.modules.care.mapper.CarePlanMapper;
import com.example.elderlycare.modules.care.service.impl.CarePlanServiceImpl;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("长者护理计划制定与明细项级联维护测试")
class CarePlanServiceTest {

    @Mock
    private CarePlanMapper carePlanMapper;

    @Mock
    private CarePlanItemMapper carePlanItemMapper;

    @Mock
    private CareLevelMapper careLevelMapper;

    @Mock
    private CareItemMapper careItemMapper;

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
    private CarePlanServiceImpl carePlanService;

    private static final Long ORG_ID = 1L;
    private static final Long OTHER_ORG_ID = 2L;
    private static final Long ELDER_ID = 100L;
    private static final Long LEVEL_ID = 1L;
    private static final Long ITEM_ID = 5L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(ORG_ID);
        OrgContextHolder.setUserId(1L);
        OrgContextHolder.setUsername("test_admin");
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("制定护理计划成功: 级联保存计划主体与计划明细项")
    void testCreatePlan_Success() {
        Elder elder = Elder.builder().id(ELDER_ID).orgId(ORG_ID).name("张建国").build();
        when(elderMapper.selectById(ELDER_ID)).thenReturn(elder);

        CareLevel level = CareLevel.builder().id(LEVEL_ID).orgId(ORG_ID).levelName("二级护理").build();
        when(careLevelMapper.selectById(LEVEL_ID)).thenReturn(level);

        CareItem careItem = CareItem.builder().id(ITEM_ID).orgId(ORG_ID).itemName("口腔清洁").build();
        when(careItemMapper.selectById(ITEM_ID)).thenReturn(careItem);

        when(carePlanMapper.insert(any(CarePlan.class))).thenAnswer(invocation -> {
            CarePlan p = invocation.getArgument(0);
            p.setId(888L);
            return 1;
        });

        CarePlanItemDTO itemDTO = CarePlanItemDTO.builder()
                .careItemId(ITEM_ID)
                .frequencyType("DAILY")
                .timeWindowStart(LocalTime.of(8, 0))
                .timeWindowEnd(LocalTime.of(9, 0))
                .build();

        CarePlanCreateDTO dto = CarePlanCreateDTO.builder()
                .elderId(ELDER_ID)
                .careLevelId(LEVEL_ID)
                .startDate(LocalDate.of(2026, 11, 1))
                .items(List.of(itemDTO))
                .remark("日常护理计划")
                .build();

        Long planId = carePlanService.createPlan(dto);
        assertEquals(888L, planId);

        ArgumentCaptor<CarePlan> planCaptor = ArgumentCaptor.forClass(CarePlan.class);
        verify(carePlanMapper).insert(planCaptor.capture());
        assertEquals("ACTIVE", planCaptor.getValue().getStatus());

        ArgumentCaptor<CarePlanItem> itemCaptor = ArgumentCaptor.forClass(CarePlanItem.class);
        verify(carePlanItemMapper).insert(itemCaptor.capture());
        assertEquals(ITEM_ID, itemCaptor.getValue().getCareItemId());
        assertEquals(888L, itemCaptor.getValue().getPlanId());
    }

    @Test
    @DisplayName("制定护理计划校验: 跨机构长者抛出 ORG_ACCESS_DENIED 异常")
    void testCreatePlan_ThrowsException_WhenCrossOrgElder() {
        Elder elder = Elder.builder().id(ELDER_ID).orgId(OTHER_ORG_ID).name("其他机构长者").build();
        when(elderMapper.selectById(ELDER_ID)).thenReturn(elder);

        CarePlanCreateDTO dto = CarePlanCreateDTO.builder()
                .elderId(ELDER_ID)
                .careLevelId(LEVEL_ID)
                .startDate(LocalDate.now())
                .items(List.of(CarePlanItemDTO.builder().careItemId(ITEM_ID).build()))
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> carePlanService.createPlan(dto));
        assertEquals(ErrorCode.ORG_ACCESS_DENIED.getCode(), ex.getCode());
        verify(carePlanMapper, never()).insert(any(CarePlan.class));
    }
}
