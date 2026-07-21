package io.github.lunasaw.voglander.repository.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;

@Mapper
public interface ImageAssetMapper extends BaseMapper<ImageAssetDO> {
    int insertIfAbsent(@Param("asset") ImageAssetDO asset);
    ImageAssetDO selectByAssetId(@Param("assetId") String assetId);
    ImageAssetDO selectByIdempotency(@Param("ownerType") String ownerType, @Param("ownerId") String ownerId,
        @Param("idempotencyKey") String idempotencyKey);
    Page<ImageAssetDO> selectPageByCondition(Page<ImageAssetDO> page, @Param("ownerType") String ownerType,
        @Param("ownerId") String ownerId, @Param("assetId") String assetId, @Param("assetName") String assetName,
        @Param("status") String status, @Param("sourceType") String sourceType,
        @Param("sourceTaskId") String sourceTaskId, @Param("sourceExecutionId") String sourceExecutionId,
        @Param("deviceId") String deviceId, @Param("channelId") String channelId,
        @Param("capturedStart") LocalDateTime capturedStart, @Param("capturedEnd") LocalDateTime capturedEnd);
    long countVisible(@Param("ownerType") String ownerType, @Param("ownerId") String ownerId,
        @Param("status") String status, @Param("createdAfter") LocalDateTime createdAfter);
    int markDeleting(@Param("assetId") String assetId, @Param("version") int version,
        @Param("updateTime") LocalDateTime updateTime);
    int markDeleted(@Param("assetId") String assetId, @Param("version") int version,
        @Param("deletedAt") LocalDateTime deletedAt, @Param("updateTime") LocalDateTime updateTime);
    int markDeleteFailed(@Param("assetId") String assetId, @Param("version") int version,
        @Param("failureCode") String failureCode, @Param("failureMessage") String failureMessage,
        @Param("updateTime") LocalDateTime updateTime);
}
