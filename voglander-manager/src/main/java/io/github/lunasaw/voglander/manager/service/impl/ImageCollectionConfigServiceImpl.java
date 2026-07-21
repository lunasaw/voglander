package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.ImageCollectionConfigService;
import io.github.lunasaw.voglander.repository.entity.ImageCollectionConfigDO;
import io.github.lunasaw.voglander.repository.mapper.ImageCollectionConfigMapper;

@Service("imageCollectionConfigService")
public class ImageCollectionConfigServiceImpl extends ServiceImpl<ImageCollectionConfigMapper, ImageCollectionConfigDO>
    implements ImageCollectionConfigService {
}
