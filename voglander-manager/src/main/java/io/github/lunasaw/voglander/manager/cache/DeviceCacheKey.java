package io.github.lunasaw.voglander.manager.cache;

/**
 * 设备缓存 Key 统一生成工具（Phase 1：修 P2 key 不一致）。
 * <p>
 * {@code @Cacheable}、手动 evict、{@code clearCache} 三处共用同一套 key 生成逻辑，
 * 杜绝散落字符串导致的"写入 key 与 evict key 互不命中"脏读根因。
 * </p>
 * <p>
 * 旧代码三套 key 互不命中：{@code @Cacheable(key="#deviceId")} 写裸 deviceId，
 * 而 clearCache evict 的是 {@code "do:"/"dto:"} 前缀 key，精确 evict 永不命中，
 * 仅靠 {@code cache.clear()} 兜底（P1）。本工具统一为单一前缀方案。
 * </p>
 *
 * @author luna
 */
public final class DeviceCacheKey {

    /**
     * 设备单对象缓存区名称。
     */
    public static final String CACHE_NAME      = "device";

    /**
     * 设备列表/分页缓存区名称（与单对象隔离，短 TTL，单对象写不连坐）。
     */
    public static final String LIST_CACHE_NAME = "device:list";

    private static final String ID_PREFIX        = "id:";
    private static final String DEVICE_ID_PREFIX = "deviceId:";

    private DeviceCacheKey() {}

    /**
     * 主键 ID 缓存 key。
     *
     * @param id 设备数据库主键
     * @return 形如 {@code id:123}
     */
    public static String byId(Long id) {
        return ID_PREFIX + id;
    }

    /**
     * 业务键 deviceId 缓存 key。
     *
     * @param deviceId 设备国标 ID
     * @return 形如 {@code deviceId:340200...}
     */
    public static String byDeviceId(String deviceId) {
        return DEVICE_ID_PREFIX + deviceId;
    }
}
