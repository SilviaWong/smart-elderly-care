package com.example.elderlycare.modules.dining.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.dining.dto.MealOrderCreateDTO;
import com.example.elderlycare.modules.dining.dto.MealOrderQueryDTO;
import com.example.elderlycare.modules.dining.vo.DietaryCheckResultVO;
import com.example.elderlycare.modules.dining.vo.DiningSummaryVO;
import com.example.elderlycare.modules.dining.vo.MealOrderVO;

import java.util.List;

public interface MealOrderService {

    /**
     * 订餐前长者过敏原与饮食禁忌智能筛查
     */
    DietaryCheckResultVO checkDietaryWarning(Long elderId, List<Long> dishIds);

    /**
     * 创建长者订餐订单并保存快照
     */
    Long createOrder(MealOrderCreateDTO dto);

    /**
     * 取消订餐订单（严格校验餐次截止时间规则）
     */
    void cancelOrder(Long orderId);

    /**
     * 配送完成/核销
     */
    void deliverOrder(Long orderId);

    /**
     * 查询订单详情及菜品快照明细
     */
    MealOrderVO getOrderDetail(Long orderId);

    /**
     * 分页查询订餐订单
     */
    PageResult<MealOrderVO> pageOrders(MealOrderQueryDTO query);

    /**
     * 获取餐饮运营看板统计指标
     */
    DiningSummaryVO getSummary();
}
