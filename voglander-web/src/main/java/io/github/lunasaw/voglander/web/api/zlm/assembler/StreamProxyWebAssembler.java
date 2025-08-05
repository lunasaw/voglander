package io.github.lunasaw.voglander.web.api.zlm.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * 拉流代理Web层装配器
 * 负责Web层对象与业务层对象的转换
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class StreamProxyWebAssembler {

    /**
     * 创建请求转DTO
     *
     * @param createReq 创建请求
     * @return DTO对象
     */
    public StreamProxyDTO createReqToDto(StreamProxyCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(createReq.getApp());
        dto.setStream(createReq.getStream());
        dto.setUrl(createReq.getUrl());
        dto.setDescription(createReq.getDescription());
        dto.setEnabled(createReq.getEnabled());
        dto.setExtend(createReq.getExtend());

        return dto;
    }

    /**
     * 更新请求转DTO
     *
     * @param updateReq 更新请求
     * @return DTO对象
     */
    public StreamProxyDTO updateReqToDto(StreamProxyUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }

        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setDescription(updateReq.getDescription());
        dto.setStatus(updateReq.getStatus());
        dto.setEnabled(updateReq.getEnabled());
        dto.setExtend(updateReq.getExtend());

        return dto;
    }

    /**
     * DTO转VO
     *
     * @param dto DTO对象
     * @return VO对象
     */
    public StreamProxyVO dtoToVo(StreamProxyDTO dto) {
        if (dto == null) {
            return null;
        }

        StreamProxyVO vo = new StreamProxyVO();
        vo.setId(dto.getId());
        vo.setApp(dto.getApp());
        vo.setStream(dto.getStream());
        vo.setUrl(dto.getUrl());
        vo.setStatus(dto.getStatus());
        vo.setOnlineStatus(dto.getOnlineStatus());
        vo.setProxyKey(dto.getProxyKey());
        vo.setDescription(dto.getDescription());
        vo.setEnabled(dto.getEnabled());
        vo.setExtend(dto.getExtend());

        // 时间转换为时间戳
        if (dto.getCreateTime() != null) {
            vo.setCreateTime(dto.getCreateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        if (dto.getUpdateTime() != null) {
            vo.setUpdateTime(dto.getUpdateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }

        return vo;
    }
}