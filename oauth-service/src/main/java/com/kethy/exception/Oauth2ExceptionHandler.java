package com.kethy.exception;

import com.kethy.constant.result.ApiResult;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 全局处理Oauth2抛出的异常
 *
 * @author andylau 2023/5/10
 */
@ControllerAdvice
public class Oauth2ExceptionHandler {
    @ResponseBody
    @ExceptionHandler(value = OAuth2Exception.class)
    public ApiResult<String> handleOauth2(OAuth2Exception e) {
        return ApiResult.fail(e.getMessage());
    }
}
