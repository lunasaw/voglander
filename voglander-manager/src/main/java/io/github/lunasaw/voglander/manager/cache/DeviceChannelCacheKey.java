package io.github.lunasaw.voglander.manager.cache;

/**
 * 设备通道缓存 Key 统一生成工具（1.0.4）。
 * 仿 {@link DeviceCacheKey}，收敛 key 生成，废弃 DEVICE_CHANNEL_CACHE_PREFIX 裸字符串。
 *
 * @author luna
 */
public final class DeviceChannelCacheKey {

    /** Spring Cache 命名空间：单对象缓存（TTL 默认 3min） */
    public static final String CACHE_NAME      = "deviceChannel";

    /** Spring Cache 命名空间：列表缓存（TTL 独立 60s） */
    public static final String LIST_CACHE_NAME = "deviceChannel:list";

    private static final String ID_PREFIX  = "id:";
    private static final String BIZ_PREFIX = "biz:";
    private static final String DEV_PREFIX = "device:";

    private DeviceChannelCacheKey() {}

    public static String byId(Long id) {
        return ID_PREFIX + id;
    }

    public static String byBizKey(String deviceId, String channelId) {
        return BIZ_PREFIX + deviceId + ":" + channelId;
    }

    public static String byDevice(String deviceId) {
        return DEV_PREFIX + deviceId;
    }
}
