package io.github.lunasaw.voglander.manager.event;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件分片槽（Phase 4）。
 * <p>
 * 单线程顺序消费一个队列，保证同 shardKey 的事件串行处理。队列满时：
 * <ul>
 *   <li>冗余 Keepalive：立即丢弃（返回 false）</li>
 *   <li>关键事件（Register/Invite/Offline）：阻塞等待入队</li>
 * </ul>
 * </p>
 *
 * @author luna
 */
@Slf4j
public class EventShard {

    @Getter
    private final int shardId;

    private final BlockingQueue<DeviceEvent> queue;

    private final InboundEventDispatcher dispatcher;

    private volatile boolean running = true;

    private final AtomicLong processedCount = new AtomicLong(0);

    private final AtomicLong droppedKeepaliveCount = new AtomicLong(0);

    public EventShard(int shardId, BlockingQueue<DeviceEvent> queue, InboundEventDispatcher dispatcher) {
        this.shardId = shardId;
        this.queue = queue;
        this.dispatcher = dispatcher;
    }

    /**
     * 启动单线程消费循环。
     */
    public void start() {
        log.info("EventShard-{} 启动消费", shardId);
        while (running) {
            try {
                DeviceEvent event = queue.poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    dispatcher.dispatch(event);
                    processedCount.incrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("EventShard-{} 消费线程被中断", shardId);
                break;
            } catch (Exception e) {
                log.error("EventShard-{} 处理事件异常", shardId, e);
            }
        }
        log.info("EventShard-{} 停止，已处理 {} 事件，丢弃 {} 个 Keepalive",
            shardId, processedCount.get(), droppedKeepaliveCount.get());
    }

    /**
     * 提交事件到队列。队列满时的策略：
     * <ul>
     *   <li>Keepalive：立即丢弃</li>
     *   <li>其他关键事件：阻塞等待（最多 5 秒）</li>
     * </ul>
     *
     * @return true=入队成功，false=已丢弃
     */
    public boolean offer(DeviceEvent event) {
        if (event == null) {
            return false;
        }

        // 判断是否为冗余 Keepalive
        boolean isKeepalive = "Keepalive".equals(event.name())
            || ("Notify".equals(event.group()) && "Keepalive".equals(event.name()));

        if (isKeepalive) {
            // Keepalive 立即丢弃
            boolean offered = queue.offer(event);
            if (!offered) {
                droppedKeepaliveCount.incrementAndGet();
                if (droppedKeepaliveCount.get() % 100 == 0) {
                    log.warn("EventShard-{} 已丢弃 {} 个 Keepalive", shardId, droppedKeepaliveCount.get());
                }
            }
            return offered;
        } else {
            // 关键事件阻塞等待（最多 5 秒）
            try {
                boolean offered = queue.offer(event, 5, TimeUnit.SECONDS);
                if (!offered) {
                    log.error("EventShard-{} 关键事件入队超时 5 秒，type={}, deviceId={}",
                        shardId, event.type(), event.deviceId());
                }
                return offered;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("EventShard-{} 入队被中断", shardId);
                return false;
            }
        }
    }

    /**
     * 停止消费。
     */
    public void shutdown() {
        log.info("EventShard-{} 准备停止，队列剩余 {} 事件", shardId, queue.size());
        running = false;
    }

    /**
     * 获取队列当前大小。
     */
    public int getQueueSize() {
        return queue.size();
    }

    /**
     * 获取已处理事件数。
     */
    public long getProcessedCount() {
        return processedCount.get();
    }

    /**
     * 获取已丢弃 Keepalive 数。
     */
    public long getDroppedKeepaliveCount() {
        return droppedKeepaliveCount.get();
    }
}
