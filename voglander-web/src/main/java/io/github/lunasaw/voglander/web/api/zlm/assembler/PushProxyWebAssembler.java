package io.github.lunasaw.voglander.web.api.zlm.assembler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyVO;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * 推流代理Web层装配器
 * 负责Web层对象与业务层对象的转换，支持ZLM参数扩展模型自动映射
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class PushProxyWebAssembler {

    /**
     * 创建请求转DTO（支持ZLM参数扩展模型映射）
     * <p>
     * 将平铺的ZLM参数自动映射到扩展模型中，实现入参简化和扩展存储的完美结合
     * </p>
     *
     * @param createReq 创建请求
     * @return DTO对象
     */
    public PushProxyDTO createReqToDto(PushProxyCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        PushProxyDTO dto = new PushProxyDTO();

        // 设置核心业务字段
        dto.setApp(createReq.getApp());
        dto.setStream(createReq.getStream());
        dto.setDstUrl(createReq.getDstUrl());
        dto.setSchema(createReq.getSchema());
        dto.setDescription(createReq.getDescription());
        dto.setStatus(createReq.getStatus());
        dto.setServerId(createReq.getServerId());

        // 构建ZLM扩展参数模型
        if (createReq.getPushProxyExtendReq() != null) {
            PushProxyDTO.ExtendObj extendObj = JSON.parseObject(JSON.toJSONString(createReq.getPushProxyExtendReq()), PushProxyDTO.ExtendObj.class);
            dto.setExtendObj(extendObj);
            dto.setExtend(JSON.toJSONString(dto.getExtendObj()));
        }

        return dto;
    }

    /**
     * 更新请求转DTO
     *
     * @param updateReq 更新请求
     * @return DTO对象
     */
    public PushProxyDTO updateReqToDto(PushProxyUpdateReq updateReq) {
        if (updateReq == null) {
            return null;
        }

        PushProxyDTO dto = new PushProxyDTO();
        dto.setId(updateReq.getId());
        dto.setApp(updateReq.getApp());
        dto.setStream(updateReq.getStream());
        dto.setDstUrl(updateReq.getDstUrl());
        dto.setSchema(updateReq.getSchema());
        dto.setDescription(updateReq.getDescription());
        dto.setStatus(updateReq.getStatus());
        dto.setServerId(updateReq.getServerId());

        // 构建ZLM扩展参数模型
        if (updateReq.getPushProxyExtendReq() != null) {
            PushProxyDTO.ExtendObj extendObj = JSON.parseObject(JSON.toJSONString(updateReq.getPushProxyExtendReq()), PushProxyDTO.ExtendObj.class,
                JSONReader.Feature.SupportSmartMatch);
            dto.setExtendObj(extendObj);
            dto.setExtend(JSON.toJSONString(dto.getExtendObj()));
        }

        return dto;
    }

    /**
     * 查询请求转DTO
     *
     * @param queryReq 查询请求
     * @return DTO对象
     */
    public PushProxyDTO queryReqToDto(PushProxyQueryReq queryReq) {
        if (queryReq == null) {
            return null;
        }

        PushProxyDTO dto = new PushProxyDTO();
        dto.setId(queryReq.getId());
        dto.setApp(queryReq.getApp());
        dto.setStream(queryReq.getStream());
        dto.setDstUrl(queryReq.getDstUrl());
        dto.setSchema(queryReq.getSchema());
        dto.setStatus(queryReq.getStatus());
        dto.setOnlineStatus(queryReq.getOnlineStatus());
        dto.setProxyKey(queryReq.getProxyKey());
        dto.setServerId(queryReq.getServerId());
        dto.setEnabled(queryReq.getEnabled());
        dto.setDescription(queryReq.getDescription());

        return dto;
    }

    /**
     * DTO转视图对象（支持时间戳转换和扩展模型映射）
     * <p>
     * 自动处理时间字段到Unix时间戳的转换，并映射扩展模型到视图对象
     * </p>
     *
     * @param dto DTO对象
     * @return 视图对象
     */
    public PushProxyVO dtoToVo(PushProxyDTO dto) {
        if (dto == null) {
            return null;
        }

        PushProxyVO vo = new PushProxyVO();
        vo.setId(dto.getId());
        vo.setApp(dto.getApp());
        vo.setStream(dto.getStream());
        vo.setDstUrl(dto.getDstUrl());
        vo.setSchema(dto.getSchema());
        vo.setStatus(dto.getStatus());
        vo.setOnlineStatus(dto.getOnlineStatus());
        vo.setProxyKey(dto.getProxyKey());
        vo.setServerId(dto.getServerId());
        vo.setEnabled(dto.getEnabled());
        vo.setDescription(dto.getDescription());
        vo.setExtend(dto.getExtend());

        // 时间字段转换为Unix时间戳(毫秒)
        if (dto.getCreateTime() != null) {
            vo.setCreateTime(dto.getCreateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        if (dto.getUpdateTime() != null) {
            vo.setUpdateTime(dto.getUpdateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }

        // 扩展模型映射
        if (dto.getExtendObj() != null) {
            PushProxyVO.PushProxyExtendVO extendVO = JSON.parseObject(JSON.toJSONString(dto.getExtendObj()), PushProxyVO.PushProxyExtendVO.class);
            vo.setExtendObj(extendVO);
        }

        return vo;
    }
}