package com.kethy.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONObject;
import com.kethy.constant.result.ApiResult;
import com.kethy.domain.dto.UserDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 获取登录用户信息接口
 * @author 2023/5/10
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/currentUser")
    public ApiResult currentUser(HttpServletRequest request) {
        // 从Header中获取用户信息
        String userStr = request.getHeader("user");
        JSONObject userObj = new JSONObject(userStr);
        List<String> authorities = userObj.getJSONArray("authorities").toList(String.class);
        return ApiResult.ok(
                UserDto.builder()
                        .id(Convert.toLong(userObj.get("id")))
                        .username(userObj.getStr("user_name"))
                        .authorities(authorities)
                        .build()
        );
    }
}
