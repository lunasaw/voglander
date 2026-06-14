package io.github.lunasaw.voglander.common.constant.device;

/**
 * GB28181-2022 §9.11 设备订阅（目录 / 移动位置 / 告警）相关常量。
 * <p>
 * Event 头取值、订阅有效期/上报间隔默认值、续订提前量等集中收口于此，
 * 字段语义严格对齐 GB/T 28181-2022 附录 A.2.4。
 * </p>
 *
 * @author luna
 */
public interface SubscriptionConstant {

    /**
     * 订阅类型（与 {@code tb_device_subscription.sub_type} 取值一致）。
     */
    enum Type {
        CATALOG,
        MOBILE_POSITION,
        ALARM
    }

    /**
     * 运行态（{@code tb_device_subscription.status}）。
     */
    interface Status {
        int INACTIVE = 0;
        int ACTIVE   = 1;
        int PENDING  = 2;
        int FAILED   = 3;
    }

    /**
     * 默认订阅有效期（秒）：目录 / 告警 / 位置均 3600。
     */
    int DEFAULT_EXPIRES = 3600;

    /**
     * 位置默认上报间隔（秒）。
     */
    int DEFAULT_POSITION_INTERVAL = 5;

    /**
     * 续订提前量（秒）：过期前 N 秒发起 refresh。
     */
    int REFRESH_AHEAD_SECONDS = 120;

    /**
     * GB28181 Event 头取值——目录订阅。
     */
    String EVENT_CATALOG = "Catalog";

    /**
     * GB28181 Event 头取值——移动位置订阅。
     */
    String EVENT_MOBILE_POSITION = "presence.mobile";

    /**
     * GB28181 Event 头取值——告警订阅。
     */
    String EVENT_ALARM = "presence.alarm";
}
