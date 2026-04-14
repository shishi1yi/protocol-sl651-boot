package cc.shaoyi.sl651.modules.protocol.entity;

import cc.shaoyi.sl651.common.enums.ResultCode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一 API 响应结果封装
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    private int code;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应信息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    private R(int code, boolean success, String msg, T data) {
        this.code = code;
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 成功并返回数据
     *
     * @param data 数据实体
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> R<T> data(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), true, ResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 失败返回（仅包含错误信息）
     *
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(ResultCode.FAILURE.getCode(), false, msg, null);
    }

    /**
     * 失败返回（包含状态枚举和错误信息）
     *
     * @param resultCode 状态码枚举
     * @param msg        错误信息
     * @param <T>        数据类型
     * @return 失败结果
     */
    public static <T> R<T> fail(ResultCode resultCode, String msg) {
        return new R<>(resultCode.getCode(), false, msg, null);
    }
}
