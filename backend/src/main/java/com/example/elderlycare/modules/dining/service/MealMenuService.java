package com.example.elderlycare.modules.dining.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.MealMenuCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealMenuQueryDTO;
import com.example.elderlycare.modules.dining.vo.MealMenuVO;

import java.time.LocalDate;

public interface MealMenuService {

    Long saveOrUpdateMenu(MealMenuCreateDTO dto);

    MealMenuVO getMenuDetail(Long id);

    MealMenuVO getMenuByDateAndType(LocalDate menuDate, String mealType);

    PageResult<MealMenuVO> pageMenus(MealMenuQueryDTO query);

    void deleteMenu(Long id);
}
