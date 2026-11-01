package com.example.elderlycare.modules.iot.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceEventHandleDTO;
import com.example.elderlycare.modules.iot.dto.DeviceEventQueryDTO;
import com.example.elderlycare.modules.iot.vo.DeviceEventVO;

public interface DeviceEventService {

    /**
     * 分页查询设备事件列表
     */
    PageResult<DeviceEventVO> pageEvents(DeviceEventQueryDTO query);

    /**
     * 查询设备事件详情
     */
    DeviceEventVO getEventDetail(Long id);

    /**
     * 处理设备事件（处置中、结案或忽略，并记录处置结论与审计日志）
     */
    void handleEvent(Long id, DeviceEventHandleDTO dto);
}
