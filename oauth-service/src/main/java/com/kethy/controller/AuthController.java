package com.kethy.controller;

import com.kethy.constant.result.ApiResult;
import com.kethy.domain.dto.Oauth2TokenDto;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.endpoint.TokenEndpoint;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;

/**
 * 自定义Oauth2获取令牌接口
 * @author andylau 2023/5/10
 */
@RestController
@RequestMapping("/oauth")
public class AuthController {

    @Resource
    private TokenEndpoint tokenEndpoint;

    /**
     * Oauth2登录认证
     */
    @PostMapping("/token")
    public ApiResult<Oauth2TokenDto> postAccessToken(Principal principal, @RequestParam Map<String, String> parameters) throws HttpRequestMethodNotSupportedException {
        OAuth2AccessToken oAuth2AccessToken = tokenEndpoint.postAccessToken(principal, parameters).getBody();
        Oauth2TokenDto oauth2TokenDto = Oauth2TokenDto.builder()
                .token(Objects.requireNonNull(oAuth2AccessToken).getValue())
                .refreshToken(oAuth2AccessToken.getRefreshToken().getValue())
                .expiresIn(oAuth2AccessToken.getExpiresIn())
                .tokenHead("Bearer ").build();
        return ApiResult.ok(oauth2TokenDto);
    }

}
