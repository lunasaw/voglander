package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.MediaNodeService;
import io.github.lunasaw.voglander.repository.mapper.MediaNodeMapper;
import io.github.lunasaw.voglander.repository.entity.MediaNodeDO;
import org.springframework.stereotype.Service;

/**
 * 流媒体节点管理表服务实现类
 *
 * @author luna
 * @since 2025-01-23
 */
@Service("mediaNodeService")
public class MediaNodeServiceImpl extends ServiceImpl<MediaNodeMapper, MediaNodeDO> implements MediaNodeService {

}