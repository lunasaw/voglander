package io.github.lunasaw.voglander.web.api.zlm.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
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
        dto.setEnabled(createReq.getEnabled());

        // 构建ZLM扩展参数模型
        Map<String, Object> zlmExtension = buildZlmExtensionModel(createReq);

        // 将扩展模型序列化为JSON字符串存储
        if (!zlmExtension.isEmpty()) {
            dto.setExtend(JSON.toJSONString(zlmExtension));
        }

        return dto;
    }

    /**
     * 构建ZLM扩展参数模型
     * <p>
     * 根据字段命名自动将请求参数映射到扩展模型中，支持所有ZLM /proxy/add API参数
     * </p>
     *
     * @param createReq 创建请求
     * @return ZLM扩展参数Map
     */
    private Map<String, Object> buildZlmExtensionModel(StreamProxyCreateReq createReq) {
        Map<String, Object> zlmParams = new HashMap<>();

        // ZLM 核心配置参数
        addIfNotNull(zlmParams, "vhost", createReq.getVhost());
        addIfNotNull(zlmParams, "retry_count", createReq.getRetryCount());
        addIfNotNull(zlmParams, "rtp_type", createReq.getRtpType());
        addIfNotNull(zlmParams, "timeout_sec", createReq.getTimeoutSec());

        // 协议转换启用开关
        addIfNotNull(zlmParams, "enable_hls", createReq.getEnableHls());
        addIfNotNull(zlmParams, "enable_hls_fmp4", createReq.getEnableHlsFmp4());
        addIfNotNull(zlmParams, "enable_mp4", createReq.getEnableMp4());
        addIfNotNull(zlmParams, "enable_rtsp", createReq.getEnableRtsp());
        addIfNotNull(zlmParams, "enable_rtmp", createReq.getEnableRtmp());
        addIfNotNull(zlmParams, "enable_ts", createReq.getEnableTs());
        addIfNotNull(zlmParams, "enable_fmp4", createReq.getEnableFmp4());

        // 按需生成控制参数
        addIfNotNull(zlmParams, "hls_demand", createReq.getHlsDemand());
        addIfNotNull(zlmParams, "rtsp_demand", createReq.getRtspDemand());
        addIfNotNull(zlmParams, "rtmp_demand", createReq.getRtmpDemand());
        addIfNotNull(zlmParams, "ts_demand", createReq.getTsDemand());
        addIfNotNull(zlmParams, "fmp4_demand", createReq.getFmp4Demand());

        // 音频处理参数
        addIfNotNull(zlmParams, "enable_audio", createReq.getEnableAudio());
        addIfNotNull(zlmParams, "add_mute_audio", createReq.getAddMuteAudio());

        // MP4录制配置参数
        addIfNotNull(zlmParams, "mp4_save_path", createReq.getMp4SavePath());
        addIfNotNull(zlmParams, "mp4_max_second", createReq.getMp4MaxSecond());
        addIfNotNull(zlmParams, "mp4_as_player", createReq.getMp4AsPlayer());

        // HLS录制配置参数
        addIfNotNull(zlmParams, "hls_save_path", createReq.getHlsSavePath());

        // 高级配置参数
        addIfNotNull(zlmParams, "modify_stamp", createReq.getModifyStamp());
        addIfNotNull(zlmParams, "auto_close", createReq.getAutoClose());

        return zlmParams;
    }

    /**
     * 辅助方法：非空值才添加到Map中
     */
    private void addIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
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
        dto.setApp(updateReq.getApp());
        dto.setStream(updateReq.getStream());
        dto.setUrl(updateReq.getUrl());
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