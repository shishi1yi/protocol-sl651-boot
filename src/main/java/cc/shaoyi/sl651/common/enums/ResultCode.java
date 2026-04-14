package cc.shaoyi.sl651.common.enums;

/**
 * 响应状态码枚举
 */
public enum ResultCode {

    /**
     * 操作成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 业务异常/操作失败
     */
    FAILURE(400, "操作失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
