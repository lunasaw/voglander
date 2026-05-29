package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.config;

import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command.AbstractVoglanderServerCommand;

/**
 * GB28181服务端设备配置指令实现类
 * <p>
 * 提供设备配置相关的指令发送功能，包括设备参数配置、配置下载（查询）、设备重启等操作。
 * 继承AbstractVoglanderServerCommand获得统一的异常处理和日志记录能力。
 * </p>
 *
 * <h3>sip-gateway 1.8.0 适配</h3>
 * <p>
 * 1.8.0 移除了 {@code deviceConfigDownloadQuery(...)}。GB28181 中
 * {@code ConfigDownload}（cmdType=ConfigDownload）本身即配置下载/查询入口，
 * 因此原先重复的 {@code queryXxxConfig} 系列方法已合并到 {@link #downloadDeviceConfig}。
 * </p>
 *
 * <h3>支持的配置操作</h3>
 * <ul>
 * <li>设备基本配置 - 设备名称、过期时间、心跳间隔等</li>
 * <li>配置下载（查询） - 下载/查询设备配置</li>
 * <li>设备重启 - 远程重启设备</li>
 * </ul>
 *
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
@Component
public class VoglanderServerConfigCommand extends AbstractVoglanderServerCommand {

    /**
     * 配置设备基本参数
     * <p>
     * 向设备发送基本参数配置指令，设置设备名称、过期时间、心跳间隔等。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称
     * @param expiration 过期时间（秒）
     * @param heartBeatInterval 心跳��隔（秒）
     * @param heartBeatCount 心跳次数
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> configDevice(String deviceId, String name, String expiration,
        String heartBeatInterval, String heartBeatCount) {
        validateDeviceId(deviceId, "配置设备时设备ID不能为空");

        return executeCommand("configDevice", deviceId,
            () -> serverCommandSender.deviceConfig(deviceId, name, expiration, heartBeatInterval, heartBeatCount),
            deviceId, name, expiration, heartBeatInterval, heartBeatCount);
    }

    /**
     * 配置设备基本参数（使用默认值）
     *
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> configDevice(String deviceId, String name) {
        validateNotNull(name, "设备名称不能为空");
        return configDevice(deviceId, name, "3600", "60", "3");
    }

    /**
     * 下载（查询）设备配置
     * <p>
     * 向设备发送配置下载指令（cmdType=ConfigDownload），下载/查询指定类型的配置。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param configType 配置类型（如：BasicParam、VideoParamOpt、AudioParamOpt等）
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当参数无效时抛出
     */
    public ResultDTO<Void> downloadDeviceConfig(String deviceId, String configType) {
        validateDeviceId(deviceId, "下载设备配置时设备ID不能为空");
        validateNotNull(configType, "配置类型不能为空");

        return executeCommand("downloadDeviceConfig", deviceId,
            () -> serverCommandSender.deviceConfigDownload(deviceId, configType),
            deviceId, configType);
    }

    /**
     * 重启设备
     * <p>
     * 向设备发送重启指令，远程重启设备。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空时抛出
     */
    public ResultDTO<Void> rebootDevice(String deviceId) {
        validateDeviceId(deviceId, "重启设备时设备ID不能为空");

        return executeCommand("rebootDevice", deviceId,
            () -> serverCommandSender.deviceControlReboot(deviceId),
            deviceId);
    }

    // ==================== 预定义配置类型的便捷方法 ====================

    /**
     * 下载基本参数配置
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> downloadBasicConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "BasicParam");
    }

    /**
     * 下载视频参数配置
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> downloadVideoConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "VideoParamOpt");
    }

    /**
     * 下载音频参数配置
     *
     * @param deviceId 设备ID
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> downloadAudioConfig(String deviceId) {
        return downloadDeviceConfig(deviceId, "AudioParamOpt");
    }

    // ==================== 批量配置方法 ====================

    /**
     * 配置设备完整参数
     * <p>
     * 使用推荐的参数配置设备的完整基本信息。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称，不能为空
     * @param customExpiration 自定义过期时间（秒），null则使用默认值3600
     * @param customHeartBeatInterval 自定义心跳间隔（秒），null则使用默认值60
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> configDeviceComplete(String deviceId, String name,
        String customExpiration, String customHeartBeatInterval) {
        String expiration = customExpiration != null ? customExpiration : "3600";
        String heartBeatInterval = customHeartBeatInterval != null ? customHeartBeatInterval : "60";

        return configDevice(deviceId, name, expiration, heartBeatInterval, "3");
    }

    /**
     * 配置高频心跳设备
     * <p>
     * 为需要高频心跳的重要设备配置参数（心跳间隔30秒）。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> configHighFrequencyDevice(String deviceId, String name) {
        return configDevice(deviceId, name, "3600", "30", "5");
    }

    /**
     * 配置低频心跳设备
     * <p>
     * 为不重要或网络条件较差的设备配置参数（心跳间隔120秒）。
     * </p>
     *
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称，不能为空
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> configLowFrequencyDevice(String deviceId, String name) {
        return configDevice(deviceId, name, "7200", "120", "2");
    }
}
