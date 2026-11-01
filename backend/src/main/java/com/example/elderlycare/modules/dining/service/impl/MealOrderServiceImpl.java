package com.example.elderlycare.modules.dining.service.impl;

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
import com.example.elderlycare.modules.dining.dto.MealOrderCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealOrderItemDTO;
import com.example.elderlycare.modules.dining.dto.MealOrderQueryDTO;
import com.example.elderlycare.modules.dining.entity.Dish;
import com.example.elderlycare.modules.dining.entity.MealMenu;
import com.example.elderlycare.modules.dining.entity.MealOrder;
import com.example.elderlycare.modules.dining.entity.MealOrderItem;
import com.example.elderlycare.modules.dining.mapper.DishMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealOrderMapper;
import com.example.elderlycare.modules.dining.service.MealOrderService;
import com.example.elderlycare.modules.dining.vo.*;
import com.example.elderlycare.modules.elder.entity.Elder;
import com.example.elderlycare.modules.elder.mapper.ElderMapper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealOrderServiceImpl implements MealOrderService {

    private final MealOrderMapper mealOrderMapper;
    private final MealOrderItemMapper mealOrderItemMapper;
    private final DishMapper dishMapper;
    private final MealMenuMapper mealMenuMapper;
    private final MealMenuItemMapper mealMenuItemMapper;
    private final ElderMapper elderMapper;
    private final AdmissionMapper admissionMapper;
    private final BedMapper bedMapper;
    private final RoomMapper roomMapper;
    private final BuildingMapper buildingMapper;
    private final SysUserMapper sysUserMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    public DietaryCheckResultVO checkDietaryWarning(Long elderId, List<Long> dishIds) {
        Elder elder = elderMapper.selectById(elderId);
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "长者档案不存在");
        }

        ElderDietaryProfileVO profile = ElderDietaryProfileVO.builder()
                .elderId(elder.getId())
                .elderName(elder.getName())
                .elderNo(elder.getElderNo())
                .allergyDesc(elder.getAllergyDesc())
                .dietTaboo(elder.getDietTaboo())
                .chronicDiseaseDesc(elder.getChronicDiseaseDesc())
                .build();

        if (CollectionUtils.isEmpty(dishIds)) {
            return DietaryCheckResultVO.builder()
                    .hasWarning(false)
                    .warnings(Collections.emptyList())
                    .elderProfile(profile)
                    .build();
        }

        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        List<String> warnings = new ArrayList<>();
        String allergyDesc = elder.getAllergyDesc();
        String dietTaboo = elder.getDietTaboo();

        for (Dish dish : dishes) {
            // 1. 过敏原比对
            if (StringUtils.hasText(dish.getAllergens()) && StringUtils.hasText(allergyDesc)) {
                String[] dishAllergenArray = dish.getAllergens().split("[,，/、\\s]+");
                for (String a : dishAllergenArray) {
                    if (StringUtils.hasText(a) && (allergyDesc.contains(a) || isAllergenFuzzyMatch(allergyDesc, a))) {
                        warnings.add(String.format("长者有过敏史【%s】，所选菜品【%s】含有过敏原【%s】", allergyDesc, dish.getDishName(), a));
                        break;
                    }
                }
            }

            // 2. 膳食禁忌比对 (如低糖、低盐少油、禁食辛辣、高嘌呤海鲜等)
            if (StringUtils.hasText(dietTaboo)) {
                if ((dietTaboo.contains("辛辣") || dietTaboo.contains("辣")) &&
                        (dish.getDishName().contains("辣") || (dish.getDescription() != null && dish.getDescription().contains("辣")))) {
                    warnings.add(String.format("长者饮食禁忌为【%s】，菜品【%s】含辛辣成分不适宜", dietTaboo, dish.getDishName()));
                }
                if ((dietTaboo.contains("海鲜") || dietTaboo.contains("痛风")) &&
                        (dish.getDishName().contains("虾") || dish.getDishName().contains("鱼") || dish.getDishName().contains("蟹") || "鱼类/海鲜".equals(dish.getAllergens()))) {
                    warnings.add(String.format("长者饮食禁忌为【%s】，菜品【%s】含有海鲜嘌呤成分", dietTaboo, dish.getDishName()));
                }
                if ((dietTaboo.contains("少糖") || dietTaboo.contains("低糖") || dietTaboo.contains("糖尿病")) &&
                        (dish.getDishName().contains("甜") || dish.getDishName().contains("糖") || (dish.getDescription() != null && dish.getDescription().contains("糖")))) {
                    warnings.add(String.format("长者饮食禁忌为【%s】，菜品【%s】含糖量较高需注意", dietTaboo, dish.getDishName()));
                }
            }
        }

        return DietaryCheckResultVO.builder()
                .hasWarning(!warnings.isEmpty())
                .warnings(warnings)
                .elderProfile(profile)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(MealOrderCreateDTO dto) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        Long currentUserId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        Elder elder = elderMapper.selectById(dto.getElderId());
        if (elder == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "下单长者不存在");
        }
        if (currentOrgId != null && !currentOrgId.equals(elder.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权为其他机构长者订餐");
        }
        Long orgId = currentOrgId != null ? currentOrgId : elder.getOrgId();

        List<Long> dishIds = dto.getItems().stream().map(MealOrderItemDTO::getDishId).collect(Collectors.toList());
        List<Dish> dishes = dishMapper.selectBatchIds(dishIds);
        if (dishes.size() != dishIds.size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部分选购菜品不存在或已下架");
        }
        Map<Long, Dish> dishMap = dishes.stream().collect(Collectors.toMap(Dish::getId, d -> d));

        // 校验过敏与禁忌
        DietaryCheckResultVO checkResult = checkDietaryWarning(dto.getElderId(), dishIds);
        if (checkResult.getHasWarning() && !Boolean.TRUE.equals(dto.getOverrideWarning())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "所选菜品触发长者饮食过敏或禁忌提醒，请确认后勾选【忽略提示强行下单】再提交！警告详情: " + String.join("; ", checkResult.getWarnings()));
        }

        // 计算订单总金额，使用 BigDecimal 运算
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<MealOrderItem> orderItems = new ArrayList<>();

        for (MealOrderItemDTO itemDto : dto.getItems()) {
            Dish dish = dishMap.get(itemDto.getDishId());
            if (!orgId.equals(dish.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可跨机构订购菜品");
            }
            if (!"ACTIVE".equalsIgnoreCase(dish.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品【" + dish.getDishName() + "】已停售，无法订购");
            }

            BigDecimal unitPrice = dish.getPrice();
            BigDecimal quantity = BigDecimal.valueOf(itemDto.getQuantity());
            BigDecimal lineAmount = unitPrice.multiply(quantity);

            totalAmount = totalAmount.add(lineAmount);

            // 构造订单明细并固化菜品名称和单价快照
            MealOrderItem orderItem = MealOrderItem.builder()
                    .dishId(dish.getId())
                    .dishNameSnapshot(dish.getDishName())
                    .quantity(itemDto.getQuantity())
                    .unitPrice(unitPrice)
                    .lineAmount(lineAmount)
                    .build();
            orderItems.add(orderItem);
        }

        String orderNo = generateOrderNo();
        MealOrder order = MealOrder.builder()
                .orgId(orgId)
                .orderNo(orderNo)
                .elderId(elder.getId())
                .orderDate(dto.getOrderDate())
                .mealType(dto.getMealType().trim().toUpperCase())
                .totalAmount(totalAmount)
                .status("CONFIRMED")
                .orderedBy(currentUserId)
                .createdAt(LocalDateTime.now())
                .build();

        mealOrderMapper.insert(order);

        for (MealOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            mealOrderItemMapper.insert(item);
        }

        sysOperationLogService.recordLog(
                "MEAL_ORDER",
                "CREATE",
                "MEAL_ORDER",
                order.getId(),
                "/api/v1/dining/orders",
                "127.0.0.1",
                String.format("创建订餐订单: 单号=%s, 长者=%s, 日期=%s, 餐次=%s, 金额=%s",
                        orderNo, elder.getName(), order.getOrderDate(), order.getMealType(), totalAmount)
        );

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        MealOrder order = mealOrderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订餐订单不存在");
        }
        checkOrderAccess(order);

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该订单已处于取消状态，无需重复取消");
        }
        if ("DELIVERED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已配送完成的餐次订单无法取消");
        }

        // 按餐次截止时间严格执行取消规则
        if (isPastCancelCutoff(order.getOrderDate(), order.getMealType(), LocalDateTime.now())) {
            String cutoffStr = getCutoffTimeString(order.getMealType());
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    String.format("订单就餐时间为【%s %s】，当前已超过取消截止时间（%s），无法取消，请联系营养餐厅厨房沟通！",
                            order.getOrderDate(), getMealTypeName(order.getMealType()), cutoffStr));
        }

        order.setStatus("CANCELLED");
        mealOrderMapper.updateById(order);

        sysOperationLogService.recordLog(
                "MEAL_ORDER",
                "CANCEL",
                "MEAL_ORDER",
                order.getId(),
                "/api/v1/dining/orders/" + orderId + "/cancel",
                "127.0.0.1",
                String.format("取消订餐订单: 单号=%s, 日期=%s, 餐次=%s",
                        order.getOrderNo(), order.getOrderDate(), order.getMealType())
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverOrder(Long orderId) {
        MealOrder order = mealOrderMapper.selectByIdForUpdate(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订餐订单不存在");
        }
        checkOrderAccess(order);

        if (!"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅已确认的订餐订单可标记为配送完成");
        }

        order.setStatus("DELIVERED");
        mealOrderMapper.updateById(order);
    }

    @Override
    public MealOrderVO getOrderDetail(Long orderId) {
        MealOrder order = mealOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订餐订单不存在");
        }
        checkOrderAccess(order);

        List<MealOrderVO> voList = convertToOrderVOList(Collections.singletonList(order));
        return voList.get(0);
    }

    @Override
    public PageResult<MealOrderVO> pageOrders(MealOrderQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<MealOrder> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(MealOrder::getOrgId, currentOrgId);
        }
        if (query.getElderId() != null) {
            qw.eq(MealOrder::getElderId, query.getElderId());
        }
        if (query.getOrderDate() != null) {
            qw.eq(MealOrder::getOrderDate, query.getOrderDate());
        }
        if (StringUtils.hasText(query.getMealType())) {
            qw.eq(MealOrder::getMealType, query.getMealType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(MealOrder::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(MealOrder::getOrderNo, kw));
        }

        qw.orderByDesc(MealOrder::getOrderDate).orderByDesc(MealOrder::getId);

        Page<MealOrder> page = mealOrderMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<MealOrderVO> voList = convertToOrderVOList(page.getRecords());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DiningSummaryVO getSummary() {
        Long currentOrgId = OrgContextHolder.getOrgId();

        Long activeDishCount = dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>()
                        .eq(currentOrgId != null, Dish::getOrgId, currentOrgId)
                        .eq(Dish::getStatus, "ACTIVE")
        );

        LocalDate today = LocalDate.now();

        Long todayMenuCount = mealMenuMapper.selectCount(
                new LambdaQueryWrapper<MealMenu>()
                        .eq(currentOrgId != null, MealMenu::getOrgId, currentOrgId)
                        .eq(MealMenu::getMenuDate, today)
                        .eq(MealMenu::getStatus, "PUBLISHED")
        );

        Long todayOrderCount = mealOrderMapper.selectCount(
                new LambdaQueryWrapper<MealOrder>()
                        .eq(currentOrgId != null, MealOrder::getOrgId, currentOrgId)
                        .eq(MealOrder::getOrderDate, today)
        );

        Long pendingDeliverCount = mealOrderMapper.selectCount(
                new LambdaQueryWrapper<MealOrder>()
                        .eq(currentOrgId != null, MealOrder::getOrgId, currentOrgId)
                        .eq(MealOrder::getOrderDate, today)
                        .eq(MealOrder::getStatus, "CONFIRMED")
        );

        Long deliveredCount = mealOrderMapper.selectCount(
                new LambdaQueryWrapper<MealOrder>()
                        .eq(currentOrgId != null, MealOrder::getOrgId, currentOrgId)
                        .eq(MealOrder::getOrderDate, today)
                        .eq(MealOrder::getStatus, "DELIVERED")
        );

        List<MealOrder> todayOrders = mealOrderMapper.selectList(
                new LambdaQueryWrapper<MealOrder>()
                        .eq(currentOrgId != null, MealOrder::getOrgId, currentOrgId)
                        .eq(MealOrder::getOrderDate, today)
                        .ne(MealOrder::getStatus, "CANCELLED")
        );

        BigDecimal todayRevenue = todayOrders.stream()
                .map(MealOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DiningSummaryVO.builder()
                .activeDishCount(activeDishCount)
                .todayMenuCount(todayMenuCount)
                .todayOrderCount(todayOrderCount)
                .pendingDeliverCount(pendingDeliverCount)
                .deliveredCount(deliveredCount)
                .todayRevenue(todayRevenue)
                .build();
    }

    /**
     * 判断是否已超过餐次取消截止时间
     * 规则:
     * - 早餐 (BREAKFAST): 订餐当日 07:00
     * - 午餐 (LUNCH): 订餐当日 10:30
     * - 晚餐 (DINNER): 订餐当日 16:30
     */
    public boolean isPastCancelCutoff(LocalDate orderDate, String mealType, LocalDateTime now) {
        if (orderDate == null || mealType == null) {
            return false;
        }
        LocalTime cutoffTime = switch (mealType.toUpperCase()) {
            case "BREAKFAST" -> LocalTime.of(7, 0);
            case "LUNCH" -> LocalTime.of(10, 30);
            case "DINNER" -> LocalTime.of(16, 30);
            default -> LocalTime.of(10, 0);
        };

        LocalDateTime cutoffDateTime = LocalDateTime.of(orderDate, cutoffTime);
        return now.isAfter(cutoffDateTime);
    }

    private String getCutoffTimeString(String mealType) {
        if (mealType == null) return "当日 10:00";
        return switch (mealType.toUpperCase()) {
            case "BREAKFAST" -> "当日 07:00";
            case "LUNCH" -> "当日 10:30";
            case "DINNER" -> "当日 16:30";
            default -> "当日 10:00";
        };
    }

    private boolean isAllergenFuzzyMatch(String allergyDesc, String allergen) {
        if (allergyDesc.contains("海鲜") && (allergen.contains("鱼") || allergen.contains("虾") || allergen.contains("蟹"))) {
            return true;
        }
        if (allergyDesc.contains("蛋") && allergen.contains("蛋")) {
            return true;
        }
        if (allergyDesc.contains("花生") && allergen.contains("坚果")) {
            return true;
        }
        return false;
    }

    private void checkOrderAccess(MealOrder order) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(order.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构订餐记录");
        }
    }

    private String generateOrderNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int rand = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "MLO" + dateStr + rand;
    }

    private List<MealOrderVO> convertToOrderVOList(List<MealOrder> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return Collections.emptyList();
        }

        Set<Long> elderIds = orders.stream().map(MealOrder::getElderId).collect(Collectors.toSet());
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

        Set<Long> orderIds = orders.stream().map(MealOrder::getId).collect(Collectors.toSet());
        List<MealOrderItem> allItems = mealOrderItemMapper.selectList(
                new LambdaQueryWrapper<MealOrderItem>().in(MealOrderItem::getOrderId, orderIds)
        );
        Map<Long, List<MealOrderItemVO>> orderItemsMap = new HashMap<>();
        for (MealOrderItem item : allItems) {
            orderItemsMap.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>())
                    .add(MealOrderItemVO.builder()
                            .id(item.getId())
                            .dishId(item.getDishId())
                            .dishNameSnapshot(item.getDishNameSnapshot())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .lineAmount(item.getLineAmount())
                            .build());
        }

        Set<Long> userIds = orders.stream().map(MealOrder::getOrderedBy).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                sysUserMapper.selectBatchIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, u -> u));

        LocalDateTime now = LocalDateTime.now();

        return orders.stream().map(o -> {
            Elder el = elderMap.get(o.getElderId());
            SysUser user = o.getOrderedBy() != null ? userMap.get(o.getOrderedBy()) : null;
            boolean pastCutoff = isPastCancelCutoff(o.getOrderDate(), o.getMealType(), now);

            return MealOrderVO.builder()
                    .id(o.getId())
                    .orgId(o.getOrgId())
                    .orderNo(o.getOrderNo())
                    .elderId(o.getElderId())
                    .elderName(el != null ? el.getName() : "未知长者")
                    .elderNo(el != null ? el.getElderNo() : "-")
                    .bedInfo(elderBedMap.getOrDefault(o.getElderId(), "-"))
                    .orderDate(o.getOrderDate())
                    .mealType(o.getMealType())
                    .mealTypeName(getMealTypeName(o.getMealType()))
                    .totalAmount(o.getTotalAmount())
                    .status(o.getStatus())
                    .statusName(getStatusName(o.getStatus()))
                    .orderedBy(o.getOrderedBy())
                    .orderedByName(user != null ? user.getRealName() : "系统自动")
                    .createdAt(o.getCreatedAt())
                    .isPastCancelCutoff(pastCutoff)
                    .items(orderItemsMap.getOrDefault(o.getId(), Collections.emptyList()))
                    .build();
        }).collect(Collectors.toList());
    }

    private String getMealTypeName(String type) {
        if (type == null) return "-";
        return switch (type.toUpperCase()) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            default -> type;
        };
    }

    private String getStatusName(String status) {
        if (status == null) return "-";
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "已确认/待配送";
            case "DELIVERED" -> "已配送/已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
