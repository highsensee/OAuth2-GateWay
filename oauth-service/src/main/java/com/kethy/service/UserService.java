package com.kethy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kethy.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends IService<User>, UserDetailsService {

}
