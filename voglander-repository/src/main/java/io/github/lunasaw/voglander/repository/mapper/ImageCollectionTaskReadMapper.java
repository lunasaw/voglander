package io.github.lunasaw.voglander.repository.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.repository.domain.image.ImageCollectionTaskQueryCondition;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionTaskDO;

@Mapper
public interface ImageCollectionTaskReadMapper {
    Page<ImageCollectionTaskDO> selectPageByCondition(Page<ImageCollectionTaskDO> page,
        @Param("condition") ImageCollectionTaskQueryCondition condition);

    ImageCollectionTaskDO selectDetailByCondition(
        @Param("condition") ImageCollectionTaskQueryCondition condition);
}
