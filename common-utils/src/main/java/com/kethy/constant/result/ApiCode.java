package com.kethy.constant.result;

public enum ApiCode {

    /**
     * 操作成功
     **/
    SUCCESS(200, "操作成功"),

    /**
     * 非法访问
     **/
    UNAUTHORIZED(401, "非法访问"),

    /**
     * 没有权限
     **/
    FORBIDDEN(403, "没有权限"),

    /**
     * 你请求的资源不存在
     **/
    NOT_FOUND(4004, "你请求的资源不存在"),

    /**
     * 操作失败
     **/
    FAIL(500, "操作失败"),

    /**
     * 登录失败
     **/
    LOGIN_EXCEPTION(400, "登录失败"),

    /**
     * 系统异常
     **/
    SYSTEM_EXCEPTION(501, "系统异常"),

    /**
     * 请求参数校验异常
     **/
    PARAMETER_EXCEPTION(502, "请求参数校验异常"),

    /**
     * 请求参数解析异常
     **/
    PARAMETER_PARSE_EXCEPTION(503, "请求参数解析异常"),
    /**
     * HTTP内容类型异常
     **/
    HTTP_MEDIA_TYPE_EXCEPTION(504, "HTTP内容类型异常"),

    /**
     * 系统处理异常
     **/
    SPRING_BOOT_PLUS_EXCEPTION(510, "系统处理异常"),

    /**
     * 业务处理异常
     **/
    BUSINESS_EXCEPTION(511, "业务处理异常"),

    /**
     * 数据库处理异常
     **/
    DAO_EXCEPTION(512, "数据库处理异常"),

    /**
     * 验证码校验异常
     **/
    VERIFICATION_CODE_EXCEPTION(513, "验证码校验异常"),

    /**
     * 登录授权异常
     **/
    AUTHENTICATION_EXCEPTION(514, "登录授权异常"),

    /**
     * 没有访问权限
     **/
    UNAUTHENTICATED_EXCEPTION(515, "没有访问权限"),

    /**
     * 没有访问权限
     **/
    UNAUTHORIZED_EXCEPTION(516, "没有访问权限"),

    /**
     * JWT Token解析异常
     **/
    JWT_DECODE_EXCEPTION(517, "Token解析异常"),

    /**
     * 不支持的方法
     **/
    HTTP_REQUEST_METHOD_NOT_SUPPORTED_EXCEPTION(518, "METHOD NOT SUPPORTED"),;

    private final int code;
    private final String message;

    ApiCode(final int code, final String message) {
        this.code = code;
        this.message = message;
    }

    public static ApiCode getApiCode(int code) {
        ApiCode[] ecs = ApiCode.values();
        for (ApiCode ec : ecs) {
            if (ec.getCode() == code) {
                return ec;
            }
        }
        return SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
