package com.example.elderlycare.modules.dining.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.elderlycare.common.api.ErrorCode;
import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.common.context.OrgContextHolder;
import com.example.elderlycare.common.exception.BusinessException;
import com.example.elderlycare.modules.dining.dto.MealMenuCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealMenuQueryDTO;
import com.example.elderlycare.modules.dining.entity.Dish;
import com.example.elderlycare.modules.dining.entity.MealMenu;
import com.example.elderlycare.modules.dining.entity.MealMenuItem;
import com.example.elderlycare.modules.dining.mapper.DishMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuItemMapper;
import com.example.elderlycare.modules.dining.mapper.MealMenuMapper;
import com.example.elderlycare.modules.dining.service.MealMenuService;
import com.example.elderlycare.modules.dining.vo.MealMenuItemVO;
import com.example.elderlycare.modules.dining.vo.MealMenuVO;
import com.example.elderlycare.modules.system.entity.SysUser;
import com.example.elderlycare.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealMenuServiceImpl implements MealMenuService {

    private final MealMenuMapper mealMenuMapper;
    private final MealMenuItemMapper mealMenuItemMapper;
    private final DishMapper dishMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateMenu(MealMenuCreateDTO dto) {
        Long orgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : 1L;
        Long userId = OrgContextHolder.getUserId() != null ? OrgContextHolder.getUserId() : 1L;

        String mealType = dto.getMealType().trim().toUpperCase();

        // 校验菜品归属和启用状态
        List<Dish> dishes = dishMapper.selectBatchIds(dto.getDishIds());
        if (dishes.size() != dto.getDishIds().size()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部分菜品不存在，请检查后重新添加");
        }
        for (Dish d : dishes) {
            if (!orgId.equals(d.getOrgId())) {
                throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "不可选择其他机构的菜品: " + d.getDishName());
            }
        }

        // 查询是否已存在同日期同餐次的菜单
        MealMenu existingMenu = mealMenuMapper.selectOne(
                new LambdaQueryWrapper<MealMenu>()
                        .eq(MealMenu::getOrgId, orgId)
                        .eq(MealMenu::getMenuDate, dto.getMenuDate())
                        .eq(MealMenu::getMealType, mealType)
        );

        Long menuId;
        String status = StringUtils.hasText(dto.getStatus()) ? dto.getStatus().trim().toUpperCase() : "PUBLISHED";

        if (existingMenu == null) {
            MealMenu menu = MealMenu.builder()
                    .orgId(orgId)
                    .menuDate(dto.getMenuDate())
                    .mealType(mealType)
                    .status(status)
                    .createdBy(userId)
                    .createdAt(LocalDateTime.now())
                    .build();
            mealMenuMapper.insert(menu);
            menuId = menu.getId();
        } else {
            menuId = existingMenu.getId();
            existingMenu.setStatus(status);
            mealMenuMapper.updateById(existingMenu);
            // 清理旧菜单项重新生成
            mealMenuItemMapper.delete(
                    new LambdaQueryWrapper<MealMenuItem>().eq(MealMenuItem::getMenuId, menuId)
            );
        }

        // 保存菜单项并固化价格快照
        for (Dish dish : dishes) {
            MealMenuItem item = MealMenuItem.builder()
                    .menuId(menuId)
                    .dishId(dish.getId())
                    .priceSnapshot(dish.getPrice())
                    .build();
            mealMenuItemMapper.insert(item);
        }

        return menuId;
    }

    @Override
    public MealMenuVO getMenuDetail(Long id) {
        MealMenu menu = mealMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "餐次菜单不存在");
        }
        checkMenuAccess(menu);
        return convertToVO(menu, true);
    }

    @Override
    public MealMenuVO getMenuByDateAndType(LocalDate menuDate, String mealType) {
        Long currentOrgId = OrgContextHolder.getOrgId() != null ? OrgContextHolder.getOrgId() : 1L;
        MealMenu menu = mealMenuMapper.selectOne(
                new LambdaQueryWrapper<MealMenu>()
                        .eq(MealMenu::getOrgId, currentOrgId)
                        .eq(MealMenu::getMenuDate, menuDate)
                        .eq(MealMenu::getMealType, mealType.trim().toUpperCase())
        );
        if (menu == null) {
            return null;
        }
        return convertToVO(menu, true);
    }

    @Override
    public PageResult<MealMenuVO> pageMenus(MealMenuQueryDTO query) {
        Long currentOrgId = OrgContextHolder.getOrgId();

        LambdaQueryWrapper<MealMenu> qw = new LambdaQueryWrapper<>();
        if (currentOrgId != null) {
            qw.eq(MealMenu::getOrgId, currentOrgId);
        }
        if (query.getMenuDate() != null) {
            qw.eq(MealMenu::getMenuDate, query.getMenuDate());
        }
        if (StringUtils.hasText(query.getMealType())) {
            qw.eq(MealMenu::getMealType, query.getMealType().trim().toUpperCase());
        }
        if (StringUtils.hasText(query.getStatus())) {
            qw.eq(MealMenu::getStatus, query.getStatus().trim().toUpperCase());
        }

        qw.orderByDesc(MealMenu::getMenuDate).orderByAsc(MealMenu::getMealType);

        Page<MealMenu> page = mealMenuMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), qw);
        if (CollectionUtils.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }

        List<MealMenuVO> voList = page.getRecords().stream()
                .map(m -> convertToVO(m, true))
                .collect(Collectors.toList());

        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        MealMenu menu = mealMenuMapper.selectById(id);
        if (menu == null) {
            return;
        }
        checkMenuAccess(menu);
        mealMenuItemMapper.delete(new LambdaQueryWrapper<MealMenuItem>().eq(MealMenuItem::getMenuId, id));
        mealMenuMapper.deleteById(id);
    }

    private void checkMenuAccess(MealMenu menu) {
        Long currentOrgId = OrgContextHolder.getOrgId();
        if (currentOrgId != null && !currentOrgId.equals(menu.getOrgId())) {
            throw new BusinessException(ErrorCode.ORG_ACCESS_DENIED, "无权访问其他机构菜单");
        }
    }

    private MealMenuVO convertToVO(MealMenu menu, boolean loadItems) {
        SysUser creator = menu.getCreatedBy() != null ? sysUserMapper.selectById(menu.getCreatedBy()) : null;

        List<MealMenuItemVO> items = Collections.emptyList();
        if (loadItems) {
            List<MealMenuItem> menuItems = mealMenuItemMapper.selectList(
                    new LambdaQueryWrapper<MealMenuItem>().eq(MealMenuItem::getMenuId, menu.getId())
            );
            if (!CollectionUtils.isEmpty(menuItems)) {
                Set<Long> dishIds = menuItems.stream().map(MealMenuItem::getDishId).collect(Collectors.toSet());
                Map<Long, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream().collect(Collectors.toMap(Dish::getId, d -> d));

                items = menuItems.stream().map(mi -> {
                    Dish d = dishMap.get(mi.getDishId());
                    return MealMenuItemVO.builder()
                            .id(mi.getId())
                            .dishId(mi.getDishId())
                            .dishCode(d != null ? d.getDishCode() : "")
                            .dishName(d != null ? d.getDishName() : "未知菜品")
                            .category(d != null ? d.getCategory() : "")
                            .categoryName(d != null ? getCategoryName(d.getCategory()) : "")
                            .priceSnapshot(mi.getPriceSnapshot())
                            .allergens(d != null ? d.getAllergens() : null)
                            .description(d != null ? d.getDescription() : null)
                            .build();
                }).collect(Collectors.toList());
            }
        }

        return MealMenuVO.builder()
                .id(menu.getId())
                .orgId(menu.getOrgId())
                .menuDate(menu.getMenuDate())
                .mealType(menu.getMealType())
                .mealTypeName(getMealTypeName(menu.getMealType()))
                .status(menu.getStatus())
                .statusName("PUBLISHED".equalsIgnoreCase(menu.getStatus()) ? "已发布" : "草稿")
                .createdBy(menu.getCreatedBy())
                .createdByName(creator != null ? creator.getRealName() : "系统")
                .createdAt(menu.getCreatedAt())
                .dishCount(items.size())
                .items(items)
                .build();
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
