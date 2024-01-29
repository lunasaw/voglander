package io.github.lunasaw.voglander.manager.service.impl;

import io.github.lunasaw.voglander.manager.service.ExportTaskService;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import io.github.lunasaw.voglander.repository.mapper.ExportTaskMapper;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * (DeviceDO)表服务实现类
 *
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@Service("deviceService")
public class ExportTaskServiceImpl extends ServiceImpl<ExportTaskMapper, ExportTaskDO> implements ExportTaskService {

}
