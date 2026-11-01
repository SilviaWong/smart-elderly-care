package com.example.elderlycare.modules.iot.service;

import com.example.elderlycare.common.api.PageResult;
import com.example.elderlycare.modules.iot.dto.DeviceBindDTO;
import com.example.elderlycare.modules.iot.dto.DeviceDTO;
import com.example.elderlycare.modules.iot.dto.DeviceQueryDTO;
import com.example.elderlycare.modules.iot.dto.DeviceUnbindDTO;
import com.example.elderlycare.modules.iot.vo.DeviceBindingVO;
import com.example.elderlycare.modules.iot.vo.DeviceVO;
import com.example.elderlycare.modules.iot.vo.IotSummaryVO;

import java.util.List;

public interface DeviceService {

    /**
     * 新增设备台账
     */
    Long createDevice(DeviceDTO dto);

    /**
     * 更新设备台账信息
     */
    void updateDevice(Long id, DeviceDTO dto);

    /**
     * 查询设备详情（含当前绑定长者与床位、通信状态）
     */
    DeviceVO getDeviceDetail(Long id);

    /**
     * 分页查询设备台账列表
     */
    PageResult<DeviceVO> pageDevices(DeviceQueryDTO query);

    /**
     * 获取指定类型的所有可用设备列表
     */
    List<DeviceVO> listActiveDevices(String deviceType);

    /**
     * 设备绑定 (绑定至长者个人、床位或空间，支持并发排他检查)
     */
    void bindDevice(DeviceBindDTO dto);

    /**
     * 设备解绑 (保留历史绑定追溯记录)
     */
    void unbindDevice(DeviceUnbindDTO dto);

    /**
     * 分页查询设备绑定历史记录
     */
    PageResult<DeviceBindingVO> pageBindings(Long deviceId, Long elderId, int pageNum, int pageSize);

    /**
     * 获取物联网综合看板统计指标
     */
    IotSummaryVO getIotSummary();
}
