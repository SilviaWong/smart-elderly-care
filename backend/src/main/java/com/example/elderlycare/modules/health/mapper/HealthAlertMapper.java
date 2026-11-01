package com.example.elderlycare.modules.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.health.entity.HealthAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HealthAlertMapper extends BaseMapper<HealthAlert> {

    @Select("SELECT * FROM health_alert WHERE id = #{id} FOR UPDATE")
    HealthAlert selectByIdForUpdate(@Param("id") Long id);
}
