package io.github.lunasaw.voglander.manager.assembler;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskExecutionDTO;
import io.github.lunasaw.voglander.repository.entity.BizTaskExecutionDO;

/** FastJSON2 boundary conversion for durable execution facts. */
@Component
public class BizTaskExecutionAssembler {
    public BizTaskExecutionDTO doToDto(BizTaskExecutionDO source) {
        return convert(source, BizTaskExecutionDTO.class);
    }

    public BizTaskExecutionDTO doToSafeDto(BizTaskExecutionDO source) {
        BizTaskExecutionDTO target = doToDto(source);
        if (target != null) {
            target.setClaimToken(null);
            target.setResultSummary(BusinessTaskDataSanitizer.sanitizeJson(target.getResultSummary()));
        }
        return target;
    }

    public BizTaskExecutionDO dtoToDo(BizTaskExecutionDTO source) {
        return convert(source, BizTaskExecutionDO.class);
    }

    private <T> T convert(Object source, Class<T> targetType) {
        return source == null ? null : JSON.parseObject(JSON.toJSONString(source), targetType);
    }
}
