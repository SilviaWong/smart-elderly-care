package com.example.elderlycare.modules.regulatory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.regulatory.entity.RegulatoryBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RegulatoryBatchMapper extends BaseMapper<RegulatoryBatch> {

    @Select("SELECT * FROM regulatory_batch WHERE id = #{id} FOR UPDATE")
    RegulatoryBatch selectByIdForUpdate(@Param("id") Long id);
}
