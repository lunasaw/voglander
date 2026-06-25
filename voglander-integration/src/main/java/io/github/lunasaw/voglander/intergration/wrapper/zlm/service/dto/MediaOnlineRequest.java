package io.github.lunasaw.voglander.intergration.wrapper.zlm.service.dto;

import io.github.lunasaw.zlm.entity.req.MediaReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体在线状态查询请求DTO
 * <p>
 * 封装ZLM媒体在线状态查询操作的请求参数，提供Builder模式构建
 * </p>
 *
 * @author luna
 * @since 2025-01-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaOnlineRequest {

    /**
     * ZLM节点地址
     */
    private String   host;

    /**
     * ZLM节点密钥
     */
    private String   secret;

    /**
     * 媒体查询请求
     */
    private MediaReq mediaReq;

    /**
     * 便捷创建方法
     */
    public static MediaOnlineRequest of(String host, String secret, MediaReq mediaReq) {
        return MediaOnlineRequest.builder()
            .host(host)
            .secret(secret)
            .mediaReq(mediaReq)
            .build();
    }

    /**
     * 便捷创建方法（直接传入app和stream）
     */
    public static MediaOnlineRequest of(String host, String secret, String app, String stream) {
        MediaReq mediaReq = new MediaReq();
        mediaReq.setApp(app);
        mediaReq.setStream(stream);

        return MediaOnlineRequest.builder()
            .host(host)
            .secret(secret)
            .mediaReq(mediaReq)
            .build();
    }
}