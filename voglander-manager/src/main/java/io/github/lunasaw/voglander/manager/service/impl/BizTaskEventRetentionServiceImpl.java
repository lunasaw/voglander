package io.github.lunasaw.voglander.manager.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.github.lunasaw.voglander.manager.service.BizTaskEventRetentionService;
import io.github.lunasaw.voglander.repository.mapper.BizTaskEventMapper;
import lombok.RequiredArgsConstructor;

/** Executes the retention delete in the repository layer; it is never exposed as a Web API. */
@Service
@RequiredArgsConstructor
public class BizTaskEventRetentionServiceImpl implements BizTaskEventRetentionService {

    private final BizTaskEventMapper eventMapper;

    @Override
    public int deleteBefore(LocalDateTime cutoff, int batchSize) {
        Assert.notNull(cutoff, "事件保留截止时间不能为空");
        Assert.isTrue(batchSize > 0 && batchSize <= 100_000, "事件清理批大小必须在1-100000之间");
        List<Long> ids = eventMapper.selectIdsBefore(cutoff, batchSize);
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return eventMapper.deleteByIds(ids);
    }
}
