package io.github.lunasaw.voglander.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.lunasaw.voglander.manager.service.CascadeRecordRequestService;
import io.github.lunasaw.voglander.repository.entity.CascadeRecordRequestDO;
import io.github.lunasaw.voglander.repository.mapper.CascadeRecordRequestMapper;
import org.springframework.stereotype.Service;

@Service
public class CascadeRecordRequestServiceImpl extends ServiceImpl<CascadeRecordRequestMapper, CascadeRecordRequestDO>
    implements CascadeRecordRequestService {
}
