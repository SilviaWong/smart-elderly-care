package com.example.elderlycare.modules.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.iot.entity.DeviceBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceBindingMapper extends BaseMapper<DeviceBinding> {

    @Select("SELECT * FROM device_binding WHERE id = #{id} FOR UPDATE")
    DeviceBinding selectByIdForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM device_binding WHERE device_id = #{deviceId} AND status = 'BOUND' FOR UPDATE")
    DeviceBinding selectActiveByDeviceIdForUpdate(@Param("deviceId") Long deviceId);
}
