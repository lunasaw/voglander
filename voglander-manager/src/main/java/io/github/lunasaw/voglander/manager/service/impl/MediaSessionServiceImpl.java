package io.github.lunasaw.voglander.manager.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import io.github.lunasaw.voglander.manager.service.MediaSessionService;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import io.github.lunasaw.voglander.repository.mapper.MediaSessionMapper;

/**
 * 媒体会话服务实现
 *
 * @author luna
 * @since 2025-05-29
 */
@Service
public class MediaSessionServiceImpl extends ServiceImpl<MediaSessionMapper, MediaSessionDO> implements MediaSessionService {

}
