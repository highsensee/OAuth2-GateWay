package com.kethy.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author andylau 2023/5/10
 */
@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Builder(toBuilder = true)
@TableName("client")
public class Client {

    /**
     * 认证服务器ID
     */
    @TableId
    private String clientId;

    /**
     * 资源服务器ID
     */
    private String resourceIds;

    /**
     * 是否设置密钥
     */
    private Integer secretRequire;

    /**
     * 密钥串
     */
    private String clientSecret;

    /**
     * 是否设置范围
     */
    private Integer scopeRequire;

    /**
     * 资源范围
     */
    private String scope;

    /**
     * 认证服务器ID
     */
    private String authorizedGrantTypes;

    /**
     * 认证服务器ID
     */
    private String webServerRedirectUris;

    /**
     * 认证服务器ID
     */
    private String authorities;

    /**
     * 认证服务器ID
     */
    private Integer accessTokenValidity;

    /**
     * 认证服务器ID
     */
    private Integer refreshTokenValidity;
}
