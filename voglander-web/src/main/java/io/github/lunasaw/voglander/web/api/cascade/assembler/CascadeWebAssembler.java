package io.github.lunasaw.voglander.web.api.cascade.assembler;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelBatchBindReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelCreateReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelPageReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadeChannelUpdateReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformCreateReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformPageReq;
import io.github.lunasaw.voglander.web.api.cascade.req.CascadePlatformUpdateReq;

import java.util.ArrayList;
import java.util.List;

/**
 * Web 层级联数据转换器，负责 Req → DTO 转换。
 *
 * <p>
 * 字段名严格对齐 {@code tb_cascade_platform} / {@code tb_cascade_channel}（platformIp / localIp 等），
 * 不引入 serverIp 等臆造名。
 * </p>
 *
 * @author luna
 * @date 2026/06/16
 */
@Component
public class CascadeWebAssembler {

    // ==================== 平台 ====================

    /**
     * 平台新增请求 → DTO。
     */
    public CascadePlatformDTO toDTO(CascadePlatformCreateReq req) {
        if (req == null) {
            return null;
        }
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setPlatformId(req.getPlatformId());
        dto.setPlatformIp(req.getPlatformIp());
        dto.setPlatformPort(req.getPlatformPort());
        dto.setPlatformDomain(req.getPlatformDomain());
        dto.setUsername(req.getUsername());
        dto.setPassword(req.getPassword());
        dto.setLocalClientId(req.getLocalClientId());
        dto.setLocalIp(req.getLocalIp());
        dto.setLocalPort(req.getLocalPort());
        dto.setKeepaliveInterval(req.getKeepaliveInterval());
        dto.setRegisterExpires(req.getRegisterExpires());
        dto.setCharset(req.getCharset());
        dto.setTransport(req.getTransport());
        return dto;
    }

    /**
     * 平台更新请求 → DTO（id 透传，platformId 展示不参与更新键）。
     */
    public CascadePlatformDTO toDTO(CascadePlatformUpdateReq req) {
        if (req == null) {
            return null;
        }
        CascadePlatformDTO dto = new CascadePlatformDTO();
        dto.setId(req.getId());
        dto.setPlatformId(req.getPlatformId());
        dto.setPlatformIp(req.getPlatformIp());
        dto.setPlatformPort(req.getPlatformPort());
        dto.setPlatformDomain(req.getPlatformDomain());
        dto.setUsername(req.getUsername());
        dto.setPassword(req.getPassword());
        dto.setLocalClientId(req.getLocalClientId());
        dto.setLocalIp(req.getLocalIp());
        dto.setLocalPort(req.getLocalPort());
        dto.setKeepaliveInterval(req.getKeepaliveInterval());
        dto.setRegisterExpires(req.getRegisterExpires());
        dto.setCharset(req.getCharset());
        dto.setTransport(req.getTransport());
        return dto;
    }

    /**
     * 平台分页查询请求 → 查询条件 DTO；入参为空返回空条件（查全量）。
     */
    public CascadePlatformDTO pageReqToQueryDto(CascadePlatformPageReq req) {
        CascadePlatformDTO dto = new CascadePlatformDTO();
        if (req == null) {
            return dto;
        }
        dto.setPlatformId(req.getPlatformId());
        dto.setPlatformIp(req.getPlatformIp());
        dto.setEnabled(req.getEnabled());
        dto.setRegisterStatus(req.getRegisterStatus());
        return dto;
    }

    // ==================== 通道 ====================

    /**
     * 通道新增请求 → DTO。
     */
    public CascadeChannelDTO toDTO(CascadeChannelCreateReq req) {
        if (req == null) {
            return null;
        }
        CascadeChannelDTO dto = new CascadeChannelDTO();
        dto.setPlatformId(req.getPlatformId());
        dto.setLocalDeviceId(req.getLocalDeviceId());
        dto.setLocalChannelId(req.getLocalChannelId());
        dto.setCascadeChannelId(req.getCascadeChannelId());
        dto.setCascadeName(req.getCascadeName());
        return dto;
    }

    /**
     * 通道更新请求 → DTO（id 透传，身份字段展示不参与更新键）。
     */
    public CascadeChannelDTO toDTO(CascadeChannelUpdateReq req) {
        if (req == null) {
            return null;
        }
        CascadeChannelDTO dto = new CascadeChannelDTO();
        dto.setId(req.getId());
        dto.setPlatformId(req.getPlatformId());
        dto.setLocalDeviceId(req.getLocalDeviceId());
        dto.setLocalChannelId(req.getLocalChannelId());
        dto.setCascadeChannelId(req.getCascadeChannelId());
        dto.setCascadeName(req.getCascadeName());
        dto.setEnabled(req.getEnabled());
        return dto;
    }

    /**
     * 通道分页查询请求 → 查询条件 DTO；入参为空返回空条件（查全量）。
     */
    public CascadeChannelDTO pageReqToQueryDto(CascadeChannelPageReq req) {
        CascadeChannelDTO dto = new CascadeChannelDTO();
        if (req == null) {
            return dto;
        }
        dto.setPlatformId(req.getPlatformId());
        dto.setLocalDeviceId(req.getLocalDeviceId());
        dto.setLocalChannelId(req.getLocalChannelId());
        dto.setCascadeChannelId(req.getCascadeChannelId());
        return dto;
    }

    /**
     * 通道批量绑定请求 → DTO 列表（platformId 下发到每条；cascadeChannelId 缺省由 Manager 兜底）。
     */
    public List<CascadeChannelDTO> toBatchDTOList(CascadeChannelBatchBindReq req) {
        List<CascadeChannelDTO> list = new ArrayList<>();
        if (req == null || req.getChannels() == null) {
            return list;
        }
        for (CascadeChannelBatchBindReq.BindItem item : req.getChannels()) {
            if (item == null) {
                continue;
            }
            CascadeChannelDTO dto = new CascadeChannelDTO();
            dto.setPlatformId(req.getPlatformId());
            dto.setLocalDeviceId(item.getLocalDeviceId());
            dto.setLocalChannelId(item.getLocalChannelId());
            dto.setCascadeChannelId(item.getCascadeChannelId());
            dto.setCascadeName(item.getCascadeName());
            list.add(dto);
        }
        return list;
    }
}
