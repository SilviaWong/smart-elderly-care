package com.example.elderlycare.modules.dining;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.admission.mapper.AdmissionMapper;
import com.example.elderlycare.modules.admission.mapper.BedMapper;
import com.example.elderlycare.modules.admission.mapper.BuildingMapper;
import com.example.elderlycare.modules.admission.mapper.RoomMapper;
import com.example.elderlycare.modules.dining.dto.MealOrderCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealOrderItemDTO;
import com.example.elderlycare.modules.dining.entity.Dish;
import com.example.elderlycare.modules.dining.entity.MealOrder;
import com.example.elderlycare.modules.dining.entity.MealOrderItem;
import com.example.elderlycare.modules.dining.mapper.DishMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderMapper;
import com.example.elderlycare.modules.dining.service.impl.MealOrderServiceImpl;
import com.example.elderlycare.modules.dining.vo.DietaryCheckResultVO;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MealOrderServiceTest {

    @Mock
    private MealOrderMapper mealOrderMapper;

    @Mock
    private MealOrderItemMapper mealOrderItemMapper;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private MealMenuMapper mealMenuMapper;

    @Mock
    private MealMenuItemMapper mealMenuItemMapper;

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
    private MealOrderServiceImpl mealOrderService;

    private final Long orgId = 1L;
    private final Long userId = 100L;

    @BeforeEach
    void setUp() {
        OrgContextHolder.setOrgId(orgId);
        OrgContextHolder.setUserId(userId);
    }

    @AfterEach
    void tearDown() {
        OrgContextHolder.clear();
    }

    @Test
    @DisplayName("测试正常订餐并固化菜品名称与价格快照，校验总金额")
    void testCreateOrder_success_and_snapshotIntegrity() {
        Long elderId = 1L;
        Elder elder = Elder.builder()
                .id(elderId)
                .orgId(orgId)
                .name("张建国")
                .elderNo("ELD-001")
                .allergyDesc("青霉素过敏")
                .dietTaboo("低盐低脂")
                .build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        Dish dish1 = Dish.builder()
                .id(10L)
                .orgId(orgId)
                .dishCode("DISH001")
                .dishName("小米南瓜粥")
                .price(new BigDecimal("6.00"))
                .status("ACTIVE")
                .build();

        Dish dish2 = Dish.builder()
                .id(20L)
                .orgId(orgId)
                .dishCode("DISH005")
                .dishName("清炒软烂时蔬")
                .price(new BigDecimal("12.00"))
                .status("ACTIVE")
                .build();

        when(dishMapper.selectBatchIds(List.of(10L, 20L))).thenReturn(List.of(dish1, dish2));

        doAnswer(invocation -> {
            MealOrder order = invocation.getArgument(0);
            order.setId(999L);
            return 1;
        }).when(mealOrderMapper).insert(any(MealOrder.class));

        MealOrderCreateDTO dto = MealOrderCreateDTO.builder()
                .elderId(elderId)
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .items(List.of(
                        MealOrderItemDTO.builder().dishId(10L).quantity(2).build(), // 6.00 * 2 = 12.00
                        MealOrderItemDTO.builder().dishId(20L).quantity(1).build()  // 12.00 * 1 = 12.00
                ))
                .build();

        Long orderId = mealOrderService.createOrder(dto);

        assertEquals(999L, orderId);

        // 验证订单主表总金额等于 24.00
        verify(mealOrderMapper, times(1)).insert(argThat(order ->
                order.getTotalAmount().compareTo(new BigDecimal("24.00")) == 0 &&
                "CONFIRMED".equals(order.getStatus()) &&
                order.getOrderNo().startsWith("MLO")
        ));

        // 验证订单明细保存了名称快照和单价快照
        verify(mealOrderItemMapper, times(1)).insert(argThat(item ->
                item.getDishId().equals(10L) &&
                "小米南瓜粥".equals(item.getDishNameSnapshot()) &&
                item.getUnitPrice().compareTo(new BigDecimal("6.00")) == 0 &&
                item.getLineAmount().compareTo(new BigDecimal("12.00")) == 0
        ));
        verify(mealOrderItemMapper, times(1)).insert(argThat(item ->
                item.getDishId().equals(20L) &&
                "清炒软烂时蔬".equals(item.getDishNameSnapshot()) &&
                item.getUnitPrice().compareTo(new BigDecimal("12.00")) == 0 &&
                item.getLineAmount().compareTo(new BigDecimal("12.00")) == 0
        ));
    }

    @Test
    @DisplayName("测试长者过敏原与饮食禁忌智能识别筛查")
    void testCheckDietaryWarning_detectsAllergenAndTaboo() {
        Long elderId = 2L;
        Elder elder = Elder.builder()
                .id(elderId)
                .orgId(orgId)
                .name("李大爷")
                .allergyDesc("海鲜严重过敏、花生过敏")
                .dietTaboo("禁食辛辣、低盐低脂")
                .build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        Dish seafoodDish = Dish.builder()
                .id(30L)
                .dishName("清蒸鲈鱼片")
                .allergens("鱼类/海鲜")
                .build();

        Dish spicyDish = Dish.builder()
                .id(40L)
                .dishName("麻辣鲜豆腐")
                .description("特制麻辣川味豆腐")
                .build();

        when(dishMapper.selectBatchIds(List.of(30L, 40L))).thenReturn(List.of(seafoodDish, spicyDish));

        DietaryCheckResultVO result = mealOrderService.checkDietaryWarning(elderId, List.of(30L, 40L));

        assertTrue(result.getHasWarning());
        assertEquals(2, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("海鲜"));
        assertTrue(result.getWarnings().get(1).contains("辛辣"));
    }

    @Test
    @DisplayName("测试触发过敏警告未勾选强行下单时被阻断，勾选后放行")
    void testCreateOrder_blockedByAllergenWarning_unlessOverridden() {
        Long elderId = 2L;
        Elder elder = Elder.builder()
                .id(elderId)
                .orgId(orgId)
                .name("李大爷")
                .allergyDesc("海鲜过敏")
                .build();
        when(elderMapper.selectById(elderId)).thenReturn(elder);

        Dish seafoodDish = Dish.builder()
                .id(30L)
                .orgId(orgId)
                .dishCode("DISH003")
                .dishName("清蒸鲈鱼片")
                .allergens("海鲜")
                .price(new BigDecimal("28.00"))
                .status("ACTIVE")
                .build();
        when(dishMapper.selectBatchIds(List.of(30L))).thenReturn(List.of(seafoodDish));

        MealOrderCreateDTO dtoWithoutOverride = MealOrderCreateDTO.builder()
                .elderId(elderId)
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .items(List.of(MealOrderItemDTO.builder().dishId(30L).quantity(1).build()))
                .overrideWarning(false)
                .build();

        // 未二次确认时应当拦截抛出异常
        BusinessException ex = assertThrows(BusinessException.class, () ->
                mealOrderService.createOrder(dtoWithoutOverride)
        );
        assertTrue(ex.getMessage().contains("触发长者饮食过敏或禁忌提醒"));

        // 二次确认强行下单应当放行
        MealOrderCreateDTO dtoWithOverride = MealOrderCreateDTO.builder()
                .elderId(elderId)
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .items(List.of(MealOrderItemDTO.builder().dishId(30L).quantity(1).build()))
                .overrideWarning(true)
                .build();

        mealOrderService.createOrder(dtoWithOverride);
        verify(mealOrderMapper, times(1)).insert(any(MealOrder.class));
    }

    @Test
    @DisplayName("测试在取消截止时间前成功取消订单")
    void testCancelOrder_beforeCutoff_success() {
        Long orderId = 1001L;
        // 未来的订单（明天午餐），尚未到达截止时间
        MealOrder order = MealOrder.builder()
                .id(orderId)
                .orgId(orgId)
                .orderNo("MLO202611020001")
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .status("CONFIRMED")
                .build();

        when(mealOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        mealOrderService.cancelOrder(orderId);

        assertEquals("CANCELLED", order.getStatus());
        verify(mealOrderMapper, times(1)).updateById(order);
    }

    @Test
    @DisplayName("测试超过餐次取消截止时间后取消订单被阻断")
    void testCancelOrder_pastCutoff_throwsBusinessException() {
        Long orderId = 1002L;
        // 过去日期的订单，早已超过截止时间
        MealOrder order = MealOrder.builder()
                .id(orderId)
                .orgId(orgId)
                .orderNo("MLO202610310001")
                .orderDate(LocalDate.now().minusDays(1))
                .mealType("LUNCH")
                .status("CONFIRMED")
                .build();

        when(mealOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                mealOrderService.cancelOrder(orderId)
        );

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("当前已超过取消截止时间"));
        verify(mealOrderMapper, never()).updateById(argThat(o -> "CANCELLED".equals(o.getStatus())));
    }

    @Test
    @DisplayName("测试已完成配送的订单不可取消")
    void testCancelOrder_alreadyDelivered_throwsBusinessException() {
        Long orderId = 1003L;
        MealOrder order = MealOrder.builder()
                .id(orderId)
                .orgId(orgId)
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .status("DELIVERED")
                .build();

        when(mealOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                mealOrderService.cancelOrder(orderId)
        );

        assertTrue(ex.getMessage().contains("已配送完成的餐次订单无法取消"));
    }

    @Test
    @DisplayName("测试跨机构越权取消被拦截 (多租户数据隔离)")
    void testMultiTenantIsolation_denied() {
        Long otherOrgId = 99L;
        Long orderId = 1004L;
        MealOrder order = MealOrder.builder()
                .id(orderId)
                .orgId(otherOrgId)
                .orderDate(LocalDate.now().plusDays(1))
                .mealType("LUNCH")
                .status("CONFIRMED")
                .build();

        when(mealOrderMapper.selectByIdForUpdate(orderId)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                mealOrderService.cancelOrder(orderId)
        );

        assertEquals(ErrorCode.ORG_ACCESS_DENIED, ex.getErrorCode());
    }
}
