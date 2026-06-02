/**
 * GB28181 客户端出向命令包装层。
 *
 * <p>
 * 自 sip-gateway 1.8.0 起，入站消息处理（注册/心跳/Catalog/INVITE 等）统一由
 * {@link io.github.lunasaw.voglander.intergration.wrapper.gb28181.notifier.VoglanderBusinessNotifier}
 * 经 {@link io.github.lunasaw.sipgateway.core.api.BusinessNotifier} 单一回调接入业务层，
 * 原先散落的 20 个 {@code Voglander*Handler}（client/server 的 request/response）已删除。
 * </p>
 *
 * <p>
 * 本包仅保留客户端（设备侧模拟）出向命令包装：基于 {@code ClientCommandSender}（1.8.0 仍为静态 API）
 * 发送告警/目录/设备信息/PTZ/录像/状态等指令，返回结果统一包装为 {@code ResultDTO}。
 * </p>
 *
 * @author luna
 * @since 1.0.0
 */
package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client;
