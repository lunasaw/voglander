package io.github.lunasaw.voglander.manager.event;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.client.domain.event.DeviceEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件分片调度器（Phase 4）。
 * <p>
 * 按 shardKey（deviceId 优先，null 时用 correlationId）哈希分片，
 * 保证同设备/会话事件路由到同一单线程槽，实现串行处理。
 * </p>
 * <p>
 * 灰度开关：{@code voglander.event.shard.enabled=true/false}（默认 true）。
 * </p>
 *
 * @author luna
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "voglander.event.shard.enabled", havingValue = "true", matchIfMissing = true)
public class ShardDispatcher {

    private final int shardCount;

    private final List<EventShard> shards;

    private final ExecutorService executor;

    /**
     * 构造函数，默认 16 个分片，每槽队列容量 2000。
     */
    @Autowired
    public ShardDispatcher(
        @Value("${voglander.event.shard.count:16}") int shardCount,
        InboundEventDispatcher eventDispatcher) {
        this(shardCount, 2000, eventDispatcher);
    }

    /**
     * 测试用构造函数，可指定队列容量。
     */
    ShardDispatcher(int shardCount, int queueCapacity, InboundEventDispatcher eventDispatcher) {
        this.shardCount = shardCount;
        this.shards = new ArrayList<>(shardCount);
        this.executor = Executors.newFixedThreadPool(shardCount,
            r -> new Thread(r, "event-shard-" + shards.size()));

        // 创建分片槽
        for (int i = 0; i < shardCount; i++) {
            BlockingQueue<DeviceEvent> queue = new LinkedBlockingQueue<>(queueCapacity);
            EventShard shard = new EventShard(i, queue, eventDispatcher);
            shards.add(shard);
        }

        log.info("ShardDispatcher 初始化完成，分片数: {}, 每槽队列容量: {}", shardCount, queueCapacity);

        // 自动启动所有分片
        start();
    }

    /**
     * 启动所有分片消费线程。
     */
    public void start() {
        log.info("启动 {} 个事件分片消费线程", shardCount);
        for (EventShard shard : shards) {
            executor.submit(shard::start);
        }
    }

    /**
     * 分发事件到对应分片。
     */
    public void dispatch(DeviceEvent event) {
        if (event == null) {
            return;
        }

        int shardIndex = getShardIndex(event);
        EventShard shard = shards.get(shardIndex);
        shard.offer(event);
    }

    /**
     * 计算事件的分片索引。
     * <p>
     * 规则：deviceId 优先，null 时用 correlationId（满足 Session 事件需求）。
     * </p>
     */
    int getShardIndex(DeviceEvent event) {
        String shardKey = event.deviceId() != null ? event.deviceId() : event.correlationId();
        if (shardKey == null) {
            // 极端情况：都为 null，用 type 兜底
            shardKey = event.type();
        }
        return Math.floorMod(shardKey.hashCode(), shardCount);
    }

    /**
     * 优雅关闭所有分片（Spring 容器销毁时自动调用）。
     */
    public void shutdown() {
        log.info("开始关闭 ShardDispatcher");

        // 停止所有分片
        for (EventShard shard : shards) {
            shard.shutdown();
        }

        // 关闭线程池
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("分片线程池 10 秒内未完全停止，强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }

        // 统计信息
        long totalProcessed = shards.stream().mapToLong(EventShard::getProcessedCount).sum();
        long totalDropped = shards.stream().mapToLong(EventShard::getDroppedKeepaliveCount).sum();
        log.info("ShardDispatcher 已关闭，总处理 {} 事件，总丢弃 {} 个 Keepalive", totalProcessed, totalDropped);
    }

    /**
     * 获取分片数量。
     */
    public int getShardCount() {
        return shardCount;
    }

    /**
     * 获取指定分片的队列大小。
     */
    public int getShardQueueSize(int shardId) {
        if (shardId < 0 || shardId >= shardCount) {
            return 0;
        }
        return shards.get(shardId).getQueueSize();
    }
}
