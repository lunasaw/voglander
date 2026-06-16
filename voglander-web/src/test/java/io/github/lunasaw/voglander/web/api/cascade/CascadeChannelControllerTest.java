package io.github.lunasaw.voglander.web.api.cascade;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeChannelDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeChannelManager;
import io.github.lunasaw.voglander.web.api.cascade.assembler.CascadeWebAssembler;

/**
 * CascadeChannelController 纯 Mockito + MockMvc 单元测试。
 *
 * @author luna
 */
@DisplayName("CascadeChannelController 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeChannelControllerTest {

    @InjectMocks
    CascadeChannelController controller;
    @Mock
    CascadeChannelManager   cascadeChannelManager;
    @Spy
    CascadeWebAssembler     cascadeWebAssembler = new CascadeWebAssembler();

    MockMvc                 mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("新增：委托 manager.add 并返回 id")
    void add_ok() throws Exception {
        when(cascadeChannelManager.add(any(CascadeChannelDTO.class))).thenReturn(66L);
        mvc.perform(post("/api/v1/cascade/channel")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"platformId\":\"p1\",\"cascadeChannelId\":\"34020000001310000001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(66));
        verify(cascadeChannelManager).add(any(CascadeChannelDTO.class));
    }

    @Test
    @DisplayName("更新：回填路径 id 后委托 manager.update")
    void update_ok() throws Exception {
        when(cascadeChannelManager.update(any(CascadeChannelDTO.class))).thenReturn(true);
        mvc.perform(put("/api/v1/cascade/channel/7")
            .contentType(MediaType.APPLICATION_JSON).content("{\"cascadeName\":\"x\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        verify(cascadeChannelManager).update(argThat(d -> Long.valueOf(7L).equals(d.getId())));
    }

    @Test
    @DisplayName("删除：委托 manager.delete")
    void delete_ok() throws Exception {
        when(cascadeChannelManager.delete(7L)).thenReturn(true);
        mvc.perform(delete("/api/v1/cascade/channel/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(true));
        verify(cascadeChannelManager).delete(7L);
    }

    @Test
    @DisplayName("详情：委托 manager.getById 返回 VO")
    void getById_ok() throws Exception {
        CascadeChannelDTO dto = new CascadeChannelDTO();
        dto.setId(7L);
        when(cascadeChannelManager.getById(7L)).thenReturn(dto);
        mvc.perform(get("/api/v1/cascade/channel/7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(7));
        verify(cascadeChannelManager).getById(7L);
    }

    @Test
    @DisplayName("分页：POST /getPage 委托 manager.getPage 并透传 platformId 过滤")
    void page_ok() throws Exception {
        when(cascadeChannelManager.getPage(any(CascadeChannelDTO.class), eq(1), eq(20)))
            .thenReturn(new Page<>(1, 20));
        mvc.perform(post("/api/v1/cascade/channel/getPage")
            .param("page", "1").param("size", "20")
            .contentType(MediaType.APPLICATION_JSON).content("{\"platformId\":\"p1\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.total").value(0))
            .andExpect(jsonPath("$.data.items").isArray());
        verify(cascadeChannelManager).getPage(argThat(d -> "p1".equals(d.getPlatformId())), eq(1), eq(20));
    }

    @Test
    @DisplayName("批量绑定：委托 manager.batchBind 并返回新增条数")
    void batchBind_ok() throws Exception {
        when(cascadeChannelManager.batchBind(anyList())).thenReturn(2);
        mvc.perform(post("/api/v1/cascade/channel/batchBind")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"platformId\":\"p1\",\"channels\":["
                + "{\"localChannelId\":\"34020000001310000001\"},"
                + "{\"localChannelId\":\"34020000001310000002\"}]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value(2));
        verify(cascadeChannelManager).batchBind(argThat(list -> list.size() == 2
            && "p1".equals(list.get(0).getPlatformId())));
    }
}
