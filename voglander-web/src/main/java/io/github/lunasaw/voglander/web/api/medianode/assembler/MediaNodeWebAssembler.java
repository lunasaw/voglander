package io.github.lunasaw.voglander.web.api.medianode.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.web.api.medianode.req.MediaNodeCreateReq;
import io.github.lunasaw.voglander.web.api.medianode.req.MediaNodeUpdateReq;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流媒体节点Web层转换器
 * 负责处理 Web 层 Req -> DTO 的转换
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class MediaNodeWebAssembler {

    /**
     * MediaNodeCreateReq -> MediaNodeDTO
     *
     * @param createReq 创建请求
     * @return MediaNodeDTO
     */
    public MediaNodeDTO toMediaNodeDTO(MediaNodeCreateReq createReq) {
        if (createReq == null) {
            return null;
        }
        MediaNodeDTO dto = new MediaNodeDTO();
        dto.setServerId(createReq.getServerId());
        dto.setName(createReq.getName());
        dto.setHost(createReq.getHost());
        dto.setSecret(createReq.getSecret());
        dto.setEnabled(createReq.getEnabled());
        dto.setHookEnabled(createReq.getHookEnabled());
        dto.setWeight(createReq.getWeight());
        dto.setDescription(createReq.getDescription());
        dto.setExtend(createReq.getExtend());
        return dto;
    }

    /**
     * MediaNodeUpdateReq -> MediaNodeDTO
     *
     * @param updateReq 更新请求
     * @return MediaNodeDTO
     */
    public MediaNodeDTO toMediaNodeDTO(MediaNodeUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }
        MediaNodeDTO dto = new MediaNodeDTO();
        dto.setId(updateReq.getId());
        dto.setServerId(updateReq.getServerId());
        dto.setName(updateReq.getName());
        dto.setHost(updateReq.getHost());
        dto.setSecret(updateReq.getSecret());
        dto.setEnabled(updateReq.getEnabled());
        dto.setHookEnabled(updateReq.getHookEnabled());
        dto.setWeight(updateReq.getWeight());
        dto.setDescription(updateReq.getDescription());
        dto.setExtend(updateReq.getExtend());
        return dto;
    }

    /**
     * 批量转换 MediaNodeCreateReq -> MediaNodeDTO
     *
     * @param createReqList 创建请求列表
     * @return MediaNodeDTO列表
     */
    public List<MediaNodeDTO> toMediaNodeDTOList(List<MediaNodeCreateReq> createReqList) {
        if (createReqList == null) {
            return null;
        }
        return createReqList.stream()
                .map(this::toMediaNodeDTO)
                .collect(Collectors.toList());
    }

    /**
     * 批量转换 MediaNodeUpdateReq -> MediaNodeDTO
     *
     * @param updateReqList 更新请求列表
     * @return MediaNodeDTO列表
     */
    public List<MediaNodeDTO> toUpdateMediaNodeDTOList(List<MediaNodeUpdateReq> updateReqList) {
        if (updateReqList == null) {
            return null;
        }
        return updateReqList.stream()
                .map(this::toMediaNodeDTO)
                .collect(Collectors.toList());
    }
}