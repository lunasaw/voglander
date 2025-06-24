package io.github.lunasaw.voglander.web.api.channel.assembler;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelCreateReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelUpdateReq;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Web层设备通道数据转换器
 * 负责处理 Req -> DTO 的转换
 *
 * @author luna
 * @date 2024/01/30
 */
@Component
public class DeviceChannelWebAssembler {

    /**
     * 将 DeviceChannelCreateReq 转换为 DeviceChannelDTO
     *
     * @param createReq 创建设备通道请求对象
     * @return DeviceChannelDTO
     */
    public DeviceChannelDTO toDeviceChannelDTO(DeviceChannelCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setChannelId(createReq.getChannelId());
        dto.setDeviceId(createReq.getDeviceId());
        dto.setName(createReq.getName());
        dto.setStatus(DeviceConstant.Status.ONLINE); // 默认在线状态

        // 设置时间字段
        Date now = new Date();
        dto.setCreateTime(now);
        dto.setUpdateTime(now);

        // 转换扩展信息
        if (createReq.getExtendInfo() != null) {
            DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
            extendInfo.setChannelInfo(createReq.getExtendInfo().getChannelInfo());
            dto.setExtendInfo(extendInfo);
        }

        return dto;
    }

    /**
     * 批量将 DeviceChannelCreateReq 转换为 DeviceChannelDTO
     *
     * @param createReqList 创建设备通道请求列表
     * @return DeviceChannelDTO 列表
     */
    public List<DeviceChannelDTO> toDeviceChannelDTOList(List<DeviceChannelCreateReq> createReqList) {
        if (createReqList == null || createReqList.isEmpty()) {
            return Collections.emptyList();
        }
        return createReqList.stream()
                .map(this::toDeviceChannelDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 DeviceChannelUpdateReq 转换为 DeviceChannelDTO
     *
     * @param updateReq 更新设备通道请求对象
     * @return DeviceChannelDTO
     */
    public DeviceChannelDTO toDeviceChannelDTO(DeviceChannelUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }

        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setId(updateReq.getId());
        dto.setChannelId(updateReq.getChannelId());
        dto.setDeviceId(updateReq.getDeviceId());
        dto.setName(updateReq.getName());
        dto.setStatus(updateReq.getStatus());

        // 设置更新时间
        dto.setUpdateTime(new Date());

        // 转换扩展信息
        if (updateReq.getExtendInfo() != null) {
            DeviceChannelDTO.ExtendInfo extendInfo = new DeviceChannelDTO.ExtendInfo();
            extendInfo.setChannelInfo(updateReq.getExtendInfo().getChannelInfo());
            dto.setExtendInfo(extendInfo);
        }

        return dto;
    }

    /**
     * 批量将 DeviceChannelUpdateReq 转换为 DeviceChannelDTO
     *
     * @param updateReqList 更新设备通道请求列表
     * @return DeviceChannelDTO 列表
     */
    public List<DeviceChannelDTO> toUpdateDeviceChannelDTOList(List<DeviceChannelUpdateReq> updateReqList) {
        if (updateReqList == null || updateReqList.isEmpty()) {
            return Collections.emptyList();
        }
        return updateReqList.stream()
                .map(this::toDeviceChannelDTO)
                .collect(Collectors.toList());
    }
}