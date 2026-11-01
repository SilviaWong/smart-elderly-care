package com.example.elderlycare.modules.finance.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.finance.dto.FeeItemDTO;
import com.example.elderlycare.modules.finance.dto.FeeItemQueryDTO;
import com.example.elderlycare.modules.finance.vo.FeeItemVO;

import java.util.List;

public interface FeeItemService {

    Long createFeeItem(FeeItemDTO dto);

    void updateFeeItem(Long id, FeeItemDTO dto);

    void toggleFeeItemStatus(Long id, String status);

    FeeItemVO getFeeItem(Long id);

    PageResult<FeeItemVO> pageFeeItems(FeeItemQueryDTO query);

    List<FeeItemVO> listActiveFeeItems();
}
