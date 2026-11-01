package com.example.elderlycare.modules.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.iot.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeviceDataMapper extends BaseMapper<DeviceData> {
}
