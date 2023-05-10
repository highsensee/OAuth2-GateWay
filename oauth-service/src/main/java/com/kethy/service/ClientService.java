package com.kethy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kethy.domain.entity.Client;
import org.springframework.security.oauth2.provider.ClientDetailsService;

public interface ClientService extends IService<Client>, ClientDetailsService {


}
