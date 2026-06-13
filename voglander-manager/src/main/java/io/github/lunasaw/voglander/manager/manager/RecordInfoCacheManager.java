package io.github.lunasaw.voglander.manager.manager;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;
import lombok.extern.slf4j.Slf4j;

/**
 * 录像查询结果缓存管理器（S3）。
 *
 * <p>
 * 录像查询返回的是列表型数据（{@code DeviceRecord.recordList}），不塞 {@code tb_device.extend}，
 * 改用 {@link RedisCache} 缓存（值为 FastJSON 字符串，TTL 默认 10 分钟，符合 ARCHITECTURE.md §13.1
 * 列表型结果走 RedisCache、不 @Cached 的约定）。
 * </p>
 *
 * <p>
 * key 由 {@code (deviceId, sn)} 组成；{@code DeviceRecord} 实体无 channelId 字段，sn 取入站事件的
 * correlationId（或实体内 cmdType 关联）。前端先发 {@code /record} 触发查询，再轮询读缓存。
 * </p>
 *
 * @author luna
 * @date 2026/06/13
 */
@Slf4j
@Component
public class RecordInfoCacheManager {

    private static final String KEY_PREFIX     = "device:record:result:";

    /**
     * 缓存有效期（分钟）
     */
    private static final int    TTL_MINUTES    = 10;

    @Autowired
    private RedisCache          redisCache;

    /**
     * 写入录像查询结果缓存。
     *
     * @param deviceId   设备国标 ID
     * @param sn         查询序列号（入站事件 correlationId）
     * @param recordJson 录像结果的 FastJSON 字符串
     */
    public void put(String deviceId, String sn, String recordJson) {
        if (deviceId == null || recordJson == null) {
            return;
        }
        String key = buildKey(deviceId, sn);
        redisCache.setCacheObject(key, recordJson, TTL_MINUTES, TimeUnit.MINUTES);
        log.info("录像结果缓存写入, deviceId={}, sn={}", deviceId, sn);
    }

    /**
     * 读取录像查询结果缓存。
     *
     * @param deviceId 设备国标 ID
     * @param sn       查询序列号
     * @return 录像结果 FastJSON 字符串；不存在返回 null
     */
    public String get(String deviceId, String sn) {
        if (deviceId == null) {
            return null;
        }
        return redisCache.getCacheObject(buildKey(deviceId, sn));
    }

    private String buildKey(String deviceId, String sn) {
        return KEY_PREFIX + deviceId + ":" + (sn != null ? sn : "_");
    }
}
