package io.github.lunasaw.voglander.web.api.channel;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.web.api.channel.assembler.DeviceChannelWebAssembler;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelCreateReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelQueryReq;
import io.github.lunasaw.voglander.web.api.channel.req.DeviceChannelUpdateReq;
import io.github.lunasaw.voglander.web.api.channel.resp.DeviceChannelListResp;
import io.github.lunasaw.voglander.web.api.channel.vo.DeviceChannelVO;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceChannelController纯单元测试类
 * 
 * 测试策略：
 * - 使用@ExtendWith(MockitoExtension.class)进行纯单元测试，不启动Spring上下文
 * - 使用@Mock模拟所有依赖：Manager、Assembler等全部模拟
 * - 使用@InjectMocks创建控制器实例，让Mockito自动注入模拟依赖
 * - 不使用@WebMvcTest，避免Spring上下文启动和其他控制器干扰
 * - 专注业务逻辑测试：验证控制器方法调用、参数传递、返回值处理
 * 
 * 测试覆盖：
 * - 6个核心模板方法的完整测试：add, update, get, deleteOne, deleteBatch, getPage
 * - 增强业务方法测试：createDeviceChannel, updateDeviceChannel, deleteDeviceChannel
 * - 兼容历史接口测试：getById, insert等
 * - 参数校验、数据转换、异常处理测试
 * - AjaxResult响应格式验证
 * 
 * 模板方法测试重点：
 * - 入参转换：WebAssembler.xxxReqToDto(xxxReq) → Manager层
 * - 出参转换：Manager层返回DTO → WebAssembler.dtoToVo(dto) → 前端
 * - 错误处理：统一使用AjaxResult.error()和AjaxResult.success()
 * - 分页响应：包装为*ListResp对象，包含total和items字段
 * - 参数验证：使用@Valid进行请求参数校验
 * 
 * @author luna
 * @date 2025-01-31
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class DeviceChannelControllerTest {

    // 测试数据常量
    private static final Long         TEST_ID           = 1L;
    private static final String       TEST_DEVICE_ID    = "test-device-123";
    private static final String       TEST_CHANNEL_ID   = "channel-001";
    private static final String       TEST_CHANNEL_NAME = "测试通道";
    private static final Integer      TEST_STATUS       = DeviceConstant.Status.ONLINE;

    @Mock
    private DeviceChannelManager      deviceChannelManager;

    @Mock
    private DeviceChannelWebAssembler deviceChannelWebAssembler;

    @InjectMocks
    private DeviceChannelController   deviceChannelController;

    private DeviceChannelCreateReq    createReq;
    private DeviceChannelUpdateReq    updateReq;
    private DeviceChannelQueryReq     queryReq;
    private DeviceChannelDTO          testDTO;
    private DeviceChannelVO           testVO;

    @BeforeEach
    public void setUp() {
        // 创建测试请求对象
        createReq = new DeviceChannelCreateReq();
        createReq.setDeviceId(TEST_DEVICE_ID);
        createReq.setChannelId(TEST_CHANNEL_ID);
        createReq.setName(TEST_CHANNEL_NAME);

        updateReq = new DeviceChannelUpdateReq();
        updateReq.setId(TEST_ID);
        updateReq.setDeviceId(TEST_DEVICE_ID);
        updateReq.setChannelId(TEST_CHANNEL_ID);
        updateReq.setName(TEST_CHANNEL_NAME);
        updateReq.setStatus(TEST_STATUS);

        queryReq = new DeviceChannelQueryReq();
        queryReq.setId(TEST_ID);
        queryReq.setDeviceId(TEST_DEVICE_ID);

        // 创建测试DTO和VO对象
        testDTO = new DeviceChannelDTO();
        testDTO.setId(TEST_ID);
        testDTO.setDeviceId(TEST_DEVICE_ID);
        testDTO.setChannelId(TEST_CHANNEL_ID);
        testDTO.setName(TEST_CHANNEL_NAME);
        testDTO.setStatus(TEST_STATUS);

        testVO = new DeviceChannelVO();
        testVO.setId(TEST_ID);
        testVO.setDeviceId(TEST_DEVICE_ID);
        testVO.setChannelId(TEST_CHANNEL_ID);
        testVO.setName(TEST_CHANNEL_NAME);
        testVO.setStatus(TEST_STATUS);
        testVO.setStatusName("在线");
    }

    // ================================
    // 核心模板方法测试
    // ================================

    @Test
    public void testAdd_Success() {
        // Given
        when(deviceChannelWebAssembler.createReqToDto(createReq)).thenReturn(testDTO);
        when(deviceChannelManager.add(testDTO)).thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = deviceChannelController.add(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).createReqToDto(createReq);
        verify(deviceChannelManager).add(testDTO);

        log.info("add方法测试通过");
    }

    @Test
    public void testUpdate_Success() {
        // Given
        when(deviceChannelWebAssembler.updateReqToDto(updateReq)).thenReturn(testDTO);
        when(deviceChannelManager.updateById(TEST_ID, testDTO)).thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = deviceChannelController.update(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).updateReqToDto(updateReq);
        verify(deviceChannelManager).updateById(TEST_ID, testDTO);

        log.info("update方法测试通过");
    }

    @Test
    public void testGet_Success() {
        // Given
        when(deviceChannelWebAssembler.queryReqToDto(queryReq)).thenReturn(testDTO);
        when(deviceChannelManager.get(testDTO)).thenReturn(testDTO);
        when(deviceChannelWebAssembler.dtoToVo(testDTO)).thenReturn(testVO);

        // When
        AjaxResult<DeviceChannelVO> result = deviceChannelController.get(queryReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(testVO, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).queryReqToDto(queryReq);
        verify(deviceChannelManager).get(testDTO);
        verify(deviceChannelWebAssembler).dtoToVo(testDTO);

        log.info("get方法测试通过");
    }

    @Test
    public void testGet_NotFound() {
        // Given
        when(deviceChannelWebAssembler.queryReqToDto(queryReq)).thenReturn(testDTO);
        when(deviceChannelManager.get(testDTO)).thenReturn(null);

        // When
        AjaxResult<DeviceChannelVO> result = deviceChannelController.get(queryReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertEquals("记录不存在", result.getMsg());

        log.info("get方法-记录不存在测试通过");
    }

    @Test
    public void testDeleteOne_Success() {
        // Given
        when(deviceChannelWebAssembler.updateReqToDto(updateReq)).thenReturn(testDTO);
        when(deviceChannelManager.deleteOne(testDTO)).thenReturn(true);

        // When
        AjaxResult<Void> result = deviceChannelController.deleteOne(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("删除成功", result.getMsg());

        // 验证方法调用
        verify(deviceChannelWebAssembler).updateReqToDto(updateReq);
        verify(deviceChannelManager).deleteOne(testDTO);

        log.info("deleteOne方法测试通过");
    }

    @Test
    public void testDeleteOne_Failed() {
        // Given
        when(deviceChannelWebAssembler.updateReqToDto(updateReq)).thenReturn(testDTO);
        when(deviceChannelManager.deleteOne(testDTO)).thenReturn(false);

        // When
        AjaxResult<Void> result = deviceChannelController.deleteOne(updateReq);

        // Then
        assertNotNull(result);
        assertNotEquals(0, result.getCode());
        assertEquals("删除失败", result.getMsg());

        log.info("deleteOne方法-删除失败测试通过");
    }

    @Test
    public void testDeleteBatch_Success() {
        // Given
        when(deviceChannelWebAssembler.queryReqToDto(queryReq)).thenReturn(testDTO);
        when(deviceChannelManager.deleteBatch(testDTO)).thenReturn(true);

        // When
        AjaxResult<Void> result = deviceChannelController.deleteBatch(queryReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals("批量删除成功", result.getMsg());

        // 验证方法调用
        verify(deviceChannelWebAssembler).queryReqToDto(queryReq);
        verify(deviceChannelManager).deleteBatch(testDTO);

        log.info("deleteBatch方法测试通过");
    }

    @Test
    public void testGetPage_Success() {
        // Given
        Page<DeviceChannelDTO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(2L);

        List<DeviceChannelDTO> records = new ArrayList<>();
        records.add(testDTO);
        records.add(testDTO);
        mockPage.setRecords(records);

        when(deviceChannelWebAssembler.queryReqToDto(queryReq)).thenReturn(testDTO);
        when(deviceChannelManager.getPage(testDTO, 1, 10)).thenReturn(mockPage);
        when(deviceChannelWebAssembler.dtoToVo(testDTO)).thenReturn(testVO);

        // When
        AjaxResult<DeviceChannelListResp> result = deviceChannelController.getPage(queryReq, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());

        DeviceChannelListResp resp = result.getData();
        assertEquals(2L, resp.getTotal());
        assertEquals(2, resp.getItems().size());

        // 验证方法调用
        verify(deviceChannelWebAssembler).queryReqToDto(queryReq);
        verify(deviceChannelManager).getPage(testDTO, 1, 10);
        verify(deviceChannelWebAssembler, times(2)).dtoToVo(testDTO);

        log.info("getPage方法测试通过");
    }

    @Test
    public void testGetPage_EmptyQuery() {
        // Given
        Page<DeviceChannelDTO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(0L);
        mockPage.setRecords(new ArrayList<>());

        when(deviceChannelWebAssembler.queryReqToDto(null)).thenReturn(new DeviceChannelDTO());
        when(deviceChannelManager.getPage(any(DeviceChannelDTO.class), eq(1), eq(10))).thenReturn(mockPage);

        // When
        AjaxResult<DeviceChannelListResp> result = deviceChannelController.getPage(null, 1, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertEquals(0, result.getData().getItems().size());

        log.info("getPage方法-空查询测试通过");
    }

    // ================================
    // 增强业务方法测试
    // ================================

    @Test
    public void testDeleteDeviceChannel_Success() {
        // Given
        when(deviceChannelWebAssembler.updateReqToDto(updateReq)).thenReturn(testDTO);
        when(deviceChannelManager.deleteDeviceChannel(TEST_DEVICE_ID, TEST_CHANNEL_ID)).thenReturn(true);

        // When
        AjaxResult<Boolean> result = deviceChannelController.deleteDeviceChannel(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(true, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).updateReqToDto(updateReq);
        verify(deviceChannelManager).deleteDeviceChannel(TEST_DEVICE_ID, TEST_CHANNEL_ID);

        log.info("deleteDeviceChannel方法测试通过");
    }

    @Test
    public void testCreateDeviceChannel_Success() {
        // Given
        when(deviceChannelWebAssembler.createReqToDto(createReq)).thenReturn(testDTO);
        when(deviceChannelManager.createDeviceChannel(testDTO)).thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = deviceChannelController.createDeviceChannel(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).createReqToDto(createReq);
        verify(deviceChannelManager).createDeviceChannel(testDTO);

        log.info("createDeviceChannel方法测试通过");
    }

    @Test
    public void testUpdateDeviceChannel_Success() {
        // Given
        when(deviceChannelWebAssembler.updateReqToDto(updateReq)).thenReturn(testDTO);
        when(deviceChannelManager.updateDeviceChannel(testDTO)).thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = deviceChannelController.updateDeviceChannel(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).updateReqToDto(updateReq);
        verify(deviceChannelManager).updateDeviceChannel(testDTO);

        log.info("updateDeviceChannel方法测试通过");
    }

    // ================================
    // 兼容历史接口测试
    // ================================

    @Test
    public void testGetById_Success() {
        // Given
        when(deviceChannelWebAssembler.queryReqToDto(any(DeviceChannelQueryReq.class))).thenReturn(testDTO);
        when(deviceChannelManager.get(testDTO)).thenReturn(testDTO);
        when(deviceChannelWebAssembler.dtoToVo(testDTO)).thenReturn(testVO);

        // When
        AjaxResult<DeviceChannelVO> result = deviceChannelController.getById(TEST_ID);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(testVO, result.getData());

        // 验证方法调用
        verify(deviceChannelWebAssembler).queryReqToDto(any(DeviceChannelQueryReq.class));
        verify(deviceChannelManager).get(testDTO);
        verify(deviceChannelWebAssembler).dtoToVo(testDTO);

        log.info("getById方法测试通过");
    }

    @Test
    public void testInsert_Success() {
        // Given
        when(deviceChannelWebAssembler.createReqToDto(createReq)).thenReturn(testDTO);
        when(deviceChannelManager.add(testDTO)).thenReturn(TEST_ID);

        // When
        AjaxResult<Long> result = deviceChannelController.insert(createReq);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertEquals(TEST_ID, result.getData());

        log.info("insert方法测试通过");
    }

    @Test
    public void testInsertBatch_Success() {
        // Given
        List<DeviceChannelCreateReq> createReqList = new ArrayList<>();
        createReqList.add(createReq);
        createReqList.add(createReq);

        List<DeviceChannelDTO> dtoList = new ArrayList<>();
        dtoList.add(testDTO);
        dtoList.add(testDTO);

        when(deviceChannelWebAssembler.toDeviceChannelDTOList(createReqList)).thenReturn(dtoList);
        when(deviceChannelManager.batchCreateDeviceChannel(dtoList)).thenReturn(2);

        // When
        AjaxResult<String> result = deviceChannelController.insertBatch(createReqList);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertTrue(result.getData().contains("成功创建 2 个设备通道"));

        // 验证方法调用
        verify(deviceChannelWebAssembler).toDeviceChannelDTOList(createReqList);
        verify(deviceChannelManager).batchCreateDeviceChannel(dtoList);

        log.info("insertBatch方法测试通过");
    }

    @Test
    public void testListPage_Success() {
        // Given
        Page<DeviceChannelDTO> mockPage = new Page<>(1, 10);
        mockPage.setTotal(1L);
        mockPage.setRecords(List.of(testDTO));

        when(deviceChannelWebAssembler.queryReqToDto(null)).thenReturn(new DeviceChannelDTO());
        when(deviceChannelManager.getPage(any(DeviceChannelDTO.class), eq(1), eq(10))).thenReturn(mockPage);
        when(deviceChannelWebAssembler.dtoToVo(testDTO)).thenReturn(testVO);

        // When
        AjaxResult<DeviceChannelListResp> result = deviceChannelController.listPage(1, 10);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getItems().size());

        log.info("listPage方法测试通过");
    }

    // ================================
    // 数据转换和验证测试
    // ================================

    @Test
    public void testDataConversion() {
        // 测试关键的数据转换调用模式

        // 1. CreateReq -> DTO
        deviceChannelController.add(createReq);
        verify(deviceChannelWebAssembler).createReqToDto(createReq);

        // 2. UpdateReq -> DTO
        deviceChannelController.update(updateReq);
        verify(deviceChannelWebAssembler).updateReqToDto(updateReq);

        // 3. QueryReq -> DTO 和 DTO -> VO（一次调用测试两个转换）
        when(deviceChannelManager.get(any())).thenReturn(testDTO);
        deviceChannelController.get(queryReq);
        verify(deviceChannelWebAssembler).queryReqToDto(queryReq);
        verify(deviceChannelWebAssembler).dtoToVo(testDTO);

        log.info("数据转换测试通过");
    }

    @Test
    public void testAjaxResultFormat() {
        // 测试AjaxResult格式的一致性

        when(deviceChannelWebAssembler.createReqToDto(createReq)).thenReturn(testDTO);
        when(deviceChannelManager.add(testDTO)).thenReturn(TEST_ID);

        AjaxResult<Long> result = deviceChannelController.add(createReq);

        // 验证AjaxResult格式
        assertNotNull(result);
        assertEquals(0, result.getCode()); // 成功响应码为0
        assertNotNull(result.getData()); // 有返回数据
        assertNotNull(result.getMsg()); // 有消息内容

        log.info("AjaxResult格式测试通过");
    }

    @Test
    public void testErrorHandling() {
        // 测试错误处理模式

        when(deviceChannelWebAssembler.queryReqToDto(queryReq)).thenReturn(testDTO);
        when(deviceChannelManager.get(testDTO)).thenReturn(null);

        AjaxResult<DeviceChannelVO> result = deviceChannelController.get(queryReq);

        // 验证错误响应格式
        assertNotNull(result);
        assertNotEquals(0, result.getCode()); // 错误响应码非0
        assertEquals("记录不存在", result.getMsg());
        assertNull(result.getData()); // 错误时数据为null

        log.info("错误处理测试通过");
    }
}