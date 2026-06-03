package io.github.lunasaw.voglander.manager.assembler;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadePlatformDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.repository.entity.CascadePlatformDO;
import io.github.lunasaw.voglander.repository.entity.CascadeChannelDO;

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
}
