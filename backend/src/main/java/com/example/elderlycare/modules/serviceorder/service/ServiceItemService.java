package com.example.elderlycare.modules.serviceorder.service;

import com.example.elderlycare.modules.serviceorder.dto.ServiceItemDTO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceItemVO;

import java.util.List;

public interface ServiceItemService {

    List<ServiceItemVO> listItems(String category, String status);

    ServiceItemVO getItemById(Long id);

    Long createItem(ServiceItemDTO dto);

    void updateItem(Long id, ServiceItemDTO dto);

    void toggleItemStatus(Long id, String status);
}
