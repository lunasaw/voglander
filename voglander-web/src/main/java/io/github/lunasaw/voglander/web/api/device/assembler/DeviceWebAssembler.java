package io.github.lunasaw.voglander.web.api.device.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.web.api.device.req.DeviceCreateReq;
import org.springframework.stereotype.Component;

/**
 * Web层设备数据转换器
 * 负责处理 Req -> DTO 的转换
 *
 * @author luna
 * @date 2024/01/30
 */
@Component
public class DeviceWebAssembler {

    /**
     * 将 DeviceCreateReq 转换为 DeviceDTO
     *
     * @param createReq 创建设备请求对象
     * @return DeviceDTO
     */
    public DeviceDTO toDeviceDTO(DeviceCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTO.setDeviceId(createReq.getDeviceId());
        deviceDTO.setName(createReq.getName());
        deviceDTO.setIp(createReq.getIp());
        deviceDTO.setPort(createReq.getPort());
        deviceDTO.setType(createReq.getType());
        deviceDTO.setServerIp(createReq.getServerIp());

        // 设置默认状态为离线
        deviceDTO.setStatus(0);

        // 转换扩展信息
        if (createReq.getExtendInfo() != null) {
            DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
            extendInfo.setSerialNumber(createReq.getExtendInfo().getSerialNumber());
            extendInfo.setTransport(createReq.getExtendInfo().getTransport());
            extendInfo.setExpires(createReq.getExtendInfo().getExpires());
            extendInfo.setPassword(createReq.getExtendInfo().getPassword());
            extendInfo.setStreamMode(createReq.getExtendInfo().getStreamMode());
            extendInfo.setCharset(createReq.getExtendInfo().getCharset());
            extendInfo.setDeviceInfo(createReq.getExtendInfo().getDeviceInfo());
            deviceDTO.setExtendInfo(extendInfo);
        }

        return deviceDTO;
    }
}