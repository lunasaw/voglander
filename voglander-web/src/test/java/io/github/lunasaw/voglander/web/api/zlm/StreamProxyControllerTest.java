package io.github.lunasaw.voglander.web.api.zlm;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import io.github.lunasaw.voglander.web.api.zlm.assembler.StreamProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyController最小化单元测试
 * 继承BaseMockTest实现Mock依赖隔离测试
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyControllerTest extends BaseTest {

    // 测试常量
    private final String            TEST_APP       = "live";
    private final String            TEST_STREAM    = "test";
    private final String            TEST_URL       = "rtmp://live.example.com/live/test";
    private final String            TEST_PROXY_KEY = "test-proxy-key-123";
    private final Long              TEST_ID        = 1L;

    @Autowired
    private MockMvc                 mockMvc;

    @Autowired
    private ObjectMapper            objectMapper;

    @MockitoBean
    private StreamProxyManager      streamProxyManager;

    @MockitoBean
    private StreamProxyWebAssembler streamProxyWebAssembler;

    // 测试数据对象
    private StreamProxyDO           testStreamProxyDO;
    private StreamProxyDTO          testStreamProxyDTO;
    private StreamProxyVO           testStreamProxyVO;
    private StreamProxyCreateReq    testCreateReq;
    private StreamProxyUpdateReq    testUpdateReq;

    @BeforeEach
    public void setUp() {
        testStreamProxyDO = createTestStreamProxyDO();
        testStreamProxyDTO = createTestStreamProxyDTO();
        testStreamProxyVO = createTestStreamProxyVO();
        testCreateReq = createTestCreateReq();
        testUpdateReq = createTestUpdateReq();
    }

    private StreamProxyDO createTestStreamProxyDO() {
        StreamProxyDO streamProxy = new StreamProxyDO();
        streamProxy.setId(TEST_ID);
        streamProxy.setApp(TEST_APP);
        streamProxy.setStream(TEST_STREAM);
        streamProxy.setUrl(TEST_URL);
        streamProxy.setProxyKey(TEST_PROXY_KEY);
        streamProxy.setStatus(1);
        streamProxy.setOnlineStatus(1);
        streamProxy.setEnabled(true);
        streamProxy.setDescription("Test proxy");
        streamProxy.setExtend("{\"vhost\":\"__defaultVhost__\"}");
        streamProxy.setCreateTime(LocalDateTime.now());
        streamProxy.setUpdateTime(LocalDateTime.now());
        return streamProxy;
    }

    private StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        dto.setEnabled(true);
        dto.setDescription("Test proxy");
        return dto;
    }

    private StreamProxyVO createTestStreamProxyVO() {
        StreamProxyVO vo = new StreamProxyVO();
        vo.setId(TEST_ID);
        vo.setApp(TEST_APP);
        vo.setStream(TEST_STREAM);
        vo.setUrl(TEST_URL);
        vo.setProxyKey(TEST_PROXY_KEY);
        vo.setStatus(1);
        vo.setOnlineStatus(1);
        vo.setEnabled(true);
        vo.setDescription("Test proxy");
        vo.setExtend("{\"vhost\":\"__defaultVhost__\"}");
        vo.setCreateTime(System.currentTimeMillis());
        vo.setUpdateTime(System.currentTimeMillis());
        return vo;
    }

    private StreamProxyCreateReq createTestCreateReq() {
        StreamProxyCreateReq req = new StreamProxyCreateReq();
        req.setApp(TEST_APP);
        req.setStream(TEST_STREAM);
        req.setUrl(TEST_URL);
        req.setEnabled(true);
        req.setDescription("Test proxy");
        return req;
    }

    private StreamProxyUpdateReq createTestUpdateReq() {
        StreamProxyUpdateReq req = new StreamProxyUpdateReq();
        req.setDescription("Test proxy updated");
        req.setStatus(1);
        req.setEnabled(true);
        req.setExtend("{\"vhost\":\"__defaultVhost__\"}");
        return req;
    }

    @Test
    public void testGetById_Success() throws Exception {
        // Arrange
        when(streamProxyManager.getById(TEST_ID)).thenReturn(testStreamProxyDO);
        when(streamProxyManager.doToDto(testStreamProxyDO)).thenReturn(testStreamProxyDTO);
        when(streamProxyWebAssembler.dtoToVo(testStreamProxyDTO)).thenReturn(testStreamProxyVO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/proxy/get/{id}", TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.data.id").value(TEST_ID))
            .andExpect(jsonPath("$.data.app").value(TEST_APP))
            .andExpect(jsonPath("$.data.stream").value(TEST_STREAM))
            .andExpect(jsonPath("$.data.proxyKey").value(TEST_PROXY_KEY));

        verify(streamProxyManager).getById(TEST_ID);
        verify(streamProxyManager).doToDto(testStreamProxyDO);
        verify(streamProxyWebAssembler).dtoToVo(testStreamProxyDTO);
    }

    @Test
    public void testGetById_NotFound() throws Exception {
        // Arrange
        when(streamProxyManager.getById(TEST_ID)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/v1/proxy/get/{id}", TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("代理不存在"));

        verify(streamProxyManager).getById(TEST_ID);
        verify(streamProxyManager, never()).doToDto(any());
        verify(streamProxyWebAssembler, never()).dtoToVo(any());
    }

    @Test
    public void testPageQuery_Success() throws Exception {
        // Arrange
        Page<StreamProxyDO> page = new Page<>(1, 10);
        page.setRecords(Lists.newArrayList(testStreamProxyDO));
        page.setTotal(1);

        when(streamProxyManager.getProxyPage(1, 10)).thenReturn(page);
        when(streamProxyManager.doToDto(testStreamProxyDO)).thenReturn(testStreamProxyDTO);
        when(streamProxyWebAssembler.dtoToVo(testStreamProxyDTO)).thenReturn(testStreamProxyVO);

        // Act & Assert
        mockMvc.perform(get("/api/v1/proxy/page")
            .param("page", "1")
            .param("size", "10")
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.data.items").isArray())
            .andExpect(jsonPath("$.data.items[0].id").value(TEST_ID))
            .andExpect(jsonPath("$.data.total").value(1));

        verify(streamProxyManager).getProxyPage(1, 10);
    }

    @Test
    public void testCreate_Success() throws Exception {
        // Arrange
        when(streamProxyWebAssembler.createReqToDto(any(StreamProxyCreateReq.class))).thenReturn(testStreamProxyDTO);
        when(streamProxyManager.createStreamProxy(testStreamProxyDTO)).thenReturn(TEST_ID);

        // Act & Assert
        mockMvc.perform(post("/api/v1/proxy/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testCreateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.data").value(TEST_ID));

        verify(streamProxyWebAssembler).createReqToDto(any(StreamProxyCreateReq.class));
        verify(streamProxyManager).createStreamProxy(testStreamProxyDTO);
    }

    @Test
    public void testUpdate_Success() throws Exception {
        // Arrange
        when(streamProxyManager.getById(TEST_ID)).thenReturn(testStreamProxyDO);
        when(streamProxyManager.updateStreamProxy(any(StreamProxyDO.class), anyString())).thenReturn(TEST_ID);

        // Act & Assert
        mockMvc.perform(put("/api/v1/proxy/update/{id}", TEST_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testUpdateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("操作成功"))
            .andExpect(jsonPath("$.data").value(TEST_ID));

        verify(streamProxyManager).getById(TEST_ID);
        verify(streamProxyManager).updateStreamProxy(any(StreamProxyDO.class), anyString());
    }

    @Test
    public void testDelete_Success() throws Exception {
        // Arrange
        when(streamProxyManager.deleteStreamProxy(TEST_ID, anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/proxy/delete/{id}", TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.msg").value("操作成功"));

        verify(streamProxyManager).deleteStreamProxy(TEST_ID, anyString());
    }

    @Test
    public void testExceptionHandling() throws Exception {
        // Arrange - 模拟服务层抛出异常
        when(streamProxyManager.getById(TEST_ID)).thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(get("/api/v1/proxy/get/{id}", TEST_ID)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").exists());

        verify(streamProxyManager).getById(TEST_ID);
    }
}