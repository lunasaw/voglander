package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeRecordRequestDTO;
import io.github.lunasaw.voglander.repository.entity.CascadePlatformDO;
import io.github.lunasaw.voglander.repository.entity.CascadeChannelDO;
import io.github.lunasaw.voglander.repository.entity.CascadeSubscribeDO;
import io.github.lunasaw.voglander.repository.entity.CascadeRecordRequestDO;

public class CascadeAssembler {

    public static CascadePlatformDTO toDTO(CascadePlatformDO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadePlatformDTO.class);
    }

    public static CascadePlatformDO toDO(CascadePlatformDTO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadePlatformDO.class);
    }

    public static CascadeChannelDTO toDTO(CascadeChannelDO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeChannelDTO.class);
    }

    public static CascadeChannelDO toDO(CascadeChannelDTO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeChannelDO.class);
    }

    public static CascadeSubscribeDTO toDTO(CascadeSubscribeDO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeSubscribeDTO.class);
    }

    public static CascadeSubscribeDO toDO(CascadeSubscribeDTO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeSubscribeDO.class);
    }

    public static CascadeRecordRequestDTO toDTO(CascadeRecordRequestDO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeRecordRequestDTO.class);
    }

    public static CascadeRecordRequestDO toDO(CascadeRecordRequestDTO src) {
        if (src == null) return null;
        return JSON.parseObject(JSON.toJSONString(src), CascadeRecordRequestDO.class);
    }
}
