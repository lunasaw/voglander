package io.github.lunasaw.voglander.web.api.image.assembler;

import java.time.ZoneId;

import org.springframework.stereotype.Component;

import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetQueryDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetSourceDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.image.ImageAssetStatisticsDTO;
import io.github.lunasaw.voglander.web.api.image.req.ImageAssetQueryReq;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetStatisticsVO;
import io.github.lunasaw.voglander.web.api.image.vo.ImageAssetVO;

@Component
public class ImageAssetWebAssembler {
    public ImageAssetQueryDTO toQuery(ImageAssetQueryReq req) {
        ImageAssetQueryDTO q = new ImageAssetQueryDTO();
        if (req == null) return q;
        q.setAssetId(req.getAssetId()); q.setAssetName(req.getAssetName()); q.setStatus(req.getStatus());
        q.setSourceType(req.getSourceType()); q.setSourceTaskId(req.getSourceTaskId());
        q.setSourceExecutionId(req.getSourceExecutionId()); q.setDeviceId(req.getDeviceId()); q.setChannelId(req.getChannelId());
        q.setCapturedStart(req.capturedStartTime()); q.setCapturedEnd(req.capturedEndTime());
        return q;
    }

    public ImageAssetVO toVO(ImageAssetDTO asset, ImageAssetSourceDTO source) {
        if (asset == null) return null;
        ImageAssetVO vo = new ImageAssetVO();
        vo.setAssetId(asset.getAssetId()); vo.setAssetName(asset.getAssetName()); vo.setStatus(asset.getStatus());
        vo.setContentType(asset.getContentType()); vo.setImageFormat(asset.getImageFormat()); vo.setFileSize(asset.getFileSize());
        vo.setWidth(asset.getWidth()); vo.setHeight(asset.getHeight()); vo.setChecksum(asset.getChecksum());
        vo.setCapturedAt(epoch(asset.getCapturedAt())); vo.setIngestedAt(epoch(asset.getIngestedAt()));
        if (source != null) {
            vo.setSourceType(source.getSourceType()); vo.setSourceTaskId(source.getSourceTaskId());
            vo.setSourceExecutionId(source.getSourceExecutionId()); vo.setSourceEntityId(source.getSourceEntityId());
            vo.setOriginalFilename(source.getOriginalFilename());
        }
        return vo;
    }

    public ImageAssetStatisticsVO toVO(ImageAssetStatisticsDTO stats) {
        ImageAssetStatisticsVO vo = new ImageAssetStatisticsVO();
        if (stats != null) { vo.setTotal(stats.getTotal()); vo.setAvailable(stats.getAvailable()); vo.setToday(stats.getToday()); vo.setDeleteFailed(stats.getDeleteFailed()); }
        return vo;
    }

    private static Long epoch(java.time.LocalDateTime time) { return time == null ? null : time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(); }
}
