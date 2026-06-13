package io.github.lunasaw.voglander.web.api.device.resp;

import java.io.Serializable;
import java.util.List;

import io.github.lunasaw.voglander.web.api.device.vo.DeviceVO;
import lombok.Data;

/**
 * 设备分页列表响应对象（S1 设备列表筛选，统一 total + items 包装）。
 *
 * @author luna
 * @date 2026/06/13
 */
@Data
public class DeviceListResp implements Serializable {

    private static final long  serialVersionUID = 1L;

    /**
     * 总记录数
     */
    private Long               total;

    /**
     * 设备列表
     */
    private List<DeviceVO>     items;
}
