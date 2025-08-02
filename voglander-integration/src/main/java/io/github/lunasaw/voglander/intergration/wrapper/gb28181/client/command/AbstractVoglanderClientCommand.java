package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command;

import org.springframework.beans.factory.annotation.Autowired;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;

import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.sip.common.service.ClientDeviceSupplier;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181客户端指令抽象基类
 * <p>
 * 提供统一的设备获取、异常处理和日志记录功能，
 * 所有具体的指令实现类都应该继承此抽象类。
 * </p>
 * 
 * <h3>功能特性</h3>
 * <ul>
 * <li>统一的设备信息获取</li>
 * <li>标准化的异常处理和错误返回</li>
 * <li>一致的日志记录格式</li>
 * <li>统一的ResultDTO返回格式</li>
 * </ul>
 * 
 * <h3>使用方式</h3>
 * 
 * <pre>
 * {@code @Component
 * public class VoglanderClientAlarmCommand extends AbstractVoglanderClientCommand {
 *     public ResultDTO<Void> sendAlarmCommand(String deviceId, DeviceAlarm deviceAlarm) {
 *         return executeCommand("sendAlarmCommand", deviceId,
 *             () -> ClientCommandSender.sendAlarmCommand(getClientFromDevice(), getToDevice(deviceId), deviceAlarm),
 *             deviceAlarm);
 *     }
 * }
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Slf4j
public abstract class AbstractVoglanderClientCommand {

    @Autowired
    public ClientDeviceSupplier clientDeviceSupplier;

    /**
     * 获取客户端发送方设备信息
     * 
     * @return FromDevice 发送方设备
     */
    protected FromDevice getClientFromDevice() {
        return clientDeviceSupplier.getClientFromDevice();
    }

    /**
     * 根据设备ID获取目标设备信息
     * 
     * @param deviceId 设备ID
     * @return ToDevice 目标设备
     */
    protected ToDevice getToDevice(String deviceId) {
        return clientDeviceSupplier.getToDevice(deviceId);
    }

    /**
     * 执行指令的通用模板方法
     * <p>
     * 提供统一的异常处理、日志记录和返回格式，
     * 子类只需要专注于具体的业务逻辑实现。
     * </p>
     * 
     * @param methodName 方法名称，用于日志记录
     * @param deviceId 设备ID
     * @param command 具体的指令执行逻辑
     * @param params 指令参数，用于日志记录
     * @return ResultDTO<Void> 统一的返回格式
     */
    protected ResultDTO<Void> executeCommand(String methodName, String deviceId, CommandExecutor command, Object... params) {
        try {
            log.info("{}::开始执行指令, deviceId = {}, params = {}", methodName, deviceId, params);

            String callId = command.execute();

            log.info("{}::指令执行成功, deviceId = {}, callId = {}", methodName, deviceId, callId);
            return ResultDTOUtils.success();

        } catch (Exception e) {
            log.error("{}::指令执行失败, deviceId = {}, params = {}", methodName, deviceId, params, e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 执行带返回值的指令的通用模板方法
     * 
     * @param <T> 返回值类型
     * @param methodName 方法名称，用于日志记录
     * @param deviceId 设备ID
     * @param command 具体的指令执行逻辑
     * @param params 指令参数，用于日志记录
     * @return ResultDTO<T> 带返回值的统一格式
     */
    protected <T> ResultDTO<T> executeCommandWithResult(String methodName, String deviceId, CommandExecutorWithResult<T> command, Object... params) {
        try {
            log.debug("{}::开始执行指令, deviceId = {}, params = {}", methodName, deviceId, params);

            T result = command.execute();

            log.info("{}::指令执行成功, deviceId = {}, result = {}", methodName, deviceId, result);
            return ResultDTOUtils.success(result);

        } catch (Exception e) {
            log.error("{}::指令执行失败, deviceId = {}, params = {}", methodName, deviceId, params, e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage(), null);
        }
    }

    /**
     * 执行不需要设备ID的指令的通用模板方法
     * 
     * @param methodName 方法名称，用于日志记录
     * @param command 具体的指令执行逻辑
     * @param params 指令参数，用于日志记录
     * @return ResultDTO<Void> 统一的返回格式
     */
    protected ResultDTO<Void> executeCommand(String methodName, CommandExecutor command, Object... params) {
        try {
            log.debug("{}::开始执行指令, params = {}", methodName, params);

            String callId = command.execute();

            log.info("{}::指令执行成功, callId = {}", methodName, callId);
            return ResultDTOUtils.success();

        } catch (Exception e) {
            log.error("{}::指令执行失败, params = {}", methodName, params, e);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, e.getMessage());
        }
    }

    /**
     * 指令执行器函数式接口
     * <p>
     * 用于封装具体的指令执行逻辑，支持Lambda表达式
     * </p>
     */
    @FunctionalInterface
    protected interface CommandExecutor {
        /**
         * 执行指令
         * 
         * @return callId 调用ID
         * @throws Exception 执行异常
         */
        String execute() throws Exception;
    }

    /**
     * 带返回值的指令执行器函数式接口
     * 
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    protected interface CommandExecutorWithResult<T> {
        /**
         * 执行指令并返回结果
         * 
         * @return T 执行结果
         * @throws Exception 执行异常
         */
        T execute() throws Exception;
    }

    /**
     * 参数校验工具方法
     * 
     * @param deviceId 设备ID
     * @param message 错误消息
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    protected void validateDeviceId(String deviceId, String message) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException(message != null ? message : "设备ID不能为空");
        }
    }

    /**
     * 参数校验工具方法
     * 
     * @param param 参数值
     * @param message 错误消息
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    protected void validateNotNull(Object param, String message) {
        if (param == null) {
            throw new IllegalArgumentException(message != null ? message : "参数不能为空");
        }
    }
}