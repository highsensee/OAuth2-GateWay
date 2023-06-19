# Gateway-Oauth2

## 架构
通过认证服务(`auth-service`)进行统一认证，然后通过网关（`oauth2-gateway`）来统一校验认证和鉴权。采用Nacos作为注册中心，Gateway作为网关，使用nimbus-jose-jwtJWT库操作JWT令牌。

- common-utils：公共组件服务，包含各种静态工具类、通用常量
- auth-service：Oauth2认证服务，负责对登录用户进行认证，整合Spring Security Oauth2
- gateway-service：网关服务，负责请求转发和鉴权功能，整合Spring Security Oauth2
- system-service：受保护的API服务，用户鉴权通过后可以访问该服务，不整合Spring Security Oauth2
## 具体实现

基础组件服务这里不在赘述。

### 一、认证服务

**auth-service** 

> 1、首先来搭建认证服务，它将作为Oauth2的认证服务使用，并且网关服务的鉴权功能也需要依赖它，在pom.xml中添加相关依赖，主要是Spring Security、Oauth2、JWT、Redis相关依赖

```java
<dependencies>
        <!-- common-utils -->
        <dependency>
            <groupId>com.kethy</groupId>
            <artifactId>common-utils</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-oauth2</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>8.16</version>
        </dependency>
        <!-- redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
    </dependencies>
```

> 2、在application.yml中添加相关配置，主要是Nacos和Redis以及Mybatis相关配置

```yml
server:
  port: 8800

---
spring:
  profiles:
    active: dev
  application:
    name: auth-service
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.200.128:8848
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
  redis:
    database: 0
    port: 6379
    host: 192.168.200.128
    password:

# Mysql
---
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.200.128:3306/micro?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&allowMultiQueries=true&useSSL=true&serverTimezone=GMT&useSSL=false
    username: root
    password: andylau
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      idle-timeout: 10000
      max-lifetime: 1800000
      connection-timeout: 40000

# Mybatis
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    auto-mapping-behavior: full
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      logic-not-delete-value: 0
      logic-delete-value: 1
    banner: false

# api管理
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

> 3、使用keytool生成RSA证书jwt.jks，复制到resource目录下，在JDK的bin目录下使用如下命令即可（口令需要与载入证书时输入的口令保持一致）

```shell
keytool -genkey -alias jwt -keyalg RSA -keystore jwt.jks
```

> 4、创建UserServiceImpl类实现Spring Security的UserDetailsService接口，用于加载用户信息

```Java
package com.kethy.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kethy.constant.MessageConstant;
import com.kethy.domain.entity.User;
import com.kethy.mapper.UserMapper;
import com.kethy.principal.UserPrincipal;
import com.kethy.service.UserService;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = this.lambdaQuery().eq(User::getUsername, username).one();
        if (user == null) {
            throw new UsernameNotFoundException(MessageConstant.USERNAME_PASSWORD_ERROR);
        }
        UserPrincipal userPrincipal = new UserPrincipal(user);
        if (!userPrincipal.isEnabled()) {
            throw new DisabledException(MessageConstant.ACCOUNT_DISABLED);
        } else if (!userPrincipal.isAccountNonLocked()) {
            throw new LockedException(MessageConstant.ACCOUNT_LOCKED);
        } else if (!userPrincipal.isAccountNonExpired()) {
            throw new AccountExpiredException(MessageConstant.ACCOUNT_EXPIRED);
        } else if (!userPrincipal.isCredentialsNonExpired()) {
            throw new CredentialsExpiredException(MessageConstant.CREDENTIALS_EXPIRED);
        }
        return userPrincipal;
    }
}

```

> 5、创建ClientServiceImpl类实现Spring Security的ClientDetailsService接口，用于加载客户端信息

```java
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

```

> 6、添加认证服务相关配置Oauth2ServerConfig，需要配置加载用户信息的服务UserServiceImpl和加载客户端信息的服务ClientServiceImpl及RSA的钥匙对KeyPair

```java
package com.kethy.config;

import com.kethy.component.JwtTokenEnhancer;
import com.kethy.service.ClientService;
import com.kethy.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;

import javax.annotation.Resource;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务器配置
 * @author andylau 2023/5/10
 */
