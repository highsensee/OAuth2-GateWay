package com.kethy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kethy.domain.entity.Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ClientMapper extends BaseMapper<Client> {

}
