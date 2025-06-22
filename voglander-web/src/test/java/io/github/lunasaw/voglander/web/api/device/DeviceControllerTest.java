package io.github.lunasaw.voglander.web.api.device;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.manager.service.DeviceService;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.web.api.device.assembler.DeviceWebAssembler;
import io.github.lunasaw.voglander.web.api.device.req.DeviceCreateReq;
import io.github.lunasaw.voglander.web.api.device.req.DeviceUpdateReq;
import lombok.extern.slf4j.Slf4j;

/**
 * DeviceController 集成测试类
 *
 * @author luna
 * @date 2024/01/30
 */
@Slf4j
@SpringBootTest(classes = {
    io.github.lunasaw.voglander.web.ApplicationWeb.class,
    io.github.lunasaw.voglander.config.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.cache.type=simple",
    "sip.enable=false",
    "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
// @Transactional - 移除事务注解避免事务隔离问题
public class DeviceControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc               mockMvc;

    @Autowired
    private ObjectMapper          objectMapper;

    @Autowired
    private DeviceManager         deviceManager;

    @Autowired
    private TransactionTemplate   transactionTemplate;

    private final String          BASE_URL       = ApiConstant.API_INDEX_V1 + "/device";
    private final String          TEST_DEVICE_ID = "TEST_DEVICE_001";

    private DeviceDTO             testDeviceDTO;
    private DeviceDO              testDeviceDO;
    private DeviceCreateReq       testCreateReq;
    private DeviceUpdateReq       testUpdateReq;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // 清理测试数据 - 确保测试环境干净
        cleanupTestData();

        testDeviceDTO = createTestDeviceDTO();
        testDeviceDO = createTestDeviceDO();
        testCreateReq = createTestCreateReq();
        testUpdateReq = createTestUpdateReq();
    }

    @AfterEach
    public void tearDown() {
        // 测试结束后清理数据，避免影响其他测试
        cleanupTestData();
    }

    /**
     * 清理测试数据，确保测试环境干净
     */
    private void cleanupTestData() {
        try {
            // 删除可能存在的测试设备
            deviceManager.deleteDevice(TEST_DEVICE_ID);
            deviceManager.deleteDevice(TEST_DEVICE_ID + "_UPDATE");
            deviceManager.deleteDevice(TEST_DEVICE_ID + "_BATCH");
            deviceManager.deleteDevice(TEST_DEVICE_ID + "_2");
            log.info("Test data cleanup completed");
        } catch (Exception e) {
            // 清理失败不影响测试执行，只记录日志
            log.debug("Test data cleanup failed: {}", e.getMessage());
        }
    }

    /**
     * 创建测试用的 DeviceDTO
     */
    private DeviceDTO createTestDeviceDTO() {
        return createTestDeviceDTO(TEST_DEVICE_ID);
    }

    /**
     * 创建测试用的 DeviceDTO，指定设备ID
     *
     * @param deviceId 设备ID
     * @return DeviceDTO
     */
    private DeviceDTO createTestDeviceDTO(String deviceId) {
        DeviceDTO dto = new DeviceDTO();
        dto.setDeviceId(deviceId);
        dto.setName("测试设备");
        dto.setIp("192.168.1.100");
        dto.setPort(5060);
        dto.setStatus(1);
        dto.setType(1);
        dto.setServerIp("192.168.1.1");
        dto.setCreateTime(new Date());
        dto.setUpdateTime(new Date());
        dto.setRegisterTime(new Date());
        dto.setKeepaliveTime(new Date());

        DeviceDTO.ExtendInfo extendInfo = new DeviceDTO.ExtendInfo();
        extendInfo.setTransport("UDP");
        extendInfo.setExpires(3600);
        extendInfo.setCharset("UTF-8");
        dto.setExtendInfo(extendInfo);

        return dto;
    }

    /**
     * 创建测试用的 DeviceDO
     */
    private DeviceDO createTestDeviceDO() {
        DeviceDO deviceDO = new DeviceDO();
        deviceDO.setDeviceId(TEST_DEVICE_ID);
        deviceDO.setName("测试设备");
        deviceDO.setIp("192.168.1.100");
        deviceDO.setPort(5060);
        deviceDO.setStatus(1);
        deviceDO.setType(1);
        deviceDO.setServerIp("192.168.1.1");
        deviceDO.setCreateTime(new Date());
        deviceDO.setUpdateTime(new Date());
        deviceDO.setRegisterTime(new Date());
        deviceDO.setKeepaliveTime(new Date());
        return deviceDO;
    }

    /**
     * 创建测试用的 DeviceCreateReq
     */
    private DeviceCreateReq createTestCreateReq() {
        DeviceCreateReq req = new DeviceCreateReq();
        req.setDeviceId(TEST_DEVICE_ID);
        req.setName("测试设备");
        req.setIp("192.168.1.100");
        req.setPort(5060);
        req.setType(1);
        req.setServerIp("192.168.1.1");

        DeviceCreateReq.ExtendInfoReq extendInfo = new DeviceCreateReq.ExtendInfoReq();
        extendInfo.setTransport("UDP");
        extendInfo.setExpires(3600);
        extendInfo.setCharset("UTF-8");
        req.setExtendInfo(extendInfo);

        return req;
    }

    /**
     * 创建测试用的 DeviceUpdateReq
     */
    private DeviceUpdateReq createTestUpdateReq() {
        DeviceUpdateReq req = new DeviceUpdateReq();
        req.setDeviceId(TEST_DEVICE_ID + "_UPDATE");
        req.setName("更新后的测试设备");
        req.setIp("192.168.1.101");
        req.setPort(5061);
        req.setType(1);
        req.setStatus(1);
        req.setServerIp("192.168.1.1");

        DeviceUpdateReq.ExtendInfoReq extendInfo = new DeviceUpdateReq.ExtendInfoReq();
        extendInfo.setTransport("TCP");
        extendInfo.setExpires(7200);
        extendInfo.setCharset("UTF-8");
        req.setExtendInfo(extendInfo);

        return req;
    }

    @Test
    public void testGetById_Success() throws Exception {
        // Given - 在独立事务中创建测试设备，确保数据提交
        Long deviceId = deviceManager.createDevice(testDeviceDTO);

        // When & Then - 测试根据ID查询设备
        mockMvc.perform(get(BASE_URL + "/get/{id}", deviceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.id").value(deviceId))
            .andExpect(jsonPath("$.data.deviceId").value(TEST_DEVICE_ID))
            .andExpect(jsonPath("$.data.name").value("测试设备"));

        log.info("testGetById_Success passed - 集成测试成功");
    }

    @Test
    public void testGetById_NotFound() throws Exception {
        // When & Then - 测试查询不存在的设备
        mockMvc.perform(get(BASE_URL + "/get/{id}", 999L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("设备不存在"));

        log.info("testGetById_NotFound passed");
    }

    @Test
    public void testGetByEntity_Success() throws Exception {
        // Given - 在独立事务中创建测试设备
        transactionTemplate.execute(status -> {
            deviceManager.createDevice(testDeviceDTO);
            return null;
        });

        // When & Then - 测试根据条件查询设备
        mockMvc.perform(get(BASE_URL + "/get")
            .param("deviceId", TEST_DEVICE_ID)
            .param("name", "测试设备"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.deviceId").value(TEST_DEVICE_ID));

        log.info("testGetByEntity_Success passed");
    }

    @Test
    public void testGetByEntity_NotFound() throws Exception {
        // When & Then - 测试查询不存在的设备
        mockMvc.perform(get(BASE_URL + "/get")
            .param("deviceId", "NOT_EXIST_DEVICE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500))
            .andExpect(jsonPath("$.msg").value("设备不存在"));

        log.info("testGetByEntity_NotFound passed");
    }

    @Test
    public void testList_Success() throws Exception {
        // Given - 在独立事务中创建测试设备
        transactionTemplate.execute(status -> {
            deviceManager.createDevice(testDeviceDTO);
            return null;
        });

        // When & Then - 测试列表查询
        mockMvc.perform(get(BASE_URL + "/list")
            .param("status", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));

        log.info("testList_Success passed");
    }

    @Test
    public void testList_Empty() throws Exception {
        // When & Then - 测试空列表查询
        mockMvc.perform(get(BASE_URL + "/list")
            .param("status", "999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", hasSize(0)));

        log.info("testList_Empty passed");
    }

    @Test
    public void testPageList_Success() throws Exception {
        // Given - 在独立事务中创建测试设备
        transactionTemplate.execute(status -> {
            deviceManager.createDevice(testDeviceDTO);
            return null;
        });

        // When & Then - 测试分页查询
        mockMvc.perform(get(BASE_URL + "/pageList/{page}/{size}", 1, 10))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.records", hasSize(greaterThanOrEqualTo(1))));

        log.info("testPageList_Success passed");
    }

    @Test
    public void testInsert_Success() throws Exception {
        // When & Then - 测试创建设备
        mockMvc.perform(post(BASE_URL + "/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testCreateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isNumber());

        log.info("testInsert_Success passed");
    }

    @Test
    public void testInsert_DuplicateDevice() throws Exception {
        // Given - 在独立事务中先创建一个设备
        transactionTemplate.execute(status -> {
            deviceManager.createDevice(testDeviceDTO);
            return null;
        });

        // When & Then - 测试创建重复设备
        mockMvc.perform(post(BASE_URL + "/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testCreateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        log.info("testInsert_DuplicateDevice passed");
    }

    @Test
    public void testInsertBatch_Success() throws Exception {
        // Given - 准备批量创建数据
        DeviceCreateReq req2 = createTestCreateReq();
        req2.setDeviceId(TEST_DEVICE_ID + "_BATCH");
        List<DeviceCreateReq> createReqList = Arrays.asList(testCreateReq, req2);

        // When & Then - 测试批量创建
        mockMvc.perform(post(BASE_URL + "/insertBatch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(createReqList)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", containsString("成功创建")));

        log.info("testInsertBatch_Success passed");
    }

    @Test
    public void testUpdate_Success() throws Exception {
        // Given - 先创建一个设备
        Long deviceId = deviceManager.createDevice(testDeviceDTO);
        testUpdateReq.setId(deviceId);

        // When & Then - 测试更新设备
        mockMvc.perform(put(BASE_URL + "/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testUpdateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(deviceId));

        log.info("testUpdate_Success passed");
    }

    @Test
    public void testUpdate_NotFound() throws Exception {
        // Given - 设置不存在的设备ID
        testUpdateReq.setId(999L);

        // When & Then - 测试更新不存在的设备
        mockMvc.perform(put(BASE_URL + "/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(testUpdateReq)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(500));

        log.info("testUpdate_NotFound passed");
    }

    @Test
    public void testUpdateBatch_Success() throws Exception {
        // Given - 先创建设备
        Long deviceId = deviceManager.createDevice(testDeviceDTO);
        testUpdateReq.setId(deviceId);
        List<DeviceUpdateReq> updateReqList = Arrays.asList(testUpdateReq);

        // When & Then - 测试批量更新
        mockMvc.perform(put(BASE_URL + "/updateBatch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updateReqList)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", containsString("成功更新")));

        log.info("testUpdateBatch_Success passed");
    }

    @Test
    public void testDeleteOne_Success() throws Exception {
        // Given - 先创建一个设备
        Long deviceId = deviceManager.createDevice(testDeviceDTO);

        // When & Then - 测试删除设备
        mockMvc.perform(delete(BASE_URL + "/delete/{id}", deviceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(true));

        log.info("testDeleteOne_Success passed");
    }

    @Test
    public void testDeleteOne_NotFound() throws Exception {
        // When & Then - 测试删除不存在的设备
        mockMvc.perform(delete(BASE_URL + "/delete/{id}", 999L))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(false));

        log.info("testDeleteOne_NotFound passed");
    }

    @Test
    public void testDeleteBatch_Success() throws Exception {
        // Given - 先创建几个设备
        Long deviceId1 = deviceManager.createDevice(testDeviceDTO);

        // 创建第二个设备DTO，使用不同的设备ID
        DeviceDTO testDeviceDTO2 = createTestDeviceDTO(TEST_DEVICE_ID + "_2");
        Long deviceId2 = deviceManager.createDevice(testDeviceDTO2);

        List<Long> ids = Arrays.asList(deviceId1, deviceId2);

        // When & Then - 测试批量删除
        mockMvc.perform(delete(BASE_URL + "/deleteIds")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(ids)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").value(true));

        log.info("testDeleteBatch_Success passed");
    }

    @Test
    public void testGetAccount_Success() throws Exception {
        // Given - 先创建一个设备
        deviceManager.createDevice(testDeviceDTO);

        // When & Then - 测试统计设备数量
        mockMvc.perform(get(BASE_URL + "/count"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", greaterThanOrEqualTo(1)));

        log.info("testGetAccount_Success passed");
    }

    @Test
    public void testGetAccountByEntity_Success() throws Exception {
        // Given - 先创建一个设备
        deviceManager.createDevice(testDeviceDTO);

        // When & Then - 测试按条件统计设备数量
        mockMvc.perform(get(BASE_URL + "/countByEntity")
            .param("status", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data", greaterThanOrEqualTo(1)));

        log.info("testGetAccountByEntity_Success passed");
    }
}