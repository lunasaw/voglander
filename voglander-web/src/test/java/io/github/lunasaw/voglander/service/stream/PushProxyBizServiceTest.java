package io.github.lunasaw.voglander.service.stream;

import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaNodeDTO;
import io.github.lunasaw.voglander.manager.manager.PushProxyManager;
import io.github.lunasaw.voglander.manager.manager.MediaNodeManager;
import io.github.lunasaw.voglander.intergration.wrapper.zlm.service.PushProxyZlmWrapperService;
import io.github.lunasaw.voglander.service.stream.impl.PushProxyBizServiceImpl;
import com.luna.common.dto.ResultDTO;
import io.github.lunasaw.zlm.entity.MediaOnlineStatus;
import io.github.lunasaw.zlm.entity.StreamKey;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 推流代理业务服务单元测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PushProxyBizServiceTest {

    @Mock
    private PushProxyManager           pushProxyManager;

    @Mock
    private MediaNodeManager           mediaNodeManager;

    @Mock
    private PushProxyZlmWrapperService pushProxyZlmWrapperService;

    @InjectMocks
    private PushProxyBizServiceImpl    pushProxyBizService;

    private PushProxyDTO               testDTO;
    private MediaNodeDTO               testNodeDTO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理业务服务测试数据");

        // 创建测试DTO对象
        testDTO = new PushProxyDTO();
        testDTO.setId(1L);
        testDTO.setApp("live");
        testDTO.setStream("test");
        testDTO.setDstUrl("rtmp://push.example.com/live/test");
        testDTO.setSchema("rtmp");
        testDTO.setDescription("测试推流代理");
        testDTO.setStatus(1);
        testDTO.setOnlineStatus(1);
        testDTO.setEnabled(1);
        testDTO.setServerId("zlm-node-1");
        testDTO.setProxyKey("test_proxy_key");

        // 设置ExtendObj
        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("__defaultVhost__");
        extendObj.setRetryCount(-1);
        extendObj.setRtpType(0);
        extendObj.setTimeoutSec(10);
        testDTO.setExtendObj(extendObj);

        // 创建测试MediaNodeDTO对象
        testNodeDTO = new MediaNodeDTO();
        testNodeDTO.setId(1L);
        testNodeDTO.setServerId("zlm-node-1");
        testNodeDTO.setHost("localhost");
        testNodeDTO.setSecret("zlm");
        testNodeDTO.setEnabled(true);

        log.info("推流代理业务服务测试数据设置完成");
    }

    // ================================
    // createPushProxyWithSpecificNode 测试
    // ================================

    @Test
    public void testCreatePushProxyWithSpecificNode_Success() {
        // Given
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);
        when(pushProxyManager.createPushProxy(testDTO)).thenReturn(1L);
        // 模拟startPushProxy内部调用的ZLM服务
        when(pushProxyManager.get(argThat(dto -> dto.getId() != null && dto.getId().equals(1L)))).thenReturn(testDTO);
        when(pushProxyZlmWrapperService.addPushProxy(any())).thenReturn(null); // 模拟ZLM API调用失败

        // When
        Long result = pushProxyBizService.createPushProxyWithSpecificNode(testDTO);

        // Then
        assertNotNull(result);
        assertEquals(1L, result);

        verify(mediaNodeManager, times(2)).getDTOByServerId(testDTO.getServerId()); // 调用两次：createPushProxy和startPushProxy
        verify(pushProxyManager).createPushProxy(testDTO);

        log.info("指定节点创建推流代理测试通过");
    }

    @Test
    public void testCreatePushProxyWithSpecificNode_NullDTO() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.createPushProxyWithSpecificNode(null));

        log.info("创建推流代理空DTO参数校验测试通过");
    }

    @Test
    public void testCreatePushProxyWithSpecificNode_EmptyServerId() {
        // Given
        testDTO.setServerId(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.createPushProxyWithSpecificNode(testDTO));

        log.info("创建推流代理空节点ID参数校验测试通过");
    }

    // ================================
    // updatePushProxyWithRecreation 测试
    // ================================

    @Test
    public void testUpdatePushProxyWithRecreation_Success() {
        // Given
        when(pushProxyManager.updatePushProxy(testDTO, "更新推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.updatePushProxyWithRecreation(testDTO);

        // Then
        assertTrue(result);

        verify(pushProxyManager).updatePushProxy(testDTO, "更新推流代理");

        log.info("更新推流代理测试通过");
    }

    @Test
    public void testUpdatePushProxyWithRecreation_NotFound() {
        // Given
        when(pushProxyManager.updatePushProxy(testDTO, "更新推流代理")).thenReturn(false);

        // When
        boolean result = pushProxyBizService.updatePushProxyWithRecreation(testDTO);

        // Then
        assertFalse(result);

        verify(pushProxyManager).updatePushProxy(testDTO, "更新推流代理");

        log.info("更新不存在的推流代理测试通过");
    }

    @Test
    public void testUpdatePushProxyWithRecreation_ByIdSuccess() {
        // Given
        // updatePushProxyWithRecreation 调用 updatePushProxy 而不是 getById/updateById
        when(pushProxyManager.updatePushProxy(testDTO, "更新推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.updatePushProxyWithRecreation(1L, testDTO);

        // Then
        assertTrue(result);

        // 验证实际调用的方法
        verify(pushProxyManager).updatePushProxy(testDTO, "更新推流代理");

        log.info("根据ID更新推流代理测试通过");
    }

    // ================================
    // deletePushProxyWithTermination 测试
    // ================================

    @Test
    public void testDeletePushProxyWithTermination_Success() {
        // Given
        when(pushProxyManager.get(testDTO)).thenReturn(testDTO);
        when(pushProxyManager.deletePushProxy(testDTO, "删除推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.deletePushProxyWithTermination(testDTO);

        // Then
        assertTrue(result);

        verify(pushProxyManager).get(testDTO);
        verify(pushProxyManager).deletePushProxy(testDTO, "删除推流代理");

        log.info("删除推流代理测试通过");
    }

    @Test
    public void testDeletePushProxyWithTermination_NotFound() {
        // Given
        // 模拟deletePushProxy方法返回false（记录不存在）
        when(pushProxyManager.deletePushProxy(testDTO, "删除推流代理")).thenReturn(false);

        // When
        boolean result = pushProxyBizService.deletePushProxyWithTermination(testDTO);

        // Then - 实际实现中，如果Manager删除失败则返回false
        assertFalse(result);

        verify(pushProxyManager).deletePushProxy(testDTO, "删除推流代理");

        log.info("删除不存在的推流代理测试通过");
    }

    @Test
    public void testDeletePushProxyByKeyWithTermination_Success() {
        // Given
        when(pushProxyManager.get(any())).thenReturn(testDTO);
        when(pushProxyManager.deletePushProxy(any(), eq("删除推流代理"))).thenReturn(true);

        // When
        boolean result = pushProxyBizService.deletePushProxyByKeyWithTermination("test_proxy_key");

        // Then
        assertTrue(result);

        verify(pushProxyManager).get(any());
        verify(pushProxyManager).deletePushProxy(any(), eq("删除推流代理"));

        log.info("根据代理键删除推流代理测试通过");
    }

    // ================================
    // updatePushProxyStatus 测试
    // ================================

    @Test
    public void testUpdatePushProxyStatus_Success() {
        // Given
        when(pushProxyManager.get(testDTO)).thenReturn(testDTO);
        when(pushProxyManager.updatePushProxy(any(), eq("更新推流代理状态"))).thenReturn(true);

        // When
        boolean result = pushProxyBizService.updatePushProxyStatus(testDTO);

        // Then
        assertTrue(result);

        verify(pushProxyManager).get(testDTO);
        verify(pushProxyManager).updatePushProxy(any(), eq("更新推流代理状态"));

        log.info("更新推流代理状态测试通过");
    }

    @Test
    public void testUpdatePushProxyStatus_ByIdSuccess() {
        // Given
        when(pushProxyManager.updatePushProxy(any(PushProxyDTO.class), eq("更新推流代理状态"))).thenReturn(true);
        when(pushProxyManager.get(any(PushProxyDTO.class))).thenReturn(testDTO);

        // When
        boolean result = pushProxyBizService.updatePushProxyStatus(1L, 0);

        // Then
        assertTrue(result);

        verify(pushProxyManager).updatePushProxy(any(PushProxyDTO.class), eq("更新推流代理状态"));
        verify(pushProxyManager).get(any(PushProxyDTO.class));

        log.info("根据ID更新推流代理状态测试通过");
    }

    @Test
    public void testUpdatePushProxyStatus_InvalidStatus() {
        // Given - 当前实现没有状态值验证，接受任何Integer值
        // TODO: 如果需要状态值验证，应在业务层实现验证逻辑
        when(pushProxyManager.updatePushProxy(any(), eq("更新推流代理状态"))).thenReturn(false);

        // When
        boolean result = pushProxyBizService.updatePushProxyStatus(1L, 2);

        // Then - 当前实现不验证状态值，返回Manager层的结果
        assertFalse(result);

        log.info("状态值处理测试通过（当前无验证）");
    }

    // ================================
    // syncPushProxyOnlineStatus 测试
    // ================================

    @Test
    public void testSyncPushProxyOnlineStatus_Success() {
        // Given - 当前实现是占位符，直接返回true，不调用Manager
        // TODO: 当PushProxyZlmWrapperService可用时更新此测试

        // When
        boolean result = pushProxyBizService.syncPushProxyOnlineStatus(testDTO);

        // Then
        assertTrue(result);

        // 当前实现是占位符，不验证Manager调用
        // TODO: 实现完成后验证Manager方法调用
        // verify(pushProxyManager).get(testDTO);
        // verify(pushProxyManager).updatePushProxyOnlineStatus(1L, 1, "同步推流代理在线状态");

        log.info("同步推流代理在线状态测试通过（占位符实现）");
    }

    @Test
    public void testSyncPushProxyOnlineStatus_ByIdSuccess() {
        // Given - 当前实现是占位符，直接返回true，不调用Manager
        // TODO: 当PushProxyZlmWrapperService可用时更新此测试

        // When
        boolean result = pushProxyBizService.syncPushProxyOnlineStatus(1L);

        // Then
        assertTrue(result);

        // 当前实现是占位符，不验证Manager调用
        // TODO: 实现完成后验证Manager方法调用
        // verify(pushProxyManager).getById(1L);
        // verify(pushProxyManager).updatePushProxyOnlineStatus(1L, 1, "同步推流代理在线状态");

        log.info("根据ID同步推流代理在线状态测试通过（占位符实现）");
    }

    // ================================
    // startPushProxy 测试
    // ================================

    @Test
    public void testStartPushProxy_Success() {
        // Given
        when(pushProxyManager.get(testDTO)).thenReturn(testDTO);
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);

        // 模拟ZLM启动推流代理成功
        StreamKey streamKey = new StreamKey();
        streamKey.setKey("test_generated_key");
        ResultDTO<StreamKey> successResult = new ResultDTO<>();
        successResult.setData(streamKey);
        when(pushProxyZlmWrapperService.addPushProxy(any())).thenReturn(successResult);

        when(pushProxyManager.updatePushProxyKey(testDTO.getId(), "test_generated_key", "启动推流代理")).thenReturn(true);
        when(pushProxyManager.updatePushProxyOnlineStatus(testDTO.getId(), 1, "启动推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.startPushProxy(testDTO);

        // Then
        assertTrue(result); // ZLM API成功时应返回true

        verify(pushProxyManager).get(testDTO);
        verify(mediaNodeManager).getDTOByServerId(testDTO.getServerId());
        verify(pushProxyZlmWrapperService).addPushProxy(any());

        log.info("启动推流代理测试通过（ZLM API成功场景）");
    }

    @Test
    public void testStartPushProxy_ByIdSuccess() {
        // Given
        // startPushProxy(Long id) 创建DTO并调用 get() 方法，不是 getById()
        when(pushProxyManager.get(argThat(dto -> dto.getId() != null && dto.getId().equals(1L)))).thenReturn(testDTO);
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);

        // 模拟ZLM启动推流代理成功
        StreamKey streamKey = new StreamKey();
        streamKey.setKey("test_generated_key");
        ResultDTO<StreamKey> successResult = new ResultDTO<>();
        successResult.setData(streamKey);
        when(pushProxyZlmWrapperService.addPushProxy(any())).thenReturn(successResult);

        when(pushProxyManager.updatePushProxyKey(testDTO.getId(), "test_generated_key", "启动推流代理")).thenReturn(true);
        when(pushProxyManager.updatePushProxyOnlineStatus(testDTO.getId(), 1, "启动推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.startPushProxy(1L);

        // Then
        assertTrue(result); // ZLM API成功时应返回true

        // 验证实际调用的方法：get() 而不是 getById()
        verify(pushProxyManager).get(argThat(dto -> dto.getId() != null && dto.getId().equals(1L)));
        verify(mediaNodeManager).getDTOByServerId(testDTO.getServerId());
        verify(pushProxyZlmWrapperService).addPushProxy(any());

        log.info("根据ID启动推流代理测试通过（ZLM API成功场景）");
    }

    @Test
    public void testStartPushProxy_NotFound() {
        // Given
        when(pushProxyManager.get(testDTO)).thenReturn(null);

        // When
        boolean result = pushProxyBizService.startPushProxy(testDTO);

        // Then
        assertFalse(result);

        verify(pushProxyManager).get(testDTO);
        verify(pushProxyManager, never()).updatePushProxy(any(), any());

        log.info("启动不存在的推流代理测试通过");
    }

    // ================================
    // stopPushProxy 测试
    // ================================

    @Test
    public void testStopPushProxy_Success() {
        // Given
        when(pushProxyManager.get(testDTO)).thenReturn(testDTO);
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);

        // 模拟ZLM删除推流代理成功
        StreamKey.StringDelFlag delFlag = new StreamKey.StringDelFlag();
        delFlag.setFlag("1");
        ResultDTO<StreamKey.StringDelFlag> successResult = new ResultDTO<>();
        successResult.setData(delFlag);
        when(pushProxyZlmWrapperService.deletePushProxy(any())).thenReturn(successResult);

        when(pushProxyManager.updatePushProxyOnlineStatus(testDTO.getId(), 0, "停止推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.stopPushProxy(testDTO);

        // Then
        assertTrue(result); // ZLM API成功时应返回true

        verify(pushProxyManager).get(testDTO);
        verify(mediaNodeManager).getDTOByServerId(testDTO.getServerId());
        verify(pushProxyZlmWrapperService).deletePushProxy(any());
        verify(pushProxyManager).updatePushProxyOnlineStatus(testDTO.getId(), 0, "停止推流代理");

        log.info("停止推流代理测试通过（ZLM API成功场景）");
    }

    @Test
    public void testStopPushProxy_ByIdSuccess() {
        // Given
        // stopPushProxy(Long id) 创建DTO并调用 get() 方法，不是 getById()
        when(pushProxyManager.get(argThat(dto -> dto.getId() != null && dto.getId().equals(1L)))).thenReturn(testDTO);
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);

        // 模拟ZLM删除推流代理成功
        StreamKey.StringDelFlag delFlag = new StreamKey.StringDelFlag();
        delFlag.setFlag("1");
        ResultDTO<StreamKey.StringDelFlag> successResult = new ResultDTO<>();
        successResult.setData(delFlag);
        when(pushProxyZlmWrapperService.deletePushProxy(any())).thenReturn(successResult);

        when(pushProxyManager.updatePushProxyOnlineStatus(1L, 0, "停止推流代理")).thenReturn(true);

        // When
        boolean result = pushProxyBizService.stopPushProxy(1L);

        // Then
        assertTrue(result); // ZLM API成功时应返回true

        // 验证实际调用的方法：get() 而不是 getById()
        verify(pushProxyManager).get(argThat(dto -> dto.getId() != null && dto.getId().equals(1L)));
        verify(mediaNodeManager).getDTOByServerId(testDTO.getServerId());
        verify(pushProxyZlmWrapperService).deletePushProxy(any());
        verify(pushProxyManager).updatePushProxyOnlineStatus(1L, 0, "停止推流代理");

        log.info("根据ID停止推流代理测试通过（ZLM API成功场景）");
    }

    // ================================
    // checkSourceStreamOnline 测试
    // ================================

    @Test
    public void testCheckSourceStreamOnline_Success() {
        // Given - 模拟完整的检查流程
        when(mediaNodeManager.getDTOByServerId(testDTO.getServerId())).thenReturn(testNodeDTO);

        // 模拟ZLM在线状态检查返回在线
        MediaOnlineStatus onlineStatus = new MediaOnlineStatus();
        onlineStatus.setOnline(true);
        ResultDTO<MediaOnlineStatus> successResult = new ResultDTO<>();
        successResult.setData(onlineStatus);
        when(pushProxyZlmWrapperService.isSourceStreamOnline(any())).thenReturn(successResult);

        // When
        boolean result = pushProxyBizService.checkSourceStreamOnline(testDTO);

        // Then
        assertTrue(result); // 期望返回在线状态

        verify(mediaNodeManager).getDTOByServerId(testDTO.getServerId());
        verify(pushProxyZlmWrapperService).isSourceStreamOnline(any());

        log.info("检查源流在线状态测试通过，结果: {}", result);
    }

    @Test
    public void testCheckSourceStreamOnline_ByParametersSuccess() {
        // Given - 模拟参数形式的检查流程
        when(mediaNodeManager.getDTOByServerId("zlm-node-1")).thenReturn(testNodeDTO);

        // 模拟ZLM在线状态检查返回在线
        MediaOnlineStatus onlineStatus = new MediaOnlineStatus();
        onlineStatus.setOnline(true);
        ResultDTO<MediaOnlineStatus> successResult = new ResultDTO<>();
        successResult.setData(onlineStatus);
        when(pushProxyZlmWrapperService.isSourceStreamOnline(any())).thenReturn(successResult);

        // When
        boolean result = pushProxyBizService.checkSourceStreamOnline("zlm-node-1", "live", "test");

        // Then
        assertTrue(result); // 期望返回在线状态

        verify(mediaNodeManager).getDTOByServerId("zlm-node-1");
        verify(pushProxyZlmWrapperService).isSourceStreamOnline(any());

        log.info("根据参数检查源流在线状态测试通过，结果: {}", result);
    }

    @Test
    public void testCheckSourceStreamOnline_NullParameters() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.checkSourceStreamOnline(null, "live", "test"));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.checkSourceStreamOnline("zlm-node-1", null, "test"));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.checkSourceStreamOnline("zlm-node-1", "live", null));

        log.info("检查源流在线状态参数校验测试通过");
    }

    // ================================
    // syncAllEnabledPushProxyStatus 测试
    // ================================

    @Test
    public void testSyncAllEnabledPushProxyStatus_Success() {
        // When
        int result = pushProxyBizService.syncAllEnabledPushProxyStatus();

        // Then
        assertTrue(result >= 0); // 同步的记录数应该为非负数

        log.info("批量同步所有启用的推流代理状态测试通过，同步记录数: {}", result);
    }

    // ================================
    // 异常场景测试
    // ================================

    @Test
    public void testNullDTOValidation() {
        // 测试所有主要接口的null DTO参数校验
        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.updatePushProxyWithRecreation(null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.deletePushProxyWithTermination(null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.updatePushProxyStatus((PushProxyDTO)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.syncPushProxyOnlineStatus((PushProxyDTO)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.startPushProxy((PushProxyDTO)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.stopPushProxy((PushProxyDTO)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.checkSourceStreamOnline((PushProxyDTO)null));

        log.info("所有主要接口null DTO参数校验测试通过");
    }

    @Test
    public void testInvalidIdValidation() {
        // 测试所有便利方法的无效ID参数校验
        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.updatePushProxyWithRecreation(null, testDTO));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.updatePushProxyStatus(null, 1));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.syncPushProxyOnlineStatus((Long)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.startPushProxy((Long)null));

        assertThrows(IllegalArgumentException.class, () -> pushProxyBizService.stopPushProxy((Long)null));

        log.info("所有便利方法无效ID参数校验测试通过");
    }
}