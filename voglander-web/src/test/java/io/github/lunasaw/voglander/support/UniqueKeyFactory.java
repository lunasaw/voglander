package io.github.lunasaw.voglander.support;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 测试唯一键生成工厂
 * 用于并发测试场景生成全局唯一的业务键，避免测试数据冲突
 *
 * 格式: {prefix}-{timestamp}-{threadId}-{uuid4}
 *
 * @author luna
 */
public class UniqueKeyFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static String generate(String prefix) {
        long timestamp = System.currentTimeMillis();
        long threadId = Thread.currentThread().getId();
        String uuid = UUID.randomUUID().toString().substring(0, 4);
        int count = COUNTER.incrementAndGet();
        return String.format("%s-%d-%d-%s-%d", prefix, timestamp, threadId, uuid, count);
    }

    public static String deviceId() {
        return generate("dev");
    }

    public static String serverId() {
        return generate("srv");
    }

    public static String callId() {
        return generate("call");
    }

    public static String app() {
        return generate("app");
    }

    public static String stream() {
        return generate("stream");
    }

    public static String channelId() {
        return generate("ch");
    }
}
