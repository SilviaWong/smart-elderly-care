package com.example.elderlycare.modules.serviceorder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.elderlycare.modules.serviceorder.entity.ServiceOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ServiceOrderMapper extends BaseMapper<ServiceOrder> {

    @Select("SELECT * FROM service_order WHERE id = #{id} FOR UPDATE")
    ServiceOrder selectByIdForUpdate(@Param("id") Long id);
}
