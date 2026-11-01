package com.example.elderlycare.modules.admission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.admission.entity.Bed;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BedMapper extends BaseMapper<Bed> {

    /**
     * 悲观排他行级锁查询床位，用于高并发下原子占床、调床和退住
     *
     * @param id 床位ID
     * @return 锁定的床位记录
     */
    @Select("SELECT * FROM bed WHERE id = #{id} FOR UPDATE")
    Bed selectByIdForUpdate(@Param("id") Long id);
}
