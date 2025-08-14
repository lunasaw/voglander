package io.github.lunasaw.voglander.web.api.zlm.assembler;

import com.alibaba.fastjson2.JSONReader;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * 拉流代理Web层装配器
 * 负责Web层对象与业务层对象的转换，支持ZLM参数扩展模型自动映射
 *
 * @author luna
 * @since 2025-01-23
 */
@Component
public class StreamProxyWebAssembler {

    /**
     * 创建请求转DTO（支持ZLM参数扩展模型映射）
     * <p>
     * 将平铺的ZLM参数自动映射到扩展模型中，实现入参简化和扩展存储的完美结合
     * </p>
     *
     * @param createReq 创建请求
     * @return DTO对象
     */
    public StreamProxyDTO createReqToDto(StreamProxyCreateReq createReq) {
        if (createReq == null) {
            return null;
        }

        StreamProxyDTO dto = new StreamProxyDTO();

        // 设置核心业务字段
        dto.setApp(createReq.getApp());
        dto.setStream(createReq.getStream());
        dto.setUrl(createReq.getUrl());
        dto.setDescription(createReq.getDescription());
        dto.setStatus(createReq.getStatus());
        dto.setServerId(createReq.getServerId());
        // 构建ZLM扩展参数模型
        StreamProxyDTO.ExtendObj extendObj = JSON.parseObject(JSON.toJSONString(createReq.getStreamProxyExtendReq()), StreamProxyDTO.ExtendObj.class);
        dto.setExtendObj(extendObj);
        dto.setExtend(JSON.toJSONString(dto.getExtendObj()));

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
        dto.setId(updateReq.getId());
        dto.setApp(updateReq.getApp());
        dto.setStream(updateReq.getStream());
        dto.setUrl(updateReq.getUrl());
        dto.setDescription(updateReq.getDescription());
        dto.setStatus(updateReq.getStatus());
        dto.setServerId(updateReq.getServerId());
        StreamProxyDTO.ExtendObj extendObj = JSON.parseObject(JSON.toJSONString(updateReq.getStreamProxyExtendReq()), StreamProxyDTO.ExtendObj.class,
            JSONReader.Feature.SupportSmartMatch);
        dto.setExtendObj(extendObj);
        dto.setExtend(JSON.toJSONString(dto.getExtendObj()));
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
        vo.setServerId(dto.getServerId());
        vo.setDescription(dto.getDescription());
        vo.setExtend(dto.getExtend());
        vo.setExtendObj(dto.getExtendObj());

        // 时间转换为时间戳
        if (dto.getCreateTime() != null) {
            vo.setCreateTime(dto.getCreateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }
        if (dto.getUpdateTime() != null) {
            vo.setUpdateTime(dto.getUpdateTime().toInstant(ZoneOffset.of("+8")).toEpochMilli());
        }

        return vo;
    }

    /**
     * 查询请求转DTO
     * <p>
     * 将查询请求对象转换为DTO，支持灵活的查询条件组合
     * </p>
     *
     * @param queryReq 查询请求
     * @return DTO对象
     */
    public StreamProxyDTO queryReqToDto(StreamProxyQueryReq queryReq) {
        if (queryReq == null) {
            return null;
        }

        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setId(queryReq.getId());
        dto.setApp(queryReq.getApp());
        dto.setStream(queryReq.getStream());
        dto.setProxyKey(queryReq.getProxyKey());
        dto.setUrl(queryReq.getUrl());
        dto.setDescription(queryReq.getDescription());
        dto.setStatus(queryReq.getStatus());
        dto.setOnlineStatus(queryReq.getOnlineStatus());
        dto.setServerId(queryReq.getServerId());

        return dto;
    }
}