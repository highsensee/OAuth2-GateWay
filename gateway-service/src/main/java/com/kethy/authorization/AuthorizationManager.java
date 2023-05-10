package com.kethy.authorization;

import cn.hutool.core.convert.Convert;
import com.kethy.constant.AuthConstant;
import com.kethy.constant.MessageConstant;
import com.kethy.constant.RedisConstant;
import com.kethy.domain.entity.RoleResource;
import com.kethy.service.RoleResourceService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权管理器，用于判断是否有资源的访问权限
 * @author 2023/5/10
 */
@Component
public class AuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RoleResourceService roleResourceService;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> mono, AuthorizationContext authorizationContext) {
        // 从Redis中获取当前路径可访问角色列表
        URI uri = authorizationContext.getExchange().getRequest().getURI();
        Object obj = this.getRoleResource(uri.getPath());
        List<String> authorities = Convert.toList(String.class, obj);
        authorities = authorities.stream().map(i -> i = AuthConstant.AUTHORITY_PREFIX + i).collect(Collectors.toList());

        // 认证通过且角色匹配的用户可访问当前路径
        return mono
                .filter(Authentication::isAuthenticated)
                .flatMapIterable(Authentication::getAuthorities)
                .map(GrantedAuthority::getAuthority)
                .any(authorities::contains)
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));
    }

    /**
     * 初始化路径-权限列表
     */
    private Object getRoleResource(String path) {
        Object obj = redisTemplate.opsForHash().get(RedisConstant.RESOURCE_ROLES_MAP, path);
        if (obj == null) {
            RoleResource roleResource = roleResourceService.lambdaQuery().eq(RoleResource::getResourcePath, path).one();
            if (roleResource == null) {
                return null;
            }
            List<String> roles = Arrays.asList(roleResource.getResourceRoles().split(MessageConstant.SPLIT_COMMA));
            redisTemplate.opsForHash().put(RedisConstant.RESOURCE_ROLES_MAP, path, roles);
            return roles;
        }
        return null;
    }
}
