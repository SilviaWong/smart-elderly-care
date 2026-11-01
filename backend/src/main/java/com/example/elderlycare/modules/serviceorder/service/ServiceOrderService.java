package com.example.elderlycare.modules.serviceorder.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderActionDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderAssignDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderCreateDTO;
import com.example.elderlycare.modules.serviceorder.dto.ServiceOrderQueryDTO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderLogVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderSummaryVO;
import com.example.elderlycare.modules.serviceorder.vo.ServiceOrderVO;

import java.util.List;

public interface ServiceOrderService {

    PageResult<ServiceOrderVO> pageOrders(ServiceOrderQueryDTO query);

    ServiceOrderVO getOrderDetail(Long id);

    Long createOrder(ServiceOrderCreateDTO dto);

    void updateOrder(Long id, ServiceOrderCreateDTO dto);

    void assignOrder(Long id, ServiceOrderAssignDTO dto);

    void executeAction(Long id, ServiceOrderActionDTO dto);

    List<ServiceOrderLogVO> listLogs(Long orderId);

    ServiceOrderSummaryVO getOrderSummary();
}
