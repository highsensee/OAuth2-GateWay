package com.kethy.constant.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@SuppressWarnings("all")
public class ApiResult<T> implements Serializable {

    private static final long serialVersionUID = 8004487252556526569L;

    /**
     * 响应码
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T result;

    public ApiResult() {
    }


    public static ApiResult<Boolean> result(boolean flag) {
        if (flag) {
            return ok();
        }
        return fail();
    }

    public static ApiResult<Boolean> result(ApiCode apiCode) {
        return result(apiCode, null);
    }

    public static <T> ApiResult<T> result(ApiCode apiCode, T data) {
        return result(apiCode, null, data);
    }

    public static <T> ApiResult<T> result(Boolean ret, String message) {
        return result(ret, message);
    }

    public static <T> ApiResult<T> result(ApiCode apiCode, String message, T data) {
        boolean success = false;

        if (apiCode.getCode() == ApiCode.SUCCESS.getCode()) {
            success = true;
        }

        String apiMessage = apiCode.getMessage();
        if (StringUtils.isNotBlank(apiMessage) && StringUtils.isEmpty(message)) {
            message = apiMessage;
        }
        return (ApiResult<T>) ApiResult.builder()
                .code(apiCode.getCode())
                .msg(message)
                .result(data)
                .build();
    }

    public static ApiResult<Boolean> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> ok(T data) {
        return result(ApiCode.SUCCESS, data);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return result(ApiCode.SUCCESS, message, data);
    }

    public static ApiResult<Map<String, Object>> okMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(key, value);
        return ok(map);
    }

    public static ApiResult<Boolean> fail(ApiCode apiCode) {
        return result(apiCode, null);
    }

    public static ApiResult<String> fail(String message) {
        return result(ApiCode.FAIL, message, null);

    }

    public static <T> ApiResult<T> fail(ApiCode apiCode, T data) {
        if (ApiCode.SUCCESS == apiCode) {
            throw new RuntimeException("失败结果状态码不能为" + ApiCode.SUCCESS.getCode());
        }
        return result(apiCode, data);

    }

    public static ApiResult<String> fail(Integer errorCode, String message) {
        return new ApiResult<String>()
                .setCode(errorCode)
                .setMsg(message);
    }

    public static ApiResult<Map<String, Object>> fail(String key, Object value) {
        Map<String, Object> map = new HashMap<>(1);
        map.put(key, value);
        return result(ApiCode.FAIL, map);
    }

    public static ApiResult<Boolean> fail() {
        return fail(ApiCode.FAIL);
    }
}

