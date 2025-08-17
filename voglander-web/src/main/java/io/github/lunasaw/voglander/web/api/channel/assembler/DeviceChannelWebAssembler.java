package io.github.lunasaw.voglander.web.api.channel.assembler;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelCreateReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelQueryReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelUpdateReq;
import io.github.lunasaw.voglander.web.api.channel.vo.DeviceChannelVO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
        LocalDateTime now = LocalDateTime.now();
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
        dto.setUpdateTime(LocalDateTime.now());

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

    // ================================
    // 新增Web层模板方法转换器
    // ================================

    /**
     * CreateReq -> DTO 转换
     */
    public DeviceChannelDTO createReqToDto(DeviceChannelCreateReq createReq) {
        return toDeviceChannelDTO(createReq);
    }

    /**
     * UpdateReq -> DTO 转换
     */
    public DeviceChannelDTO updateReqToDto(DeviceChannelUpdateReq updateReq) {
        return toDeviceChannelDTO(updateReq);
    }

    /**
     * QueryReq -> DTO 转换
     */
    public DeviceChannelDTO queryReqToDto(DeviceChannelQueryReq queryReq) {
        if (queryReq == null) {
            return new DeviceChannelDTO(); // 返回空条件，查询所有
        }

        DeviceChannelDTO dto = new DeviceChannelDTO();
        dto.setId(queryReq.getId());
        dto.setChannelId(queryReq.getChannelId());
        dto.setDeviceId(queryReq.getDeviceId());
        dto.setName(queryReq.getName());
        dto.setStatus(queryReq.getStatus());

        return dto;
    }

    /**
     * DTO -> VO 转换
     */
    public DeviceChannelVO dtoToVo(DeviceChannelDTO dto) {
        return DeviceChannelVO.convertVO(dto);
    }

    /**
     * DTO列表 -> VO列表 转换
     */
    public List<DeviceChannelVO> dtoListToVoList(List<DeviceChannelDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return Collections.emptyList();
        }
        return dtoList.stream()
            .map(DeviceChannelVO::convertVO)
            .collect(Collectors.toList());
    }
}