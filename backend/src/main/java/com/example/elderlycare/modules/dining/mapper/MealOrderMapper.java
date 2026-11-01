package com.example.elderlycare.modules.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.dining.entity.MealOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MealOrderMapper extends BaseMapper<MealOrder> {

    @Select("SELECT * FROM meal_order WHERE id = #{id} FOR UPDATE")
    MealOrder selectByIdForUpdate(@Param("id") Long id);
}
