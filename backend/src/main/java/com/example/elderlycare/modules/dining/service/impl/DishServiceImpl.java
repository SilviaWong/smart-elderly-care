package com.example.elderlycare.modules.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.dining.dto.DishDTO;
import com.example.elderlycare.modules.dining.dto.DishQueryDTO;
import com.example.elderlycare.modules.dining.entity.Dish;
import com.example.elderlycare.modules.dining.mapper.DishMapper;
import com.example.elderlycare.modules.dining.service.DishService;
import com.example.elderlycare.modules.dining.vo.DishVO;
import com.example.elderlycare.modules.system.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishServiceImpl implements DishService {

    private final DishMapper dishMapper;
    private final SysOperationLogService sysOperationLogService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDish(DishDTO dto) {
        Long orgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : 1L;

        // 唯一编码校验
        Long count = dishMapper.selectCount(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getOrgId, orgId)
                        .eq(Dish::getDishCode, dto.getDishCode().trim())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品编码已存在: " + dto.getDishCode());
        }

        Dish dish = Dish.builder()
                .orgId(orgId)
                .dishCode(dto.getDishCode().trim().toUpperCase())
                .dishName(dto.getDishName().trim())
                .category(dto.getCategory() != null ? dto.getCategory().trim().toUpperCase() : "STAPLE")
                .price(dto.getPrice())
                .allergens(dto.getAllergens())
                .description(dto.getDescription())
                .status(StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "ACTIVE")
                .build();

        dishMapper.insert(dish);

        sysOperationLogService.recordLog(
                "DINING_DISH",
                "CREATE",
                "DISH",
                dish.getId(),
                "/api/v1/dining/dishes",
                "127.0.0.1",
                String.format("创建菜品: 编码=%s, 名称=%s, 单价=%s", dish.getDishCode(), dish.getDishName(), dish.getPrice())
        );

        return dish.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDish(Long id, DishDTO dto) {
        Dish dish = getAndCheckDish(id);

        if (!dish.getDishCode().equalsIgnoreCase(dto.getDishCode().trim())) {
            Long count = dishMapper.selectCount(
                    new LambdaQueryWrapper<Dish>()
                            .eq(Dish::getOrgId, dish.getOrgId())
                            .eq(Dish::getDishCode, dto.getDishCode().trim())
                            .ne(Dish::getId, id)
            );
            if (count > 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "菜品编码已被使用: " + dto.getDishCode());
            }
        }

        dish.setDishCode(dto.getDishCode().trim().toUpperCase());
        dish.setDishName(dto.getDishName().trim());
        if (dto.getCategory() != null) {
            dish.setCategory(dto.getCategory().trim().toUpperCase());
        }
        dish.setPrice(dto.getPrice());
        dish.setAllergens(dto.getAllergens());
        dish.setDescription(dto.getDescription());
        if (StringUtils.hasText(dto.getStatus())) {
            dish.setStatus(dto.getStatus().trim().toUpperCase());
        }

        dishMapper.updateById(dish);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleDishStatus(Long id, String status) {
        Dish dish = getAndCheckDish(id);
        dish.setStatus(status.trim().toUpperCase());
        dishMapper.updateById(dish);
    }

    @Override
    public DishVO getDish(Long id) {
        Dish dish = getAndCheckDish(id);
        return convertToVO(dish);
    }

    @Override
    public PageResult<DishVO> pageDishes(DishQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(Dish::getOrgId, currentOrgId);
        }
        if (StringUtils.hasText(query.getCategory())) {
            qw.eq(Dish::getCategory, query.getCategory().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(Dish::getStatus, query.getStatus().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword().trim();
            qw.and(w -> w.like(Dish::getDishCode, kw)
                    .or().like(Dish::getDishName, kw)
                    .or().like(Dish::getDescription, kw)
                    .or().like(Dish::getAllergens, kw));
        }

        qw.orderByAsc(Dish::getCategory).orderByAsc(Dish::getDishCode);

        Page<Dish> page = dishMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<DishVO> voList = page.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<DishVO> listActiveDishes() {
        Long currentOrgId = OrgContextHolder.getOrgId();
        LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<Dish>()
                .eq(currentOrgId != null, Dish::getOrgId, currentOrgId)
                .eq(Dish::getStatus, "ACTIVE")
                .orderByAsc(Dish::getCategory)
                .orderByAsc(Dish::getDishCode);

        List<Dish> dishes = dishMapper.selectList(qw);
        return dishes.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    private Dish getAndCheckDish(Long id) {
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "菜品不存在");
        }
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(dish.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构菜品");
        }
        return dish;
    }

    private DishVO convertToVO(Dish dish) {
        return DishVO.builder()
                .id(dish.getId())
                .orgId(dish.getOrgId())
                .dishCode(dish.getDishCode())
                .dishName(dish.getDishName())
                .category(dish.getCategory())
                .categoryName(getCategoryName(dish.getCategory()))
                .price(dish.getPrice())
                .allergens(dish.getAllergens())
                .description(dish.getDescription())
                .status(dish.getStatus())
                .statusName("ACTIVE".equalsIgnoreCase(dish.getStatus()) ? "在售" : "停售")
                .build();
    }

    private String getCategoryName(String cat) {
        if (cat == null) return "其他";
        return switch (cat.toUpperCase()) {
            case "STAPLE" -> "主食碳水";
            case "MEAT" -> "荤菜优质蛋白";
            case "VEGETABLE" -> "素菜膳食纤维";
            case "SOUP" -> "滋补汤羹";
            case "FRUIT" -> "水果点心";
            case "SPECIAL" -> "特需食疗";
            default -> cat;
        };
    }
}
