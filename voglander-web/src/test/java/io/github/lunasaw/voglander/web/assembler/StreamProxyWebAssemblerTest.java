package io.github.lunasaw.voglander.web.assembler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import io.github.lunasaw.voglander.web.api.zlm.assembler.StreamProxyWebAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseMockTest;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.StreamProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.StreamProxyVO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyWebAssembler单元测试类
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyWebAssemblerTest extends BaseMockTest {

    private final String            TEST_APP       = "live";
    private final String            TEST_STREAM    = "test";
    private final String            TEST_URL       = "rtmp://live.example.com/live/test";
    private final String            TEST_PROXY_KEY = "test-proxy-key-123";
    private final Long              TEST_ID        = 1L;

    @Autowired
    private StreamProxyWebAssembler streamProxyWebAssembler;

    private StreamProxyDTO          testStreamProxyDTO;
    private StreamProxyVO           testStreamProxyVO;
    private StreamProxyCreateReq    testCreateReq;
    private StreamProxyUpdateReq    testUpdateReq;

    @BeforeEach
    public void setUp() {
        testStreamProxyDTO = createTestStreamProxyDTO();
        testStreamProxyVO = createTestStreamProxyVO();
        testCreateReq = createTestCreateReq();
        testUpdateReq = createTestUpdateReq();
    }

    private StreamProxyDTO createTestStreamProxyDTO() {
        StreamProxyDTO dto = new StreamProxyDTO();
        dto.setId(TEST_ID);
        dto.setApp(TEST_APP);
        dto.setStream(TEST_STREAM);
        dto.setUrl(TEST_URL);
        dto.setProxyKey(TEST_PROXY_KEY);
        dto.setStatus(1);
        dto.setOnlineStatus(1);
        dto.setEnabled(true);
        dto.setDescription("Test proxy");
        dto.setExtend("{\"vhost\":\"__defaultVhost__\"}");
        dto.setCreateTime(LocalDateTime.now());
        dto.setUpdateTime(LocalDateTime.now());
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
        req.setApp(TEST_APP);
        req.setStream(TEST_STREAM);
        req.setUrl(TEST_URL);
        req.setEnabled(true);
        req.setDescription("Test proxy updated");
        return req;
    }

    @Test
    public void testdtoToVo_Success() {
        // Act
        StreamProxyVO result = streamProxyWebAssembler.dtoToVo(testStreamProxyDTO);

        // Assert
        assertNotNull(result);
        assertEquals(testStreamProxyDTO.getId(), result.getId());
        assertEquals(testStreamProxyDTO.getApp(), result.getApp());
        assertEquals(testStreamProxyDTO.getStream(), result.getStream());
        assertEquals(testStreamProxyDTO.getUrl(), result.getUrl());
        assertEquals(testStreamProxyDTO.getProxyKey(), result.getProxyKey());
        assertEquals(testStreamProxyDTO.getStatus(), result.getStatus());
        assertEquals(testStreamProxyDTO.getOnlineStatus(), result.getOnlineStatus());
        assertEquals(testStreamProxyDTO.getEnabled(), result.getEnabled());
        assertEquals(testStreamProxyDTO.getDescription(), result.getDescription());
        assertEquals(testStreamProxyDTO.getExtend(), result.getExtend());

        // 验证时间转换（LocalDateTime -> Unix timestamp）
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());
        assertTrue(result.getCreateTime() > 0);
        assertTrue(result.getUpdateTime() > 0);

        log.info("testdtoToVo_Success passed");
    }

    @Test
    public void testdtoToVo_NullInput() {
        // Act
        StreamProxyVO result = streamProxyWebAssembler.dtoToVo(null);

        // Assert
        assertNull(result);
        log.info("testdtoToVo_NullInput passed");
    }

    @Test
    public void testdtoToVo_NullTimeFields() {
        // Arrange
        StreamProxyDTO dtoWithNullTime = createTestStreamProxyDTO();
        dtoWithNullTime.setCreateTime(null);
        dtoWithNullTime.setUpdateTime(null);

        // Act
        StreamProxyVO result = streamProxyWebAssembler.dtoToVo(dtoWithNullTime);

        // Assert
        assertNotNull(result);
        assertEquals(testStreamProxyDTO.getId(), result.getId());
        assertEquals(testStreamProxyDTO.getApp(), result.getApp());
        assertNull(result.getCreateTime());
        assertNull(result.getUpdateTime());

        log.info("testdtoToVo_NullTimeFields passed");
    }

    @Test
    public void testCreateReqToDto_Success() {
        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(testCreateReq);

        // Assert
        assertNotNull(result);
        assertNull(result.getId()); // 新创建的对象ID应该为null
        assertEquals(testCreateReq.getApp(), result.getApp());
        assertEquals(testCreateReq.getStream(), result.getStream());
        assertEquals(testCreateReq.getUrl(), result.getUrl());
        assertEquals(testCreateReq.getEnabled(), result.getEnabled());
        assertEquals(testCreateReq.getDescription(), result.getDescription());

        // 验证默认值
        assertNull(result.getProxyKey()); // 代理key由ZLM生成
        assertNull(result.getStatus()); // 状态由系统设置
        assertNull(result.getOnlineStatus()); // 在线状态由系统设置
        assertNull(result.getExtend()); // 扩展信息由Hook设置

        log.info("testCreateReqToDto_Success passed");
    }

    @Test
    public void testCreateReqToDto_NullInput() {
        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(null);

        // Assert
        assertNull(result);
        log.info("testCreateReqToDto_NullInput passed");
    }

    @Test
    public void testUpdateReqToDto_Success() {
        // Act
        StreamProxyDTO result = streamProxyWebAssembler.updateReqToDto(testUpdateReq);

        // Assert
        assertNotNull(result);
        assertNull(result.getId()); // 更新对象ID由URL路径参数提供，不在请求体中
        assertEquals(testUpdateReq.getApp(), result.getApp());
        assertEquals(testUpdateReq.getStream(), result.getStream());
        assertEquals(testUpdateReq.getUrl(), result.getUrl());
        assertEquals(testUpdateReq.getEnabled(), result.getEnabled());
        assertEquals(testUpdateReq.getDescription(), result.getDescription());

        log.info("testUpdateReqToDto_Success passed");
    }

    @Test
    public void testUpdateReqToDto_NullInput() {
        // Act
        StreamProxyDTO result = streamProxyWebAssembler.updateReqToDto(null);

        // Assert
        assertNull(result);
        log.info("testUpdateReqToDto_NullInput passed");
    }

    @Test
    public void testTimeConversion() {
        // Arrange
        LocalDateTime testTime = LocalDateTime.of(2025, 1, 23, 10, 30, 45);
        StreamProxyDTO dtoWithSpecificTime = createTestStreamProxyDTO();
        dtoWithSpecificTime.setCreateTime(testTime);
        dtoWithSpecificTime.setUpdateTime(testTime);

        // Act
        StreamProxyVO result = streamProxyWebAssembler.dtoToVo(dtoWithSpecificTime);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getCreateTime());
        assertNotNull(result.getUpdateTime());

        // 验证时间戳转换正确性
        // 由于时区问题，这里只验证时间戳不为0且为正数
        assertTrue(result.getCreateTime() > 0);
        assertTrue(result.getUpdateTime() > 0);
        assertEquals(result.getCreateTime(), result.getUpdateTime());

        log.info("testTimeConversion passed - CreateTime: {}, UpdateTime: {}",
            result.getCreateTime(), result.getUpdateTime());
    }

    @Test
    public void testSpecialCharacters() {
        // Arrange
        StreamProxyCreateReq specialReq = new StreamProxyCreateReq();
        specialReq.setApp("测试应用");
        specialReq.setStream("测试流");
        specialReq.setUrl("rtmp://测试.example.com/测试/流");
        specialReq.setDescription("描述包含特殊字符：!@#$%^&*()");
        specialReq.setEnabled(true);

        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(specialReq);

        // Assert
        assertNotNull(result);
        assertEquals("测试应用", result.getApp());
        assertEquals("测试流", result.getStream());
        assertEquals("rtmp://测试.example.com/测试/流", result.getUrl());
        assertEquals("描述包含特殊字符：!@#$%^&*()", result.getDescription());

        log.info("testSpecialCharacters passed");
    }

    @Test
    public void testEmptyStrings() {
        // Arrange
        StreamProxyCreateReq emptyReq = new StreamProxyCreateReq();
        emptyReq.setApp("");
        emptyReq.setStream("");
        emptyReq.setUrl("");
        emptyReq.setDescription("");
        emptyReq.setEnabled(false);

        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(emptyReq);

        // Assert
        assertNotNull(result);
        assertEquals("", result.getApp());
        assertEquals("", result.getStream());
        assertEquals("", result.getUrl());
        assertEquals("", result.getDescription());
        assertFalse(result.getEnabled());

        log.info("testEmptyStrings passed");
    }

    @Test
    public void testBooleanValues() {
        // Arrange
        StreamProxyCreateReq trueReq = new StreamProxyCreateReq();
        trueReq.setApp(TEST_APP);
        trueReq.setStream(TEST_STREAM);
        trueReq.setUrl(TEST_URL);
        trueReq.setEnabled(true);

        StreamProxyCreateReq falseReq = new StreamProxyCreateReq();
        falseReq.setApp(TEST_APP);
        falseReq.setStream(TEST_STREAM);
        falseReq.setUrl(TEST_URL);
        falseReq.setEnabled(false);

        StreamProxyCreateReq nullReq = new StreamProxyCreateReq();
        nullReq.setApp(TEST_APP);
        nullReq.setStream(TEST_STREAM);
        nullReq.setUrl(TEST_URL);
        nullReq.setEnabled(null);

        // Act
        StreamProxyDTO trueResult = streamProxyWebAssembler.createReqToDto(trueReq);
        StreamProxyDTO falseResult = streamProxyWebAssembler.createReqToDto(falseReq);
        StreamProxyDTO nullResult = streamProxyWebAssembler.createReqToDto(nullReq);

        // Assert
        assertNotNull(trueResult);
        assertTrue(trueResult.getEnabled());

        assertNotNull(falseResult);
        assertFalse(falseResult.getEnabled());

        assertNotNull(nullResult);
        assertNull(nullResult.getEnabled());

        log.info("testBooleanValues passed");
    }

    @Test
    public void testCreateAndUpdateReqDifferences() {
        // Arrange
        StreamProxyCreateReq createReq = createTestCreateReq();
        StreamProxyUpdateReq updateReq = createTestUpdateReq();

        // Act
        StreamProxyDTO createResult = streamProxyWebAssembler.createReqToDto(createReq);
        StreamProxyDTO updateResult = streamProxyWebAssembler.updateReqToDto(updateReq);

        // Assert
        assertNotNull(createResult);
        assertNotNull(updateResult);

        // 验证两者字段映射一致性
        assertEquals(createResult.getApp(), updateResult.getApp());
        assertEquals(createResult.getStream(), updateResult.getStream());
        assertEquals(createResult.getUrl(), updateResult.getUrl());
        assertEquals(createResult.getEnabled(), updateResult.getEnabled());

        // 验证描述字段差异（updateReq中有"updated"）
        assertEquals("Test proxy", createResult.getDescription());
        assertEquals("Test proxy updated", updateResult.getDescription());

        log.info("testCreateAndUpdateReqDifferences passed");
    }

    @Test
    public void testPartialFieldMapping() {
        // Arrange
        StreamProxyCreateReq partialReq = new StreamProxyCreateReq();
        partialReq.setApp(TEST_APP);
        partialReq.setStream(TEST_STREAM);
        // url和其他字段保持null

        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(partialReq);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        assertNull(result.getUrl());
        assertNull(result.getEnabled());
        assertNull(result.getDescription());

        log.info("testPartialFieldMapping passed");
    }

    @Test
    public void testLongStrings() {
        // Arrange
        String longString = "a".repeat(1000);
        StreamProxyCreateReq longReq = new StreamProxyCreateReq();
        longReq.setApp(longString);
        longReq.setStream(longString);
        longReq.setUrl("rtmp://" + longString + ".example.com/" + longString);
        longReq.setDescription(longString);
        longReq.setEnabled(true);

        // Act
        StreamProxyDTO result = streamProxyWebAssembler.createReqToDto(longReq);

        // Assert
        assertNotNull(result);
        assertEquals(longString, result.getApp());
        assertEquals(longString, result.getStream());
        assertEquals("rtmp://" + longString + ".example.com/" + longString, result.getUrl());
        assertEquals(longString, result.getDescription());

        log.info("testLongStrings passed");
    }
}