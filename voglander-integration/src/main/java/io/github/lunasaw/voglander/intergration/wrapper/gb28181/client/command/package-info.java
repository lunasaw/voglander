/**
 * GB28181客户端指令发送实现包
 * <p>
 * 本包主要负责封装 {@link io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender} 的方法实现，
 * 为上游业务层提供简化的API接口，屏蔽底层复杂的参数构建和通信细节。
 * </p>
 * 
 * <h3>架构设计</h3>
 * <ul>
 * <li>封装层：对ClientCommandSender进行包装，简化参数传递和调用复杂度</li>
 * <li>分类管理：按功能模块将不同类型的指令分类到独立的子包中</li>
 * <li>统一接口：提供一致的调用接口和错误处理机制</li>
 * <li>参数构建：内部处理FromDevice、ToDevice等必要参数的构建</li>
 * </ul>
 * 
 * <h3>包结构规划</h3>
 * <ul>
 * <li>{@link io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm} - 告警相关指令</li>
 * <li>device - 设备信息查询和配置指令（计划中）</li>
 * <li>ptz - 云台控制指令（计划中）</li>
 * <li>record - 录像控制和查询指令（计划中）</li>
 * <li>catalog - 设备目录查询指令（计划中）</li>
 * <li>status - 设备状态查询指令（计划中）</li>
 * </ul>
 * 
 * <h3>实现示例</h3>
 * <p>
 * 告警指令实现：
 * <br>
 * 原始方法：{@link io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender#sendAlarmCommand(FromDevice, ToDevice, DeviceAlarm)}
 * <br>
 * 封装实现：{@link io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm.VoglanderClientAlarmCommand}
 * </p>
 * 
 * <h3>使用规范</h3>
 * <ul>
 * <li>所有指令实现类统一返回 {@code ResultDTO} 格式</li>
 * <li>包含完整的异常处理和日志记录</li>
 * <li>支持同步和异步两种调用模式</li>
 * <li>提供参数校验和默认值处理</li>
 * </ul>
 * 
 * <h3>扩展指南</h3>
 * <p>
 * 新增指令类型时，请按照以下步骤：
 * <ol>
 * <li>在对应功能包下创建指令实现类</li>
 * <li>继承统一的基础指令类（如有）</li>
 * <li>实现标准的方法签名和返回格式</li>
 * <li>添加完整的JavaDoc文档和使用示例</li>
 * <li>编写对应的单元测试和集成测试</li>
 * </ol>
 * </p>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command;

import io.github.lunasaw.gb28181.common.entity.DeviceAlarm;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;