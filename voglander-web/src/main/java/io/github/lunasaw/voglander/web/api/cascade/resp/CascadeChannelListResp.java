package io.github.lunasaw.voglander.web.api.cascade.resp;

import java.io.Serializable;
import java.util.List;

import io.github.lunasaw.voglander.web.api.cascade.vo.CascadeChannelVO;
import lombok.Data;

/**
 * 级联上报通道分页列表响应（统一 total + items 包装）。
 *
 * @author luna
 * @date 2026/06/16
 */
@Data
public class CascadeChannelListResp implements Serializable {

    private static final long          serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long                       total;

    /**
     * 通道列表
     */
    private List<CascadeChannelVO>     items;
}
