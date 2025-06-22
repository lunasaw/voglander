package io.github.lunasaw.voglander.web.api.device.assembler;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.web.api.device.req.DeviceCreateReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceUpdateReq;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

        // 优先使用subType和protocol来确定type值
        Integer type = getTypeFromSubTypeAndProtocol(createReq.getSubType(), createReq.getProtocol(), createReq.getType());
        deviceDTO.setType(type);

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

    /**
     * 批量将 DeviceCreateReq 转换为 DeviceDTO
     *
     * @param createReqList 创建设备请求对象列表
     * @return DeviceDTO 列表
     */
    public List<DeviceDTO> toDeviceDTOList(List<DeviceCreateReq> createReqList) {
        if (createReqList == null || createReqList.isEmpty()) {
            return Collections.emptyList();
        }
        return createReqList.stream()
            .map(this::toDeviceDTO)
            .collect(Collectors.toList());
    }

    /**
     * 将 DeviceUpdateReq 转换为 DeviceDTO
     *
     * @param updateReq 更新设备请求对象
     * @return DeviceDTO
     */
    public DeviceDTO toDeviceDTO(DeviceUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }

        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTO.setId(updateReq.getId());
        deviceDTO.setDeviceId(updateReq.getDeviceId());
        deviceDTO.setName(updateReq.getName());
        deviceDTO.setIp(updateReq.getIp());
        deviceDTO.setPort(updateReq.getPort());

        // 优先使用subType和protocol来确定type值
        Integer type = getTypeFromSubTypeAndProtocol(updateReq.getSubType(), updateReq.getProtocol(), updateReq.getType());
        deviceDTO.setType(type);

        deviceDTO.setServerIp(updateReq.getServerIp());
        deviceDTO.setStatus(updateReq.getStatus());

        // 转换扩展信息
        if (updateReq.getExtendInfo() != null) {
            DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
            extendInfo.setSerialNumber(updateReq.getExtendInfo().getSerialNumber());
            extendInfo.setTransport(updateReq.getExtendInfo().getTransport());
            extendInfo.setExpires(updateReq.getExtendInfo().getExpires());
            extendInfo.setPassword(updateReq.getExtendInfo().getPassword());
            extendInfo.setStreamMode(updateReq.getExtendInfo().getStreamMode());
            extendInfo.setCharset(updateReq.getExtendInfo().getCharset());
            extendInfo.setDeviceInfo(updateReq.getExtendInfo().getDeviceInfo());
            deviceDTO.setExtendInfo(extendInfo);
        }

        return deviceDTO;
    }

    /**
     * 批量将 DeviceUpdateReq 转换为 DeviceDTO
     *
     * @param updateReqList 更新设备请求对象列表
     * @return DeviceDTO 列表
     */
    public List<DeviceDTO> toUpdateDeviceDTOList(List<DeviceUpdateReq> updateReqList) {
        if (updateReqList == null || updateReqList.isEmpty()) {
            return Collections.emptyList();
        }
        return updateReqList.stream()
            .map(this::toDeviceDTO)
            .collect(Collectors.toList());
    }

    /**
     * 根据设备种类和协议获取type值
     * 优先使用subType和protocol，如果无法找到对应枚举则使用原始type值
     *
     * @param subType 设备种类
     * @param protocol 设备协议
     * @param originalType 原始type值
     * @return type值
     */
    private Integer getTypeFromSubTypeAndProtocol(Integer subType, Integer protocol, Integer originalType) {
        // 如果subType和protocol都不为空，则通过它们查找对应的type值
        if (subType != null && protocol != null) {
            Integer typeFromEnum = DeviceAgreementEnum.getTypeBySubTypeAndProtocol(subType, protocol);
            if (typeFromEnum != null) {
                return typeFromEnum;
            }
        }

        // 如果无法通过subType和protocol找到对应的枚举，则使用原始的type值
        return originalType;
    }
}