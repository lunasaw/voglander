package io.github.lunasaw.voglander.manager.assembler;

import com.luna.common.text.CharsetUtil;
import io.github.lunasaw.gb28181.common.entity.enums.StreamModeEnum;
import io.github.lunasaw.voglander.common.domain.DevicePageDTO;
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
     * 将 DeviceDO 转换为 DevicePageDTO
     * 并解析扩展字段，设置默认值
     *
     * @param deviceDO 数据库实体
     * @return DevicePageDTO
     */
    public DevicePageDTO toDevicePageDTO(DeviceDO deviceDO) {
        if (deviceDO == null) {
            return null;
        }

        DevicePageDTO devicePageDTO = new DevicePageDTO();
        devicePageDTO.setId(deviceDO.getId());
        devicePageDTO.setCreateTime(deviceDO.getCreateTime());
        devicePageDTO.setUpdateTime(deviceDO.getUpdateTime());
        devicePageDTO.setDeviceId(deviceDO.getDeviceId());
        devicePageDTO.setStatus(deviceDO.getStatus());
        devicePageDTO.setName(deviceDO.getName());
        devicePageDTO.setIp(deviceDO.getIp());
        devicePageDTO.setPort(deviceDO.getPort());
        devicePageDTO.setRegisterTime(deviceDO.getRegisterTime());
        devicePageDTO.setKeepaliveTime(deviceDO.getKeepaliveTime());
        devicePageDTO.setServerIp(deviceDO.getServerIp());
        devicePageDTO.setType(deviceDO.getType());
        devicePageDTO.setExtend(deviceDO.getExtend());

        // 解析扩展字段并设置默认值
        DevicePageDTO.ExtendInfo extendInfo = DevicePageDTO.parseExtendInfo(deviceDO.getExtend());
        setDefaultExtendInfo(extendInfo);
        devicePageDTO.setExtendInfo(extendInfo);

        return devicePageDTO;
    }

    /**
     * 批量将 DeviceDO 转换为 DevicePageDTO
     *
     * @param deviceDOList 数据库实体列表
     * @return DevicePageDTO 列表
     */
    public List<DevicePageDTO> toDevicePageDTOList(List<DeviceDO> deviceDOList) {
        if (deviceDOList == null || deviceDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return deviceDOList.stream()
                .map(this::toDevicePageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 DevicePageDTO 转换为 DeviceVO
     * 添加友好的显示名称
     * 注意：此方法返回基本的转换，具体的 VO 转换应在 web 层的 DeviceVO.convertVO() 方法中处理
     *
     * @param devicePageDTO 分页查询DTO
     * @return 转换后的数据，需要在 web 层进一步处理为具体的 VO 类型
     */
    public DevicePageDTO enrichDevicePageDTO(DevicePageDTO devicePageDTO) {
        // 这里可以添加额外的数据处理逻辑
        // 比如添加计算字段、关联数据等
        return devicePageDTO;
    }

    /**
     * 批量处理 DevicePageDTO
     *
     * @param devicePageDTOList 分页查询DTO列表
     * @return 处理后的 DevicePageDTO 列表
     */
    public List<DevicePageDTO> enrichDevicePageDTOList(List<DevicePageDTO> devicePageDTOList) {
        if (devicePageDTOList == null || devicePageDTOList.isEmpty()) {
            return Collections.emptyList();
        }
        return devicePageDTOList.stream()
                .map(this::enrichDevicePageDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将 DeviceDO 转换为 DeviceDTO（业务DTO）
     *
     * @param deviceDO 数据库实体
     * @return DeviceDTO
     */
    public DeviceDTO toDeviceDTO(DeviceDO deviceDO) {
        return DeviceDTO.convertDTO(deviceDO);
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
     * 设置扩展信息默认值
     *
     * @param extendInfo 扩展信息
     */
    private void setDefaultExtendInfo(DevicePageDTO.ExtendInfo extendInfo) {
        if (extendInfo.getCharset() == null || extendInfo.getCharset().isEmpty()) {
            extendInfo.setCharset(CharsetUtil.UTF_8);
        }
        if (extendInfo.getStreamMode() == null || extendInfo.getStreamMode().isEmpty()) {
            extendInfo.setStreamMode(StreamModeEnum.UDP.getType());
        }
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