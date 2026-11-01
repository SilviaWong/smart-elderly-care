package com.example.elderlycare.modules.care.service;

import com.example.elderlycare.modules.care.dto.CareItemDTO;
import com.example.elderlycare.modules.care.dto.CareLevelDTO;
import com.example.elderlycare.modules.care.vo.CareItemVO;
import com.example.elderlycare.modules.care.vo.CareLevelVO;

import java.util.List;

public interface CareConfigService {

    List<CareLevelVO> listLevels(String status);

    Long createLevel(CareLevelDTO dto);

    void updateLevel(Long id, CareLevelDTO dto);

    void toggleLevelStatus(Long id, String status);

    List<CareItemVO> listItems(String category, String status);

    Long createItem(CareItemDTO dto);

    void updateItem(Long id, CareItemDTO dto);

    void toggleItemStatus(Long id, String status);
}
