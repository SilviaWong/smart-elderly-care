package com.example.elderlycare.modules.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.risk.entity.RiskEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RiskEventMapper extends BaseMapper<RiskEvent> {

    @Select("SELECT * FROM risk_event WHERE id = #{id} FOR UPDATE")
    RiskEvent selectByIdForUpdate(@Param("id") Long id);
}
