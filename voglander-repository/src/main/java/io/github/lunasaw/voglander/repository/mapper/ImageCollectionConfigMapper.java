package io.github.lunasaw.voglander.repository.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;

@Mapper
public interface ImageCollectionConfigMapper extends BaseMapper<ImageCollectionConfigDO> {
    int insertIfAbsent(@Param("config") ImageCollectionConfigDO config);
    ImageCollectionConfigDO selectByTaskId(@Param("taskId") String taskId);
    List<ImageCollectionConfigDO> selectByCamera(@Param("deviceId") String deviceId,
        @Param("channelId") String channelId);
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<ImageCollectionConfigDO> selectPageByCamera(
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ImageCollectionConfigDO> page,
        @Param("deviceId") String deviceId, @Param("channelId") String channelId);
}
