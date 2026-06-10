package io.github.lunasaw.voglander.e2e;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import io.github.lunasaw.voglander.manager.event.ShardDispatcher;
import io.github.lunasaw.voglander.web.ApplicationWeb;

/**
 * E2E 测试基类：统一所有真实 SIP 协议栈端到端用例的 Spring 上下文，消除上下文碎片化。
 * <p>
 * 背景：{@code SipLayer}(sip-common) 以「JVM 全局静态 Map」保存 SIP 协议栈与监听点，
 * SIP 监听器与固定端口 5060/5061 在整个 JVM 内唯一且只在上下文启动时由 {@code ServerStart}
 * (CommandLineRunner) 绑定一次。该库未提供「停止协议栈/释放端口/重置 isShuttingDown」的公共 API：
 * {@code clearAllListeningPoints()} 只 removeSipListener + 清空 Map，并不调用 {@code sipStack.stop()}，
 * 端口仍被遗留协议栈占用；且会把静态 {@code isShuttingDown} 永久置 true。
 * 因此「每类一份上下文 + 关闭后重绑」的隔离路线在本库下不可行(首个类之后所有真实 SIP 类确定性失败)。
 * <p>
 * 原偶发失败的真正根因是「测试态上下文碎片化」：部分用例用 {@code @MockitoSpyBean ShardDispatcher}
 * 派生出不同的上下文缓存键，使多个 Spring 上下文并存、争抢同一 5060 监听器；
 * SIP 入站事件只路由到「首个完成绑定」的上下文监听器，类执行顺序在不同运行间变化 → 失败集漂移。
 * <p>
 * 解决：所有 E2E 类共享本基类完全一致的上下文配置(含同一个 {@code @MockitoSpyBean} 覆盖),
 * Spring TestContext 据此命中同一缓存键 → 全程唯一上下文、唯一监听器、端口只绑一次、路由稳定。
 * spy 默认委派真实 {@code ShardDispatcher}，故未显式打桩的 7 个类仍走真实分发与落库；
 * Mockito 在每个测试方法后自动重置 spy。需要拦截事件的类(Alarm/MediaInvite/Playback/VoiceBroadcast)
 * 直接复用继承来的 {@link #shardSpy} 字段进行 {@code doAnswer}/验证。仅改测试、不动生产启动代码。
 */
@SpringBootTest(classes = ApplicationWeb.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public abstract class BaseE2eTest {

    /**
     * 全 E2E 共享的 ShardDispatcher spy。声明在基类以保证 11 个类的上下文覆盖元数据完全一致，
     * 从而命中同一个上下文缓存键(单一上下文、单一 SIP 监听器、路由确定)。
     * 默认委派真实实现，需要拦截事件链路的子类可直接对其 {@code doAnswer}。
     */
    @MockitoSpyBean
    protected ShardDispatcher shardSpy;
}
