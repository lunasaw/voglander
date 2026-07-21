package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.ImageAssetService;
import io.github.lunasaw.voglander.repository.entity.ImageAssetDO;
import io.github.lunasaw.voglander.repository.mapper.ImageAssetMapper;

@Service("imageAssetService")
public class ImageAssetServiceImpl extends ServiceImpl<ImageAssetMapper, ImageAssetDO> implements ImageAssetService {
}