@AllArgsConstructor
@Configuration
@EnableAuthorizationServer
public class OauthServerConfig extends AuthorizationServerConfigurerAdapter {

    @Resource
    private UserService userService;

    @Resource
    private ClientService clientService;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtTokenEnhancer jwtTokenEnhancer;

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients.withClientDetails(clientService);
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        TokenEnhancerChain enhancerChain = new TokenEnhancerChain();
        List<TokenEnhancer> delegates = new ArrayList<>();
        delegates.add(jwtTokenEnhancer);
        delegates.add(accessTokenConverter());
        enhancerChain.setTokenEnhancers(delegates); //配置JWT的内容增强器
        endpoints.authenticationManager(authenticationManager)
                .userDetailsService(userService) //配置加载用户信息的服务
                .accessTokenConverter(accessTokenConverter())
                .tokenEnhancer(enhancerChain);
    }

    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) {
        security.allowFormAuthenticationForClients();
    }

    @Bean
    public JwtAccessTokenConverter accessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        jwtAccessTokenConverter.setKeyPair(keyPair());
        return jwtAccessTokenConverter;
    }

    @Bean
    public KeyPair keyPair() {
        // 从classpath下的证书中获取秘钥对
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource("jwt.jks"), "654321".toCharArray());
        return keyStoreKeyFactory.getKeyPair("jwt", "654321".toCharArray());
    }

}
```

> 7、如果你想往JWT中添加自定义信息的话，比如说登录用户的ID，可以自己实现TokenEnhancer接口

```java
package com.kethy.component;

import com.kethy.principal.UserPrincipal;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;


/**
 * JWT内容增强器
 * @author andylau 2023/5/10
 */
@Component
public class JwtTokenEnhancer implements TokenEnhancer {
    @Override
    public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Map<String, Object> info = new HashMap<>();
        // 把用户ID设置到JWT中
        info.put("id", userPrincipal.getId());
        ((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(info);
        return accessToken;
    }
}

```

> 8、由于我们的网关服务需要RSA的公钥来验证签名是否合法，所以认证服务需要有个接口把公钥暴露出来

```java
package com.kethy.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

/**
 * 获取RSA公钥接口
 * @author andylau 2023/5/10
 */
@RestController
public class KeyPairController {

    @Resource
    private KeyPair keyPair;

    @GetMapping("/rsa/publicKey")
    public Map<String, Object> getKey() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAKey key = new RSAKey.Builder(publicKey).build();
        return new JWKSet(key).toJSONObject();
    }

}

```

> 9、还需要配置Spring Security，允许获取公钥接口的访问

```java
package com.kethy.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SpringSecurity配置
 * @author andylau 2023/5/10
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                .antMatchers("/rsa/publicKey").permitAll()
                .anyRequest().authenticated()
                .and().formLogin().permitAll();
    }

    @Bean("authenticationManager")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

```

### 二、网关服务

**gateway-service**

接下来搭建网关服务，它将作为Oauth2的资源服务、客户端服务使用，对访问微服务的请求进行统一的校验认证和鉴权操作

> 1、在pom.xml中添加相关依赖，主要是Gateway、Oauth2和JWT、MyBatis相关依赖

```java
<dependencies>
        <dependency>
            <groupId>com.kethy</groupId>
            <artifactId>common-utils</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-config</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-client</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-jose</artifactId>
        </dependency>
        <dependency>
            <groupId>com.nimbusds</groupId>
            <artifactId>nimbus-jose-jwt</artifactId>
            <version>8.16</version>
        </dependency>
        <!-- redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <!-- 自定义的元数据依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
```

> 2、在application.yml中添加相关配置，主要是路由规则的配置、Oauth2中RSA公钥的配置及路由白名单的配置

```yml
server:
  port: 8801

