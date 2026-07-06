package io.github.lunasaw.voglander.common.constant.cascade;

/**
 * GB28181 级联（下级平台）相关常量。
 *
 * <p>本平台作为级联下级平台对接上级时使用：注册保活、上级订阅、录像转查等。
 *
 * @author luna
 */
public interface CascadeConstant {

    /** 级联注册状态（对齐 tb_cascade_platform.register_status） */
    interface RegisterStatus {
        int OFFLINE     = 0;
        int ONLINE      = 1;
        int REGISTERING = 2;
        int FAILED      = 3;
    }

    /** 订阅类型（与 GB28181 Event 头映射） */
    enum SubType {

        CATALOG("Catalog"),
        ALARM("presence.alarm"),
        MOBILE_POSITION("presence.mobile");

        private final String event;

        SubType(String event) {
            this.event = event;
        }

        public String event() {
            return event;
        }
    }

    /** 订阅状态（对齐 tb_cascade_subscribe.status） */
    interface SubStatus {
        int EXPIRED = 0;
        int ACTIVE  = 1;
    }

    /** 录像查询请求状态（对齐 tb_cascade_record_request.status） */
    interface RecordReqStatus {
        int PENDING   = 0;
        int RESPONDED = 1;
        int TIMEOUT   = 2;
    }

    /** 默认订阅有效期(秒) */
    int DEFAULT_SUBSCRIBE_EXPIRES = 3600;

    /** Keepalive 心跳间隔(秒) */
    int DEFAULT_KEEPALIVE_INTERVAL = 60;

    /** 级联注册响应超时(秒)：超时仍处于 REGISTERING 则标记失败，避免状态永久卡住 */
    int DEFAULT_REGISTER_TIMEOUT_SEC = 30;

    /** 录像查询请求超时(秒)，超时清理 PENDING */
    int RECORD_REQUEST_TIMEOUT_SEC = 30;

    /** 级联 INVITE 媒体 app */
    String CASCADE_MEDIA_APP = "rtp";
}
