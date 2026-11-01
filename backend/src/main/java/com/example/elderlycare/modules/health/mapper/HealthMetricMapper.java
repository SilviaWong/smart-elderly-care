package com.example.elderlycare.modules.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.health.entity.HealthMetric;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HealthMetricMapper extends BaseMapper<HealthMetric> {
}
