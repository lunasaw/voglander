package io.github.lunasaw.voglander.web.api.zlm;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.manager.manager.StreamProxyManager;
import io.github.lunasaw.voglander.web.api.zlm.assembler.StreamProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyListResp;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyController 纯单元测试
 * 
 * 测试策略：
 * - 使用@ExtendWith(MockitoExtension.class)进行纯单元测试
 * - 不依赖Spring上下文，只关注当前控制器逻辑
 * - 使用@Mock模拟所有依赖（Manager、Assembler）
 * - 使用@InjectMocks创建控制器实例
 * 
 * 测试覆盖：
 * - 核心模板方法：add, update, get, deleteOne, getPage的控制器层逻辑
 * - 参数验证和数据转换逻辑
 * - 异常处理和响应包装
 * - WebAssembler的正确调用
 * 
 * 注意：
 * - Controller层不直接测试业务逻辑，业务逻辑在Manager层测试
 * - 重点关注请求处理、参数转换、响应包装的正确性
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class StreamProxyControllerTest {

    // 测试常量
    private static final String     TEST_APP       = "live";
    private static final String     TEST_STREAM    = "test";
    private static final String     TEST_URL       = "rtmp://live.example.com/live/test";
    private static final String     TEST_PROXY_KEY = "test-proxy-key-123";
    private static final Long       TEST_ID        = 1L;
    private static final String     TEST_DESC      = "Test proxy description";

    @Mock
    private StreamProxyManager      streamProxyManager;

    @Mock
    private StreamProxyWebAssembler streamProxyWebAssembler;

    @InjectMocks
    private StreamProxyController   streamProxyController;

    // 测试数据对象
    private StreamProxyDTO          testStreamProxyDTO;
    private StreamProxyVO           testStreamProxyVO;
    private StreamProxyCreateReq    testCreateReq;
    private StreamProxyUpdateReq    testUpdateReq;
    private StreamProxyQueryReq     testQueryReq;

    @BeforeEach
    public void setUp() {
        log.info("开始设置测试数据");

        // 初始化测试数据对象
        testStreamProxyDTO = createTestStreamProxyDTO();
        testStreamProxyVO = createTestStreamProxyVO();
        testCreateReq = createTestCreateReq();
        testUpdateReq = createTestUpdateReq();
        testQueryReq = createTestQueryReq();

        log.info("测试数据设置完成");
    }

    /**
     * 创建测试用的StreamProxyDTO对象
     */
    private StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setId(TEST_ID);
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        dto.setProxyKey(TEST_PROXY_KEY);
        dto.setStatus(1);
        dto.setOnlineStatus(0);
        dto.setEnabled(true);
        dto.setDescription(TEST_DESC);
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
        return dto;
    }

    /**
     * 创建测试用的StreamProxyVO对象
     */
    private StreamProxyVO createTestStreamProxyVO() {
        StreamProxyVO vo = new StreamProxyVO();
        vo.setId(TEST_ID);
        vo.setApp(TEST_APP);
        vo.setStream(TEST_STREAM);
        vo.setUrl(TEST_URL);
        vo.setProxyKey(TEST_PROXY_KEY);
        vo.setStatus(1);
        vo.setOnlineStatus(0);
        vo.setEnabled(true);
        vo.setDescription(TEST_DESC);
        vo.setCreateTime(System.currentTimeMillis());
        vo.setUpdateTime(System.currentTimeMillis());
        return vo;
    }

    /**
     * 创建测试用的StreamProxyCreateReq对象
     */
    private StreamProxyCreateReq createTestCreateReq() {
        StreamProxyCreateReq req = new StreamProxyCreateReq();
        req.setApp(TEST_APP);
        req.setStream(TEST_STREAM);
        req.setUrl(TEST_URL);
        req.setDescription(TEST_DESC);
        return req;
    }

    /**
     * 创建测试用的StreamProxyUpdateReq对象
     */
    private StreamProxyUpdateReq createTestUpdateReq() {
        StreamProxyUpdateReq req = new StreamProxyUpdateReq();
        req.setId(TEST_ID);
        req.setApp(TEST_APP);
        req.setStream(TEST_STREAM);
        req.setUrl("rtmp://updated.example.com/live/test");
        req.setDescription("Updated " + TEST_DESC);
        return req;
    }

    /**
     * 创建测试用的StreamProxyQueryReq对象
     */
    private StreamProxyQueryReq createTestQueryReq() {
        StreamProxyQueryReq req = new StreamProxyQueryReq();
        req.setApp(TEST_APP);
        req.setStream(TEST_STREAM);
        return req;
    }

    // ================================
    // 核心模板方法控制器测试
    // ================================

    @Test
    @DisplayName("测试控制器正确实例化")
    public void testControllerInstantiation() {
        // 验证控制器能够正常实例化，依赖正确注入
        assertNotNull(streamProxyController, "控制器不能为null");
        assertNotNull(streamProxyManager, "StreamProxyManager不能为null");
        assertNotNull(streamProxyWebAssembler, "StreamProxyWebAssembler不能为null");

        log.info("StreamProxyController 单元测试启动成功");
    }

    @Test
    @DisplayName("测试add接口 - 成功场景")
    public void testAdd_Success() {
        // Given
        when(streamProxyWebAssembler.createReqToDto(testCreateReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.add(testStreamProxyDTO))
            .thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = streamProxyController.add(testCreateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode()); // 成功响应码
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(streamProxyWebAssembler).createReqToDto(testCreateReq);
        verify(streamProxyManager).add(testStreamProxyDTO);

        log.info("测试add接口成功场景通过");
    }

    @Test
    @DisplayName("测试update接口 - 成功场景")
    public void testUpdate_Success() {
        // Given
        when(streamProxyWebAssembler.updateReqToDto(testUpdateReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.updateById(TEST_ID, testStreamProxyDTO))
            .thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = streamProxyController.updateProxy(testUpdateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(streamProxyWebAssembler).updateReqToDto(testUpdateReq);
        verify(streamProxyManager).updateById(TEST_ID, testStreamProxyDTO);

        log.info("测试update接口成功场景通过");
    }

    @Test
    @DisplayName("测试get接口 - 成功场景")
    public void testGet_Success() {
        // Given
        when(streamProxyWebAssembler.queryReqToDto(testQueryReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.get(testStreamProxyDTO))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyWebAssembler.dtoToVo(testStreamProxyDTO))
            .thenReturn(testStreamProxyVO);

        // When
        AjaxResult<StreamProxyVO> result = streamProxyController.getProxy(testQueryReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(testStreamProxyVO, result.getData());

        // 验证方法调用
        verify(streamProxyWebAssembler).queryReqToDto(testQueryReq);
        verify(streamProxyManager).get(testStreamProxyDTO);
        verify(streamProxyWebAssembler).dtoToVo(testStreamProxyDTO);

        log.info("测试get接口成功场景通过");
    }

    @Test
    @DisplayName("测试get接口 - 记录不存在")
    public void testGet_NotFound() {
        // Given
        when(streamProxyWebAssembler.queryReqToDto(testQueryReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.get(testStreamProxyDTO))
            .thenReturn(null);

        // When
        AjaxResult<StreamProxyVO> result = streamProxyController.getProxy(testQueryReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());

        // 验证方法调用
        verify(streamProxyWebAssembler).queryReqToDto(testQueryReq);
        verify(streamProxyManager).get(testStreamProxyDTO);
        verify(streamProxyWebAssembler, never()).dtoToVo(any());

        log.info("测试get接口记录不存在场景通过");
    }

    @Test
    @DisplayName("测试deleteOne接口 - 成功场景")
    public void testDeleteOne_Success() {
        // Given
        when(streamProxyWebAssembler.updateReqToDto(testUpdateReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.deleteOne(testStreamProxyDTO))
            .thenReturn(true);

        // When
        AjaxResult<Void> result = streamProxyController.deleteOne(testUpdateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("删除成功"));

        // 验证方法调用
        verify(streamProxyWebAssembler).updateReqToDto(testUpdateReq);
        verify(streamProxyManager).deleteOne(testStreamProxyDTO);

        log.info("测试deleteOne接口成功场景通过");
    }

    @Test
    @DisplayName("测试deleteOne接口 - 删除失败")
    public void testDeleteOne_Failed() {
        // Given
        when(streamProxyWebAssembler.updateReqToDto(testUpdateReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.deleteOne(testStreamProxyDTO))
            .thenReturn(false);

        // When
        AjaxResult<Void> result = streamProxyController.deleteOne(testUpdateReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertTrue(result.getMsg().contains("删除失败"));

        // 验证方法调用
        verify(streamProxyWebAssembler).updateReqToDto(testUpdateReq);
        verify(streamProxyManager).deleteOne(testStreamProxyDTO);

        log.info("测试deleteOne接口删除失败场景通过");
    }

    @Test
    @DisplayName("测试getPage接口 - 成功场景")
    public void testGetPage_Success() {
        // Given
        int page = 1;
        int size = 10;
        long total = 25L;

        // 模拟Manager层返回的分页结果
        Page<StreamProxyDTO> managerPage = new Page<>(page, size);
        managerPage.setTotal(total);
        managerPage.setRecords(List.of(testStreamProxyDTO));

        when(streamProxyWebAssembler.queryReqToDto(testQueryReq))
            .thenReturn(testStreamProxyDTO);
        when(streamProxyManager.getPage(testStreamProxyDTO, page, size))
            .thenReturn(managerPage);
        when(streamProxyWebAssembler.dtoToVo(testStreamProxyDTO))
            .thenReturn(testStreamProxyVO);

        // When
        AjaxResult<StreamProxyListResp> result = streamProxyController.getPageWithConditions(
            testQueryReq, page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        StreamProxyListResp resp = result.getData();
        assertNotNull(resp);
        assertEquals(total, resp.getTotal());
        assertNotNull(resp.getItems());
        assertEquals(1, resp.getItems().size());
        assertEquals(testStreamProxyVO, resp.getItems().get(0));

        // 验证方法调用
        verify(streamProxyWebAssembler).queryReqToDto(testQueryReq);
        verify(streamProxyManager).getPage(testStreamProxyDTO, page, size);
        verify(streamProxyWebAssembler).dtoToVo(testStreamProxyDTO);

        log.info("测试getPage接口成功场景通过");
    }

    @Test
    @DisplayName("测试getPage接口 - 无条件查询")
    public void testGetPage_NoConditions() {
        // Given
        int page = 1;
        int size = 10;

        Page<StreamProxyDTO> emptyPage = new Page<>(page, size);
        emptyPage.setTotal(0);
        emptyPage.setRecords(new ArrayList<>());

        when(streamProxyWebAssembler.queryReqToDto(null))
            .thenReturn(null);
        when(streamProxyManager.getPage(null, page, size))
            .thenReturn(emptyPage);

        // When
        AjaxResult<StreamProxyListResp> result = streamProxyController.getPageWithConditions(
            null, page, size);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        StreamProxyListResp resp = result.getData();
        assertNotNull(resp);
        assertEquals(0, resp.getTotal());
        assertTrue(resp.getItems().isEmpty());

        // 验证方法调用
        verify(streamProxyWebAssembler).queryReqToDto(null);
        verify(streamProxyManager).getPage(null, page, size);

        log.info("测试getPage接口无条件查询场景通过");
    }

}