package io.github.lunasaw.voglander.manager.assembler;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;

import io.github.lunasaw.voglander.manager.domaon.dto.task.BizTaskEventDTO;
import io.github.lunasaw.voglander.repository.entity.BizTaskEventDO;

/** FastJSON2 boundary conversion for append-only task events. */
@Component
public class BizTaskEventAssembler {
    public BizTaskEventDTO doToDto(BizTaskEventDO source) {
        return convert(source, BizTaskEventDTO.class);
    }

    public BizTaskEventDTO doToSafeDto(BizTaskEventDO source) {
        BizTaskEventDTO target = doToDto(source);
        if (target != null) {
            target.setEventData(BusinessTaskDataSanitizer.sanitizeJson(target.getEventData()));
        }
        return target;
    }

    public BizTaskEventDO dtoToDo(BizTaskEventDTO source) {
        return convert(source, BizTaskEventDO.class);
    }

    public BizTaskEventDO dtoToSafeDo(BizTaskEventDTO source) {
        BizTaskEventDO target = dtoToDo(source);
        if (target != null) {
            target.setEventData(BusinessTaskDataSanitizer.sanitizeJson(target.getEventData()));
        }
        return target;
    }

    private <T> T convert(Object source, Class<T> targetType) {
        return source == null ? null : JSON.parseObject(JSON.toJSONString(source), targetType);
    }
}
