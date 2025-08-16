package io.github.lunasaw.voglander.web.api.zlm;

import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.manager.PushProxyManager;
import io.github.lunasaw.voglander.service.stream.PushProxyBizService;
import io.github.lunasaw.voglander.web.api.zlm.assembler.PushProxyWebAssembler;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyListResp;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 推流代理控制器单元测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PushProxyControllerTest {

    @Mock
    private PushProxyManager      pushProxyManager;

    @Mock
    private PushProxyWebAssembler pushProxyWebAssembler;

    @Mock
    private PushProxyBizService   pushProxyBizService;

    @InjectMocks
    private PushProxyController   pushProxyController;

    private PushProxyCreateReq    createReq;
    private PushProxyUpdateReq    updateReq;
    private PushProxyQueryReq     queryReq;
    private PushProxyDTO          dto;
    private PushProxyVO           vo;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理控制器测试数据");

        // 创建测试请求对象
        createReq = new PushProxyCreateReq();
        createReq.setApp("live");
        createReq.setStream("test");
        createReq.setDstUrl("rtmp://push.example.com/live/test");
        createReq.setSchema("rtmp");
        createReq.setDescription("测试推流代理");
        createReq.setStatus(1);
        createReq.setServerId("zlm-node-1");

        updateReq = new PushProxyUpdateReq();
        updateReq.setId(1L);
        updateReq.setApp("live");
        updateReq.setStream("test");
        updateReq.setDstUrl("rtmp://updated.example.com/live/test");
        updateReq.setSchema("rtmp");
        updateReq.setDescription("更新后的推流代理");
        updateReq.setStatus(1);
        updateReq.setServerId("zlm-node-1");

        queryReq = new PushProxyQueryReq();
        queryReq.setId(1L);
        queryReq.setApp("live");
        queryReq.setStream("test");

        // 创建测试DTO对象
        dto = new PushProxyDTO();
        dto.setId(1L);
        dto.setApp("live");
        dto.setStream("test");
        dto.setDstUrl("rtmp://push.example.com/live/test");
        dto.setSchema("rtmp");
        dto.setDescription("测试推流代理");
        dto.setStatus(1);
        dto.setServerId("zlm-node-1");

        // 创建测试VO对象
        vo = new PushProxyVO();
        vo.setId(1L);
        vo.setApp("live");
        vo.setStream("test");
        vo.setDstUrl("rtmp://push.example.com/live/test");
        vo.setSchema("rtmp");
        vo.setDescription("测试推流代理");
        vo.setStatus(1);
        vo.setServerId("zlm-node-1");

        log.info("推流代理控制器测试数据设置完成");
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testGetById_Success() {
        // Given
        when(pushProxyManager.getById(1L)).thenReturn(dto);
        when(pushProxyWebAssembler.dtoToVo(dto)).thenReturn(vo);

        // When
        AjaxResult<PushProxyVO> result = pushProxyController.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("live", result.getData().getApp());

        verify(pushProxyManager).getById(1L);
        verify(pushProxyWebAssembler).dtoToVo(dto);

        log.info("根据ID获取推流代理测试通过");
    }

    @Test
    public void testGetById_NotFound() {
        // Given
        when(pushProxyManager.getById(1L)).thenReturn(null);

        // When
        AjaxResult<PushProxyVO> result = pushProxyController.getById(1L);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertEquals("推流代理不存在", result.getMsg());

        verify(pushProxyManager).getById(1L);
        verify(pushProxyWebAssembler, never()).dtoToVo(any());

        log.info("推流代理不存在测试通过");
    }

    @Test
    public void testAdd_Success() {
        // Given
        when(pushProxyWebAssembler.createReqToDto(createReq)).thenReturn(dto);
        when(pushProxyManager.add(dto)).thenReturn(1L);

        // When
        AjaxResult<Long> result = pushProxyController.add(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData());

        verify(pushProxyWebAssembler).createReqToDto(createReq);
        verify(pushProxyManager).add(dto);

        log.info("新增推流代理测试通过");
    }

    @Test
    public void testUpdateProxy_Success() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyManager.updateById(updateReq.getId(), dto)).thenReturn(1L);

        // When
        AjaxResult<Long> result = pushProxyController.updateProxy(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData());

        verify(pushProxyWebAssembler).updateReqToDto(updateReq);
        verify(pushProxyManager).updateById(updateReq.getId(), dto);

        log.info("更新推流代理测试通过");
    }

    @Test
    public void testGetProxy_Success() {
        // Given
        when(pushProxyWebAssembler.queryReqToDto(queryReq)).thenReturn(dto);
        when(pushProxyManager.get(dto)).thenReturn(dto);
        when(pushProxyWebAssembler.dtoToVo(dto)).thenReturn(vo);

        // When
        AjaxResult<PushProxyVO> result = pushProxyController.getProxy(queryReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());

        verify(pushProxyWebAssembler).queryReqToDto(queryReq);
        verify(pushProxyManager).get(dto);
        verify(pushProxyWebAssembler).dtoToVo(dto);

        log.info("灵活查询推流代理测试通过");
    }

    @Test
    public void testGetProxy_NotFound() {
        // Given
        when(pushProxyWebAssembler.queryReqToDto(queryReq)).thenReturn(dto);
        when(pushProxyManager.get(dto)).thenReturn(null);

        // When
        AjaxResult<PushProxyVO> result = pushProxyController.getProxy(queryReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertEquals("推流代理不存在", result.getMsg());

        verify(pushProxyManager).get(dto);
        verify(pushProxyWebAssembler, never()).dtoToVo(any());

        log.info("推流代理查询不存在测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyBizService.deletePushProxyWithTermination(dto)).thenReturn(true);

        // When
        AjaxResult<Void> result = pushProxyController.deleteOne(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        verify(pushProxyWebAssembler).updateReqToDto(updateReq);
        verify(pushProxyBizService).deletePushProxyWithTermination(dto);

        log.info("删除单个推流代理测试通过");
    }

    @Test
    public void testDeleteOne_Failed() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyBizService.deletePushProxyWithTermination(dto)).thenReturn(false);

        // When
        AjaxResult<Void> result = pushProxyController.deleteOne(updateReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertEquals("删除失败", result.getMsg());

        log.info("删除推流代理失败测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyManager.deleteBatch(dto)).thenReturn(true);

        // When
        AjaxResult<Void> result = pushProxyController.deleteBatch(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());

        verify(pushProxyWebAssembler).updateReqToDto(updateReq);
        verify(pushProxyManager).deleteBatch(dto);

        log.info("批量删除推流代理测试通过");
    }

    @Test
    public void testGetPageWithConditions_Success() {
        // Given
        List<PushProxyDTO> dtoList = Arrays.asList(dto);

        Page<PushProxyDTO> pageResult = new Page<>(1, 10);
        pageResult.setTotal(1L);
        pageResult.setRecords(dtoList);

        when(pushProxyWebAssembler.queryReqToDto(queryReq)).thenReturn(dto);
        when(pushProxyManager.getPage(dto, 1, 10)).thenReturn(pageResult);
        when(pushProxyWebAssembler.dtoToVo(dto)).thenReturn(vo);

        // When
        AjaxResult<PushProxyListResp> result = pushProxyController.getPageWithConditions(queryReq, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getItems().size());
        assertEquals(1L, result.getData().getItems().get(0).getId());

        verify(pushProxyWebAssembler).queryReqToDto(queryReq);
        verify(pushProxyManager).getPage(dto, 1, 10);
        verify(pushProxyWebAssembler).dtoToVo(dto);

        log.info("分页查询推流代理测试通过");
    }

    // ================================
    // 增强业务方法测试
    // ================================

    @Test
    public void testCreatePushProxy_Success() {
        // Given
        when(pushProxyWebAssembler.createReqToDto(createReq)).thenReturn(dto);
        when(pushProxyManager.createPushProxy(dto)).thenReturn(1L);

        // When
        AjaxResult<Long> result = pushProxyController.createPushProxy(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData());

        verify(pushProxyWebAssembler).createReqToDto(createReq);
        verify(pushProxyManager).createPushProxy(dto);

        log.info("业务创建推流代理测试通过");
    }

    @Test
    public void testCreatePushProxyWithNode_Success() {
        // Given
        when(pushProxyWebAssembler.createReqToDto(createReq)).thenReturn(dto);
        when(pushProxyBizService.createPushProxyWithSpecificNode(dto)).thenReturn(1L);

        // When
        AjaxResult<Long> result = pushProxyController.createPushProxyWithNode(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData());

        verify(pushProxyWebAssembler).createReqToDto(createReq);
        verify(pushProxyBizService).createPushProxyWithSpecificNode(dto);

        log.info("指定节点创建推流代理测试通过");
    }

    @Test
    public void testUpdatePushProxy_Success() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyManager.updatePushProxy(dto, "更新推流代理")).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.updatePushProxy(updateReq, "更新推流代理");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyWebAssembler).updateReqToDto(updateReq);
        verify(pushProxyManager).updatePushProxy(dto, "更新推流代理");

        log.info("业务更新推流代理测试通过");
    }

    @Test
    public void testDeletePushProxy_Success() {
        // Given
        when(pushProxyWebAssembler.updateReqToDto(updateReq)).thenReturn(dto);
        when(pushProxyManager.deletePushProxy(dto, "删除推流代理")).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.deletePushProxy(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyWebAssembler).updateReqToDto(updateReq);
        verify(pushProxyManager).deletePushProxy(dto, "删除推流代理");

        log.info("业务删除推流代理测试通过");
    }

    @Test
    public void testUpdatePushProxyStatus_Success() {
        // Given
        when(pushProxyBizService.updatePushProxyStatus(1L, 0)).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.updatePushProxyStatus(1L, 0);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyBizService).updatePushProxyStatus(1L, 0);

        log.info("更新推流代理状态测试通过");
    }

    @Test
    public void testStartPushProxy_Success() {
        // Given
        when(pushProxyBizService.startPushProxy(1L)).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.startPushProxy(1L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyBizService).startPushProxy(1L);

        log.info("启动推流代理测试通过");
    }

    @Test
    public void testStopPushProxy_Success() {
        // Given
        when(pushProxyBizService.stopPushProxy(1L)).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.stopPushProxy(1L);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyBizService).stopPushProxy(1L);

        log.info("停止推流代理测试通过");
    }

    @Test
    public void testCheckSourceStreamOnline_Success() {
        // Given
        when(pushProxyBizService.checkSourceStreamOnline("zlm-node-1", "live", "test")).thenReturn(true);

        // When
        AjaxResult<Boolean> result = pushProxyController.checkSourceStreamOnline("zlm-node-1", "live", "test");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData());

        verify(pushProxyBizService).checkSourceStreamOnline("zlm-node-1", "live", "test");

        log.info("检查源流在线状态测试通过");
    }
}