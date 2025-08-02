/**
 * GB28181服务端指令包
 * <p>
 * 该包提供了GB28181协议服务端指令的统一封装和管理，包含了所有常用的设备查询、控制、配置等指令。
 * </p>
 * 
 * <h3>主要功能模块</h3>
 * <ul>
 * <li>设备查询指令 - 设备信息、状态、目录、预设位等查询</li>
 * <li>设备控制指令 - 云台控制、录像控制、重启等控制操作</li>
 * <li>设备配置指令 - 设备参数配置、配置下载等</li>
 * <li>录像查询指令 - 录像信息查询、录像文件管理</li>
 * <li>告警查询指令 - 设备告警信息查询和管理</li>
 * <li>会话控制指令 - SIP会话的ACK、BYE等控制</li>
 * <li>媒体流指令 - 实时流和回放流的点播控制</li>
 * </ul>
 * 
 * <h3>设计原则</h3>
 * <ul>
 * <li>统一的抽象基类 {@link AbstractVoglanderServerCommand} 提供通用功能</li>
 * <li>按功能模块划分子包，便于管理和维护</li>
 * <li>统一的ResultDTO返回格式，便于上层调用</li>
 * <li>完善的异常处理和日志记录</li>
 * <li>参数校验和类型安全</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {
 *     &#64;code
 *     // 注入相应的指令组件
 *     &#64;Autowired
 *     private VoglanderServerDeviceCommand deviceCommand;
 * 
 *     @Autowired
 *     private VoglanderServerPtzCommand ptzCommand;
 * 
 *     // 执行设备信息查询
 *     ResultDTO<Void> result1 = deviceCommand.queryDeviceInfo("34020000001320000001");
 * 
 *     // 执行云台控制
 *     ResultDTO<Void> result2 = ptzCommand.controlDevicePtz("34020000001320000001", "A50F01010600FF");
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/2
 * @version 1.0
 */
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command;