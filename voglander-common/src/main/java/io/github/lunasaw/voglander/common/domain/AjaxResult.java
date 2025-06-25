package io.github.lunasaw.voglander.common.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Objects;

/**
 * 操作消息提醒
 * 支持泛型的返回结果封装类
 *
 * @author luna
 * @param <T> 数据类型
 */
@Getter
@Setter
public class AjaxResult<T> extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    /** 状态码 */
    public static final String CODE_TAG = "code";

    /** 返回内容 */
    public static final String MSG_TAG = "msg";

    /** 数据对象 */
    public static final String DATA_TAG = "data";

    /**
     * 初始化一个新创建的 AjaxResult 对象，使其表示一个空消息。
     */
    public AjaxResult()
    {
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     */
    public AjaxResult(int code, String msg)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * 初始化一个新创建的 AjaxResult 对象
     *
     * @param code 状态码
     * @param msg 返回内容
     * @param data 数据对象
     */
    public AjaxResult(int code, String msg, T data)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (Objects.nonNull(data))
        {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * 返回成功消息
     *
     * @return 成功消息
     */
    public static AjaxResult<Void> success()
    {
        return AjaxResult.success("操作成功");
    }

    /**
     * 返回成功数据
     *
     * @param data 数据对象
     * @return 成功消息
     * @param <T> 数据类型
     */
    public static <T> AjaxResult<T> success(T data)
    {
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @return 成功消息
     */
    public static AjaxResult<Void> success(String msg)
    {
        return AjaxResult.success(msg, null);
    }

    /**
     * 返回成功消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 成功消息
     * @param <T> 数据类型
     */
    public static <T> AjaxResult<T> success(String msg, T data)
    {
        return new AjaxResult<>(0, msg, data);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @return 警告消息
     */
    public static AjaxResult<Void> warn(String msg)
    {
        return AjaxResult.warn(msg, null);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 警告消息
     * @param <T> 数据类型
     */
    public static <T> AjaxResult<T> warn(String msg, T data)
    {
        return new AjaxResult<>(300, msg, data);
    }

    /**
     * 返回错误消息
     *
     * @return 错误消息
     */
    public static AjaxResult<Void> error()
    {
        return AjaxResult.error("操作失败");
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @return 错误消息
     */
    public static AjaxResult<Void> error(String msg)
    {
        return AjaxResult.error(msg, null);
    }

    /**
     * 返回错误消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 错误消息
     * @param <T> 数据类型
     */
    public static <T> AjaxResult<T> error(String msg, T data)
    {
        return new AjaxResult<>(500, msg, data);
    }

    /**
     * 返回错误消息
     *
     * @param code 状态码
     * @param msg 返回内容
     * @return 错误消息
     */
    public static AjaxResult<Void> error(int code, String msg) {
        return new AjaxResult<>(code, msg, null);
    }

    /**
     * 获取数据对象，带类型转换
     *
     * @return 数据对象
     */
    @SuppressWarnings("unchecked")
    public T getData() {
        return (T)super.get(DATA_TAG);
    }

    /**
     * 设置数据对象
     *
     * @param data 数据对象
     * @return 当前对象
     */
    public AjaxResult<T> setData(T data) {
        super.put(DATA_TAG, data);
        return this;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer getCode() {
        return (Integer)super.get(CODE_TAG);
    }

    /**
     * 设置状态码
     *
     * @param code 状态码
     * @return 当前对象
     */
    public AjaxResult<T> setCode(int code)
    {
        super.put(CODE_TAG, code);
        return this;
    }

    /**
     * 获取消息内容
     *
     * @return 消息内容
     */
    public String getMsg() {
        return (String)super.get(MSG_TAG);
    }

    /**
     * 设置消息内容
     *
     * @param msg 消息内容
     * @return 当前对象
     */
    public AjaxResult<T> setMsg(String msg) {
        super.put(MSG_TAG, msg);
        return this;
    }

    /**
     * 是否为成功消息
     *
     * @return 结果
     */
    public boolean isSuccess()
    {
        return Objects.equals(0, this.get(CODE_TAG));
    }

    /**
     * 是否为警告消息
     *
     * @return 结果
     */
    public boolean isWarn()
    {
        return Objects.equals(300, this.get(CODE_TAG));
    }

    /**
     * 是否为错误消息
     *
     * @return 结果
     */
    public boolean isError()
    {
        return Objects.equals(500, this.get(CODE_TAG));
    }

    /**
     * 方便链式调用
     *
     * @param key 键
     * @param value 值
     * @return 数据对象
     */
    @Override
    public AjaxResult<T> put(String key, Object value)
    {
        super.put(key, value);
        return this;
    }
}
