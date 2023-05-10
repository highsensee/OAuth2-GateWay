package com.kethy.domain.entity;


import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@AllArgsConstructor
@Builder(toBuilder = true)
@TableName("role_resource")
public class RoleResource {

    @TableId
    private Long id;

    private String resourcePath;

    private String resourceRoles;

}
