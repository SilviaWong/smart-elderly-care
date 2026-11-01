package com.example.elderlycare.modules.iot.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceDataQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceReportDTO;
import com.example.elderlycare.modules.iot.vo.DeviceDataVO;

public interface IotTelemetryService {

    /**
     * 接收并处理设备遥测数据上报 (支持协议适配、指标存储、设备在线时间刷新、异常事件触发及健康档案桥接)
     */
    void reportTelemetry(DeviceReportDTO dto);

    /**
     * 分页查询设备遥测采集数据历史
     */
    PageResult<DeviceDataVO> pageTelemetryData(DeviceDataQueryDTO query);
}
