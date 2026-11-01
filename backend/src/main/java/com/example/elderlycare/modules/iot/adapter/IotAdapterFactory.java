package com.example.elderlycare.modules.iot.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 物联网硬件协议适配器工厂
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IotAdapterFactory {

    private final List<IotDeviceAdapter> adapters;
    private final DefaultHttpIotAdapter defaultAdapter;

    /**
     * 根据厂商与协议标识查找适用的适配器
     *
     * @param vendor   厂商代码
     * @param protocol 协议标识
     * @return 适用的适配器实例
     */
    public IotDeviceAdapter getAdapter(String vendor, String protocol) {
        for (IotDeviceAdapter adapter : adapters) {
            if (!(adapter instanceof DefaultHttpIotAdapter) && adapter.supports(vendor, protocol)) {
                return adapter;
            }
        }
        return defaultAdapter;
    }
}