spring:
  profiles:
    active: dev
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.200.128:8848
    gateway:
      routes: # 配置路由路径
        - id: system-service-route
          uri: lb://system-service
          predicates:
            - Path=/api/**
          filters:
            - StripPrefix=1
        - id: auth-service-route
          uri: lb://auth-service
          predicates:
            - Path=/auth/**
          filters:
            - StripPrefix=1
        - id: oauth2-auth-login
          uri: lb://auth-service
          predicates:
            - Path=/login
          filters:
            - PreserveHostHeader
        - id: oauth2-auth-token
          uri: lb://auth-service
          predicates:
            - Path=/oauth/token
          filters:
            - PreserveHostHeader
        - id: oauth2-auth-authorize
          uri: lb://auth-service
          predicates:
            - Path=/oauth/authorize
          filters:
            - PreserveHostHeader
      discovery:
        locator:
          enabled: true # 开启从注册中心动态创建路由的功能
          lower-case-service-id: true # 使用小写服务名，默认是大写

# Security 配置
---
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: 'http://localhost:8800/rsa/publicKey' # 配置RSA的公钥访问地址

# Redis 配置
---
spring:
  redis:
    database: 0
    port: 6379
    host: 192.168.200.128
    password:
# 配置白名单路径
secure:
  ignore:
    urls:
      - "/actuator/**"
      - "/oauth/token"
      - "/oauth/authorize"
      - "/login"


# Mysql
---
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.200.128:3306/micro?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&allowMultiQueries=true&useSSL=true&serverTimezone=GMT&useSSL=false
    username: root
    password: andylau
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      idle-timeout: 10000
      max-lifetime: 1800000
      connection-timeout: 40000

# Mybatis
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    auto-mapping-behavior: full
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      logic-not-delete-value: 0
      logic-delete-value: 1
    banner: false
```

> 3、对网关服务进行配置安全配置，由于Gateway使用的是WebFlux，所以需要使用@EnableWebFluxSecurity注解开启

```java
package com.kethy.config;

import cn.hutool.core.util.ArrayUtil;
import com.kethy.authorization.AuthorizationManager;
import com.kethy.component.RestAuthenticationEntryPoint;
import com.kethy.component.RestfulAccessDeniedHandler;
import com.kethy.constant.AuthConstant;
import com.kethy.filter.IgnoreUrlsRemoveJwtFilter;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * 资源服务器配置
 * @author 2023/5/10
 */
@AllArgsConstructor
@Configuration
@EnableWebFluxSecurity
public class ResourceServerConfig {

    @Resource
    private AuthorizationManager authorizationManager;

    @Resource
    private IgnoreUrlsConfig ignoreUrlsConfig;

    @Resource
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;

    @Resource
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Resource
    private IgnoreUrlsRemoveJwtFilter ignoreUrlsRemoveJwtFilter;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.oauth2ResourceServer().jwt().jwtAuthenticationConverter(jwtAuthenticationConverter());
        // 1、自定义处理JWT请求头过期或签名错误的结果
        http.oauth2ResourceServer().authenticationEntryPoint(restAuthenticationEntryPoint);
        // 2、对白名单路径，直接移除JWT请求头
        http.addFilterBefore(ignoreUrlsRemoveJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);
        http.authorizeExchange()
                .pathMatchers(ArrayUtil.toArray(ignoreUrlsConfig.getUrls(), String.class)).permitAll() // 白名单配置
                .anyExchange().access(authorizationManager) // 鉴权管理器配置
                .and().exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler) // 处理未授权
                .authenticationEntryPoint(restAuthenticationEntryPoint) // 处理未认证
                .and().csrf().disable();
        return http.build();
    }

    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix(AuthConstant.AUTHORITY_PREFIX);
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName(AuthConstant.AUTHORITY_CLAIM_NAME);
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
    }

}

```
> 4、在WebFluxSecurity中自定义鉴权操作需要实现ReactiveAuthorizationManager接口

```java
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

```

> 5、这里我们还需要实现一个全局过滤器AuthGlobalFilter，当鉴权通过后将JWT令牌中的用户信息解析出来，然后存入请求的Header中，这样后续服务就不需要解析JWT令牌了，可以直接从请求的Header中获取到用户信息

```java
package com.kethy.filter;

import cn.hutool.core.util.StrUtil;
import com.nimbusds.jose.JWSObject;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.text.ParseException;

/**
 * 将登录用户的JWT转化成用户信息的全局过滤器
 * @author 2023/5/10
 */
