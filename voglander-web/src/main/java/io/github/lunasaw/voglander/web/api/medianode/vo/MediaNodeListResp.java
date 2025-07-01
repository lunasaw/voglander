package io.github.lunasaw.voglander.web.api.medianode.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 流媒体节点列表响应VO
 *
 * @author luna
 * @since 2025-01-23
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class MediaNodeListResp implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流媒体节点列表
     */
    private List<MediaNodeVO> items;
}