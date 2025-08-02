package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * GB28181客户端指令包测试套件
 * <p>
 * 包含所有GB28181客户端指令相关的单元测试，用于验证各个指令包的功能正确性。
 * </p>
 * 
 * <h3>测试覆盖范围</h3>
 * <ul>
 * <li>基础抽象类测试 - {@link AbstractVoglanderClientCommandTest}</li>
 * <li>告警指令测试 - {@code alarm} 包</li>
 * <li>设备信息指令测试 - {@code device} 包</li>
 * <li>云台控制指令测试 - {@code ptz} 包</li>
 * <li>录像控制指令测试 - {@code record} 包</li>
 * <li>设备目录指令测试 - {@code catalog} 包</li>
 * <li>设备状态指令测试 - {@code status} 包</li>
 * </ul>
 * 
 * <h3>运行方式</h3>
 * 
 * <pre>
 * {@code
 * // Maven运行
 * mvn test -Dtest=GB28181ClientCommandTestSuite
 * 
 * // IDE运行
 * 直接运行此测试类即可执行所有相关测试
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Suite
@SelectPackages({
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.alarm",
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.device",
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.ptz",
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.record",
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.catalog",
    "io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.status"
})
public class GB28181ClientCommandTestSuite {
    // 测试套件类，无需额外实现
}