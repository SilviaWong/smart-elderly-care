package com.example.elderlycare.modules.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.care.entity.CarePlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CarePlanMapper extends BaseMapper<CarePlan> {

    @Select("SELECT * FROM care_plan WHERE id = #{id} FOR UPDATE")
    CarePlan selectByIdForUpdate(@Param("id") Long id);
}
