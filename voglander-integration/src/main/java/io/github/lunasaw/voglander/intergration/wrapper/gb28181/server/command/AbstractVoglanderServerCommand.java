package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command;

import org.springframework.beans.factory.annotation.Autowired;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;

import io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender;
import lombok.extern.slf4j.Slf4j;

/**
 * GB28181服务端指令抽象基类
 * <p>
 * 提供统一的异常处理和日志记录功能，所有具体的服务端指令实现类都应该继承此抽象类。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 适配</h3>
 * <p>
 * 1.8.0 起 {@link ServerCommandSender} 由静态工具类改为实例 Bean，调用方只需传 {@code deviceId}，
 * 内部通过 {@code DeviceSessionCache} 查找设备寻址信息。子类直接注入并调用 {@link #serverCommandSender}
 * 的实例方法，不再需要手动构造 FromDevice/ToDevice。
 * </p>
 *
 * <h3>功能特性</h3>
 * <ul>
 * <li>统一的指令发送器（实例 Bean）注入</li>
 * <li>标准化的异常处理和错误返回</li>
 * <li>一致的日志记录格式</li>
 * <li>统一的ResultDTO返回格式</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 *
 * <pre>
 * {@code @Component
 * public class VoglanderServerDeviceCommand extends AbstractVoglanderServerCommand {
 *     public ResultDTO<Void> queryDeviceInfo(String deviceId) {
 *         return executeCommand("queryDeviceInfo", deviceId,
 *             () -> serverCommandSender.deviceInfoQuery(deviceId), deviceId);
 *     }
 * }
 * }
 * </pre>
 *
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Slf4j
public abstract class AbstractVoglanderServerCommand {

    /**
     * 平台服务端出向命令发送器（1.8.0 实例 Bean），按 deviceId 调用。
     */
    @Autowired
    public ServerCommandSender serverCommandSender;

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
            log.debug("{}::开始执行指令, deviceId = {}, params = {}", methodName, deviceId, params);

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