@Component
@Slf4j
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (StrUtil.isEmpty(token)) {
            return chain.filter(exchange);
        }

        try {
            // 从token中解析用户信息并设置到Header中去
            String realToken = token.replace("Bearer ", "");
            JWSObject jwsObject = JWSObject.parse(realToken);
            String userStr = jwsObject.getPayload().toString();
            log.info("AuthGlobalFilter.filter() user:{}", userStr);
            ServerHttpRequest request = exchange.getRequest().mutate().header("user", userStr).build();
            exchange = exchange.mutate().request(request).build();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}

```

### 三、系统服务

**system-service** 

最后我们搭建一个API服务，它不会集成和实现任何安全相关逻辑，全靠网关来保护它

> 1、在pom.xml中添加相关依赖，就添加了一个web依赖

```java
<dependencies>
   <dependency>
            <groupId>com.kethy</groupId>
            <artifactId>common-utils</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
</dependencies>
```

> 2、在application.yml添加相关配置，主要是Mybatis等相关配置

```yml
server:
  port: 8802

---
spring:
  profiles:
    active: dev
  application:
    name: system-service
  cloud:
    nacos:
      discovery:
        server-addr: 192.168.200.128:8848
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss

# Redis
---
spring:
  redis:
    database: 0
    port: 6379
    host: 192.168.200.128
    password:

# Mysql
---
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://192.168.200.128:3306/micro?useUnicode=true&characterEncoding=utf-8&autoReconnect=true&allowMultiQueries=true&useSSL=true&serverTimezone=GMT&useSSL=false
    username: root
    password: andylau
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      idle-timeout: 10000
      max-lifetime: 1800000
      connection-timeout: 40000

# Mybatis
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    auto-mapping-behavior: full
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  mapper-locations: classpath:mapper/*.xml
  global-config:
    db-config:
      logic-not-delete-value: 0
      logic-delete-value: 1
    banner: false

# api管理
management:
  endpoints:
    web:
      exposure:
        include: "*"

```

> 3、创建一个测试接口，网关验证通过即可访问

```java
package com.kethy.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 2023/5/10
 */
@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World !";
    }

}

```

> 4、创建一个获取登录中的用户信息的接口，用于从请求的Header中直接获取登录用户信息

```java
package com.kethy.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import com.kethy.domain.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取登录用户信息接口
 * @author 2023/5/10
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/currentUser")
    public User currentUser(HttpServletRequest request) {
        // 从Header中获取用户信息
        String userStr = request.getHeader("user");
        JSONObject userJsonObject = new JSONObject(userStr);
        return User.builder()
                .username(userJsonObject.getStr("user_name"))
                .id(Convert.toLong(userJsonObject.get("id")))
                // .roles(Convert.toList(String.class, userJsonObject.get("authorities")))
                .build();
    }

    @GetMapping
    public JSONObject findUser(HttpServletRequest request) {
        // 从Header中获取用户信息
        String userStr = request.getHeader("user");
        return new JSONObject(userStr);
    }
}

```

## 功能演示

在此之前先启动我们的 Nacos 和 Redis 服务，然后依次启动`oauth2-auth`、`oauth2-gateway`及`oauth2-api`

> 1、使用密码模式获取JWT令牌，访问地址：http://localhost:8801/oauth/token

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/1.png) 

> 2、使用获取到的JWT令牌访问获取当前登录用户信息的接口

访问地址：http://localhost:8801/api/user/currentUser 

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/2.png) 

> 4、当token不存在时，访问地址：http://localhost:8801/api/user/currentUser 

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/3.png) 

> 5、当JWT令牌过期时，使用refresh_token获取新的JWT令牌

访问地址：http://localhost:8801/oauth/token 

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/4.png) 

> 6、使用授码模式登录

先访问地址获取授权码：http://localhost:8801/oauth/authorize?response_type=code&client_id=client-app-2&redirect_uri=https://www.baidu.com

> 7、访问地址，跳转登录页面

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/5.png) 

> 8、登录成功，进入授权页面

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/6.png) 

> 9、通过授权，拿到授权码

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/7.png) 

> 10、拿到授权码，访问地址登录：http://localhost:8801/oauth/token

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/8.png) 

> 11、使用没有访问权限的`user`账号登录，访问接口时会返回如下信息，

访问地址：http://localhost:8801/api/hello 

![2.png](https://github.com/highsensee/picture-repo/blob/master/OAuth2-GateWay/9.png) 

## 项目源码地址

https://github.com/highsensee/OAuth2-GateWay.git