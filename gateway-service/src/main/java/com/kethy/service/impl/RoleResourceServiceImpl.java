package com.kethy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kethy.domain.entity.RoleResource;
import com.kethy.mapper.RoleResourceMapper;
import com.kethy.service.RoleResourceService;
import org.springframework.stereotype.Service;

@Service("roleSourceService")
public class RoleResourceServiceImpl extends ServiceImpl<RoleResourceMapper, RoleResource> implements RoleResourceService {

}
