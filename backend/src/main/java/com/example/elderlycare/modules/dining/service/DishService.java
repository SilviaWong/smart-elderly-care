package com.example.elderlycare.modules.dining.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.DishDTO;
import com.example.elderlycare.modules.dining.dto.DishQueryDTO;
import com.example.elderlycare.modules.dining.vo.DishVO;

import java.util.List;

public interface DishService {

    Long createDish(DishDTO dto);

    void updateDish(Long id, DishDTO dto);

    void toggleDishStatus(Long id, String status);

    DishVO getDish(Long id);

    PageResult<DishVO> pageDishes(DishQueryDTO query);

    List<DishVO> listActiveDishes();
}
