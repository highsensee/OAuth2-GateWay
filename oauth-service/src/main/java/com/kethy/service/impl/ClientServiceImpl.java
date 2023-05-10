package com.kethy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kethy.constant.MessageConstant;
import com.kethy.domain.entity.Client;
import com.kethy.mapper.ClientMapper;
import com.kethy.principal.ClientPrincipal;
import com.kethy.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service("clientService")
public class ClientServiceImpl extends ServiceImpl<ClientMapper, Client> implements ClientService {

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        Client client = this.lambdaQuery().eq(Client::getClientId, clientId).one();
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, MessageConstant.NOT_FOUND_CLIENT);
        }
        return new ClientPrincipal(client);
    }
}
