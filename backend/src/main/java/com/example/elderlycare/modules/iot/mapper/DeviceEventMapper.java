package com.example.elderlycare.modules.iot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.iot.entity.DeviceEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DeviceEventMapper extends BaseMapper<DeviceEvent> {

    @Select("SELECT * FROM device_event WHERE id = #{id} FOR UPDATE")
    DeviceEvent selectByIdForUpdate(@Param("id") Long id);
}
