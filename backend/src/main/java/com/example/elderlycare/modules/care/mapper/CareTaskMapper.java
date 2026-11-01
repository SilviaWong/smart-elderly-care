package com.example.elderlycare.modules.care.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.care.entity.CareTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CareTaskMapper extends BaseMapper<CareTask> {

    @Select("SELECT * FROM care_task WHERE id = #{id} FOR UPDATE")
    CareTask selectByIdForUpdate(@Param("id") Long id);
}
