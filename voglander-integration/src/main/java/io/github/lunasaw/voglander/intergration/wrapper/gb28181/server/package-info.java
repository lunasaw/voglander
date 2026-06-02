/**
 * GB28181 服务端出向命令包装层。
 *
 * <p>
 * 自 sip-gateway 1.8.0 起，入站消息处理（注册/心跳/Catalog/INVITE/Bye 等）统一由
 * {@link io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier}
 * 经 {@link io.github.lunasaw.sipgateway.core.api.BusinessNotifier} 单一回调接入业务层，
 * 原先散落的服务端 request/response {@code Voglander*Handler} 已删除（接口在 1.8.0 中移除）。
 * </p>
 *
 * <p>
 * 本包仅保留平台服务端出向命令包装：基于 {@link io.github.lunasaw.gbproxy.server.transmit.cmd.ServerCommandSender}
 * （1.8.0 改为实例 Bean，按 {@code deviceId} 调用）发送设备查询/控制/PTZ/录像/媒体点播等指令，
 * 设备寻址信息由 {@link io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier.VoglanderDeviceSessionCache}
 * 提供，返回结果统一包装为 {@code ResultDTO}。
 * </p>
 *
 * @author luna
 * @since 1.0.0
 */
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server;
