package io.github.lunasaw.voglander.manager.assembler;


import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备数据转换器
 * 负责处理 DO、DTO、VO 之间的数据转换
 *
 * @author luna
 * @date 2024/01/30
 */
@Component
public class DeviceAssembler {

    /**
     * 将 DeviceDO 转换为 DeviceDTO
     * 复用原有的转换逻辑，包含扩展字段解析和默认值设置
     *
     * @param deviceDO 数据库实体
     * @return DeviceDTO
     */
    public DeviceDTO toDeviceDTO(DeviceDO deviceDO) {
        return DeviceDTO.convertDTO(deviceDO);
    }

    /**
     * 批量将 DeviceDO 转换为 DeviceDTO
     *
     * @param deviceDOList 数据库实体列表
     * @return DeviceDTO 列表
     */
    public List<DeviceDTO> toDeviceDTOList(List<DeviceDO> deviceDOList) {
        if (deviceDOList == null || deviceDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceDOList.stream()
            .map(this::toDeviceDTO)
                .collect(Collectors.toList());
    }



    /**
     * 将 DeviceDTO 转换为 DeviceDO
     *
     * @param deviceDTO 业务DTO
     * @return DeviceDO
     */
    public DeviceDO toDeviceDO(DeviceDTO deviceDTO) {
        return DeviceDTO.convertDO(deviceDTO);
    }

    /**
     * 获取状态显示名称
     *
     * @param status 状态码
     * @return 状态名称
     */
    public String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "在线";
            case 0:
                return "离线";
            default:
                return "异常";
        }
    }

    /**
     * 获取设备类型显示名称
     *
     * @param type 类型码
     * @return 类型名称
     */
    public String getTypeName(Integer type) {
        DeviceAgreementEnum agreementEnum = DeviceAgreementEnum.getByType(type);
        return agreementEnum != null ? agreementEnum.getDesc() : "未知类型";
    }
}