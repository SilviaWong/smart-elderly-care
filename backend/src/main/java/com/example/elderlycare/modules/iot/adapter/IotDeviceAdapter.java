package com.example.elderlycare.modules.iot.adapter;

import com.example.elderlycare.modules.iot.entity.Device;

/**
 * 物联网硬件协议适配器接口（解耦不同厂商私有协议与标准平台模型）
 */
public interface IotDeviceAdapter {

    /**
     * 判断当前适配器是否支持该厂商/通信协议
     *
     * @param vendor   设备厂商代码 (如 HUAWEI, TuyaSmart, OMRON, SleepAce, Hikvision)
     * @param protocol 协议标识 (如 STANDARD, HUAWEI_CLOUD, TUYA_DPS, MQTT_V1)
     * @return 是否支持
     */
    boolean supports(String vendor, String protocol);

    /**
     * 将厂商原始上报报文解析规整为统一的标准指标集合与事件
     *
     * @param device     设备台账实体
     * @param rawPayload 原始上报报文字符串 (JSON/HEX)
     * @return 标准化数据载荷
     */
    IotNormalizedPayload normalize(Device device, String rawPayload);
}
