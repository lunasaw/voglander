package io.github.lunasaw.voglander.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;

@Mapper
public interface ImageAssetSourceMapper extends BaseMapper<ImageAssetSourceDO> {
    int insertIfAbsent(@Param("source") ImageAssetSourceDO source);
    ImageAssetSourceDO selectByAssetId(@Param("assetId") String assetId);
    ImageAssetSourceDO selectByTaskId(@Param("taskId") String taskId);
    ImageAssetSourceDO selectByExecutionId(@Param("executionId") String executionId);
}
