package io.github.lunasaw.voglander.manager.assembler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;

/**
 * MediaSessionAssembler 纯单元测试（无 Spring）
 *
 * @author luna
 */
@DisplayName("MediaSessionAssembler 双向幂等���试")
@ExtendWith(MockitoExtension.class)
class MediaSessionAssemblerTest {

    private final MediaSessionAssembler assembler = new MediaSessionAssembler();

    private MediaSessionDTO buildDto() {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setCallId("call-123");
        dto.setDeviceId("dev-001");
        dto.setChannelId("ch-001");
        dto.setSsrc("ssrc-1");
        dto.setStream("stream-1");
        dto.setStatus(1);
        dto.setSessionType("live");
        dto.setExtend("{\"key\":\"val\"}");
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        return dto;
    }

    @Test
    @DisplayName("dtoToDo 后字段应一一对应")
    void should_map_all_fields_dto_to_do() {
        MediaSessionDTO dto = buildDto();
        MediaSessionDO doo = assembler.dtoToDo(dto);
        assertNotNull(doo);
        assertEquals(dto.getCallId(), doo.getCallId());
        assertEquals(dto.getDeviceId(), doo.getDeviceId());
        assertEquals(dto.getStatus(), doo.getStatus());
        assertEquals(dto.getExtend(), doo.getExtend());
    }

    @Test
    @DisplayName("doToDto 后字段应一一对应")
    void should_map_all_fields_do_to_dto() {
        MediaSessionDO doo = new MediaSessionDO();
        doo.setId(1L);
        doo.setCallId("call-123");
        doo.setDeviceId("dev-001");
        doo.setStatus(2);
        doo.setExtend("{\"a\":1}");

        MediaSessionDTO dto = assembler.doToDto(doo);
        assertNotNull(dto);
        assertEquals(doo.getCallId(), dto.getCallId());
        assertEquals(doo.getStatus(), dto.getStatus());
        assertEquals(doo.getId(), dto.getId());
    }

    @Test
    @DisplayName("doToDto(null) 应返回 null，不抛 NPE")
    void should_return_null_when_do_is_null() {
        assertNull(assembler.doToDto(null));
    }

    @Test
    @DisplayName("dtoToDo(null) 应返回 null，不抛 NPE")
    void should_return_null_when_dto_is_null() {
        assertNull(assembler.dtoToDo(null));
    }

    @Test
    @DisplayName("doListToDtoList 空集合应返回空集合，非 null")
    void should_return_empty_list_for_empty_input() {
        List<MediaSessionDTO> result = assembler.doListToDtoList(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
