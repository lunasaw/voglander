package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.ImageAssetSourceService;
import io.github.lunasaw.voglander.repository.entity.ImageAssetSourceDO;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetSourceMapper;

@Service("imageAssetSourceService")
public class ImageAssetSourceServiceImpl extends ServiceImpl<ImageAssetSourceMapper, ImageAssetSourceDO>
    implements ImageAssetSourceService {
}
