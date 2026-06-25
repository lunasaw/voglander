package io.github.lunasaw.voglander.web.api.cascade;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.lunasaw.voglander.manager.domaon.dto.cascade.CascadeSubscribeDTO;
import io.github.lunasaw.voglander.manager.manager.CascadeSubscribeManager;

/**
 * CascadeSubscribeController 纯 Mockito + MockMvc 单元测试。
 *
 * @author luna
 */
@DisplayName("CascadeSubscribeController 单元测试")
@ExtendWith(MockitoExtension.class)
class CascadeSubscribeControllerTest {

    @InjectMocks
    CascadeSubscribeController controller;
    @Mock
    CascadeSubscribeManager   cascadeSubscribeManager;

    MockMvc                   mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("按平台查询：委托 manager.listActiveByPlatform 返回 VO 列表（subType 派生名）")
    void list_ok() throws Exception {
        CascadeSubscribeDTO dto = new CascadeSubscribeDTO();
        dto.setId(1L);
        dto.setPlatformId("p1");
        dto.setSubType("CATALOG");
        dto.setStatus(1);
        when(cascadeSubscribeManager.listActiveByPlatform("p1")).thenReturn(List.of(dto));

        mvc.perform(get("/api/v1/cascade/subscribe/list").param("platformId", "p1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].subType").value("CATALOG"))
            .andExpect(jsonPath("$.data[0].subTypeName").value("目录订阅"))
            .andExpect(jsonPath("$.data[0].statusName").value("活跃"));
        verify(cascadeSubscribeManager).listActiveByPlatform("p1");
    }

    @Test
    @DisplayName("无订阅：返回空数组")
    void list_empty() throws Exception {
        when(cascadeSubscribeManager.listActiveByPlatform("p2")).thenReturn(List.of());
        mvc.perform(get("/api/v1/cascade/subscribe/list").param("platformId", "p2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
        verify(cascadeSubscribeManager).listActiveByPlatform("p2");
    }
}
