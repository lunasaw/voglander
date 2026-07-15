package io.github.lunasaw.voglander.manager.assembler;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskDTO;
import io.github.lunasaw.voglander.repository.entity.BizTaskDO;

/** FastJSON2 boundary conversion for durable tasks. */
@Component
public class BizTaskAssembler {
    public BizTaskDTO doToDto(BizTaskDO source) {
        return convert(source, BizTaskDTO.class);
    }

    public BizTaskDTO doToSafeDto(BizTaskDO source) {
        BizTaskDTO target = doToDto(source);
        if (target != null) {
            target.setPayload(null);
            target.setResultSummary(BusinessTaskDataSanitizer.sanitizeJson(target.getResultSummary()));
        }
        return target;
    }

    public BizTaskDO dtoToDo(BizTaskDTO source) {
        return convert(source, BizTaskDO.class);
    }

    private <T> T convert(Object source, Class<T> targetType) {
        return source == null ? null : JSON.parseObject(JSON.toJSONString(source), targetType);
    }
}
