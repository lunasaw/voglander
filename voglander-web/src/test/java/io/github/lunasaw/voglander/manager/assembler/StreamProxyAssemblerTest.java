package io.github.lunasaw.voglander.manager.assembler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;

/**
 * StreamProxyAssembler 双向幂等测试
 *
 * @author luna
 */
@DisplayName("StreamProxyAssembler 双向幂等测试")
class StreamProxyAssemblerTest {

    private final StreamProxyAssembler assembler = new StreamProxyAssembler();

    @Test
    @DisplayName("dtoToDo 后字段应一一对应")
    void should_map_all_fields_dto_to_do() {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp("live");
        dto.setStream("test");
        dto.setUrl("rtsp://x");
        dto.setStatus(1);
        dto.setOnlineStatus(0);
        dto.setProxyKey("pk");
        dto.setCreateTime(LocalDateTime.now());

        StreamProxyDO doo = assembler.dtoToDo(dto);
        assertNotNull(doo);
        assertEquals(dto.getApp(), doo.getApp());
        assertEquals(dto.getStream(), doo.getStream());
        assertEquals(dto.getUrl(), doo.getUrl());
        assertEquals(dto.getStatus(), doo.getStatus());
    }

    @Test
    @DisplayName("doToDto 后字段应一一对应")
    void should_map_all_fields_do_to_dto() {
        StreamProxyDO doo = new StreamProxyDO();
        doo.setId(1L);
        doo.setApp("live");
        doo.setStream("test");
        doo.setUrl("rtsp://x");
        doo.setStatus(1);

        StreamProxyDTO dto = assembler.doToDto(doo);
        assertNotNull(dto);
        assertEquals(doo.getApp(), dto.getApp());
        assertEquals(doo.getId(), dto.getId());
    }

    @Test
    @DisplayName("doListToDtoList 空集合应返回空集合非 null")
    void should_return_empty_list_for_empty_input() {
        List<StreamProxyDTO> result = assembler.doListToDtoList(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("doToDto(null) 应安全返回 null")
    void should_handle_null_do() {
        assertNull(assembler.doToDto(null));
    }
}
