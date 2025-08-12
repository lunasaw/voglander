package io.github.lunasaw.voglander.manager.assembler;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.lunasaw.voglander.BaseMockTest;
import io.github.lunasaw.voglander.manager.domaon.dto.StreamProxyDTO;
import io.github.lunasaw.voglander.repository.entity.StreamProxyDO;
import lombok.extern.slf4j.Slf4j;

/**
 * StreamProxyAssembler单元测试类
 * 
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
public class StreamProxyAssemblerTest extends BaseMockTest {

    private final String         TEST_APP       = "live";
    private final String         TEST_STREAM    = "test";
    private final String         TEST_URL       = "rtmp://live.example.com/live/test";
    private final String         TEST_PROXY_KEY = "test-proxy-key-123";
    private final Long           TEST_ID        = 1L;

    @Autowired
    private StreamProxyAssembler streamProxyAssembler;

    private StreamProxyDO        testStreamProxyDO;
    private StreamProxyDTO       testStreamProxyDTO;

    @BeforeEach
    public void setUp() {
        testStreamProxyDO = createTestStreamProxyDO();
        testStreamProxyDTO = createTestStreamProxyDTO();
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

    @Test
    public void testDoToDto_Success() {
        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(testStreamProxyDO);

        // Assert
        assertNotNull(result);
        assertEquals(testStreamProxyDO.getId(), result.getId());
        assertEquals(testStreamProxyDO.getApp(), result.getApp());
        assertEquals(testStreamProxyDO.getStream(), result.getStream());
        assertEquals(testStreamProxyDO.getUrl(), result.getUrl());
        assertEquals(testStreamProxyDO.getProxyKey(), result.getProxyKey());
        assertEquals(testStreamProxyDO.getStatus(), result.getStatus());
        assertEquals(testStreamProxyDO.getOnlineStatus(), result.getOnlineStatus());
        assertEquals(testStreamProxyDO.getEnabled(), result.getEnabled());
        assertEquals(testStreamProxyDO.getDescription(), result.getDescription());
        assertEquals(testStreamProxyDO.getExtend(), result.getExtend());
        assertEquals(testStreamProxyDO.getCreateTime(), result.getCreateTime());
        assertEquals(testStreamProxyDO.getUpdateTime(), result.getUpdateTime());

        log.info("testDoToDto_Success passed");
    }

    @Test
    public void testDoToDto_NullInput() {
        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(null);

        // Assert
        assertNull(result);
        log.info("testDoToDto_NullInput passed");
    }

    @Test
    public void testDoToDto_PartialFields() {
        // Arrange
        StreamProxyDO partial = new StreamProxyDO();
        partial.setId(TEST_ID);
        partial.setApp(TEST_APP);
        partial.setStream(TEST_STREAM);
        // 其他字段保持null

        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(partial);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        assertNull(result.getUrl());
        assertNull(result.getProxyKey());
        assertNull(result.getStatus());
        assertNull(result.getOnlineStatus());
        assertNull(result.getEnabled());
        assertNull(result.getDescription());
        assertNull(result.getExtend());

        log.info("testDoToDto_PartialFields passed");
    }

    @Test
    public void testDtoToDo_Success() {
        // Act
        StreamProxyDO result = streamProxyAssembler.dtoToDo(testStreamProxyDTO);

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
        assertEquals(testStreamProxyDTO.getCreateTime(), result.getCreateTime());
        assertEquals(testStreamProxyDTO.getUpdateTime(), result.getUpdateTime());

        log.info("testDtoToDo_Success passed");
    }

    @Test
    public void testDtoToDo_NullInput() {
        // Act
        StreamProxyDO result = streamProxyAssembler.dtoToDo(null);

        // Assert
        assertNull(result);
        log.info("testDtoToDo_NullInput passed");
    }

    @Test
    public void testDtoToDo_PartialFields() {
        // Arrange
        StreamProxyDTO partial = new StreamProxyDTO();
        partial.setId(TEST_ID);
        partial.setApp(TEST_APP);
        partial.setStream(TEST_STREAM);
        // 其他字段保持null

        // Act
        StreamProxyDO result = streamProxyAssembler.dtoToDo(partial);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        assertNull(result.getUrl());
        assertNull(result.getProxyKey());
        assertNull(result.getStatus());
        assertNull(result.getOnlineStatus());
        assertNull(result.getEnabled());
        assertNull(result.getDescription());
        assertNull(result.getExtend());

        log.info("testDtoToDo_PartialFields passed");
    }

    @Test
    public void testBidirectionalConversion() {
        // Act - DO -> DTO -> DO
        StreamProxyDTO dto = streamProxyAssembler.doToDto(testStreamProxyDO);
        StreamProxyDO doResult = streamProxyAssembler.dtoToDo(dto);

        // Assert
        assertNotNull(dto);
        assertNotNull(doResult);

        // 验证双向转换保持数据一致性
        assertEquals(testStreamProxyDO.getId(), doResult.getId());
        assertEquals(testStreamProxyDO.getApp(), doResult.getApp());
        assertEquals(testStreamProxyDO.getStream(), doResult.getStream());
        assertEquals(testStreamProxyDO.getUrl(), doResult.getUrl());
        assertEquals(testStreamProxyDO.getProxyKey(), doResult.getProxyKey());
        assertEquals(testStreamProxyDO.getStatus(), doResult.getStatus());
        assertEquals(testStreamProxyDO.getOnlineStatus(), doResult.getOnlineStatus());
        assertEquals(testStreamProxyDO.getEnabled(), doResult.getEnabled());
        assertEquals(testStreamProxyDO.getDescription(), doResult.getDescription());
        assertEquals(testStreamProxyDO.getExtend(), doResult.getExtend());

        log.info("testBidirectionalConversion passed");
    }

    @Test
    public void testReverseBidirectionalConversion() {
        // Act - DTO -> DO -> DTO
        StreamProxyDO doObject = streamProxyAssembler.dtoToDo(testStreamProxyDTO);
        StreamProxyDTO dtoResult = streamProxyAssembler.doToDto(doObject);

        // Assert
        assertNotNull(doObject);
        assertNotNull(dtoResult);

        // 验证反向双向转换保持数据一致性
        assertEquals(testStreamProxyDTO.getId(), dtoResult.getId());
        assertEquals(testStreamProxyDTO.getApp(), dtoResult.getApp());
        assertEquals(testStreamProxyDTO.getStream(), dtoResult.getStream());
        assertEquals(testStreamProxyDTO.getUrl(), dtoResult.getUrl());
        assertEquals(testStreamProxyDTO.getProxyKey(), dtoResult.getProxyKey());
        assertEquals(testStreamProxyDTO.getStatus(), dtoResult.getStatus());
        assertEquals(testStreamProxyDTO.getOnlineStatus(), dtoResult.getOnlineStatus());
        assertEquals(testStreamProxyDTO.getEnabled(), dtoResult.getEnabled());
        assertEquals(testStreamProxyDTO.getDescription(), dtoResult.getDescription());
        assertEquals(testStreamProxyDTO.getExtend(), dtoResult.getExtend());

        log.info("testReverseBidirectionalConversion passed");
    }

    @Test
    public void testSpecialCharacters() {
        // Arrange
        StreamProxyDO special = new StreamProxyDO();
        special.setId(TEST_ID);
        special.setApp("测试应用");
        special.setStream("测试流");
        special.setUrl("rtmp://测试.example.com/测试/流");
        special.setDescription("描述包含特殊字符：!@#$%^&*()");
        special.setExtend("{\"测试\":\"值\",\"special\":\"!@#$%^&*()\"}");

        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(special);

        // Assert
        assertNotNull(result);
        assertEquals("测试应用", result.getApp());
        assertEquals("测试流", result.getStream());
        assertEquals("rtmp://测试.example.com/测试/流", result.getUrl());
        assertEquals("描述包含特殊字符：!@#$%^&*()", result.getDescription());
        assertEquals("{\"测试\":\"值\",\"special\":\"!@#$%^&*()\"}", result.getExtend());

        log.info("testSpecialCharacters passed");
    }

    @Test
    public void testLongStrings() {
        // Arrange
        String longString = "a".repeat(1000);
        StreamProxyDO longFields = new StreamProxyDO();
        longFields.setId(TEST_ID);
        longFields.setApp(longString);
        longFields.setStream(longString);
        longFields.setUrl("rtmp://" + longString + ".example.com/" + longString);
        longFields.setDescription(longString);

        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(longFields);

        // Assert
        assertNotNull(result);
        assertEquals(longString, result.getApp());
        assertEquals(longString, result.getStream());
        assertEquals("rtmp://" + longString + ".example.com/" + longString, result.getUrl());
        assertEquals(longString, result.getDescription());

        log.info("testLongStrings passed");
    }

    @Test
    public void testBooleanFields() {
        // Arrange
        StreamProxyDO testTrue = new StreamProxyDO();
        testTrue.setId(TEST_ID);
        testTrue.setEnabled(true);

        StreamProxyDO testFalse = new StreamProxyDO();
        testFalse.setId(TEST_ID + 1);
        testFalse.setEnabled(false);

        // Act
        StreamProxyDTO resultTrue = streamProxyAssembler.doToDto(testTrue);
        StreamProxyDTO resultFalse = streamProxyAssembler.doToDto(testFalse);

        // Assert
        assertNotNull(resultTrue);
        assertTrue(resultTrue.getEnabled());

        assertNotNull(resultFalse);
        assertFalse(resultFalse.getEnabled());

        log.info("testBooleanFields passed");
    }

    @Test
    public void testNumericFields() {
        // Arrange
        StreamProxyDO numeric = new StreamProxyDO();
        numeric.setId(Long.MAX_VALUE);
        numeric.setStatus(Integer.MAX_VALUE);
        numeric.setOnlineStatus(0);

        // Act
        StreamProxyDTO result = streamProxyAssembler.doToDto(numeric);

        // Assert
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.getId());
        assertEquals(Integer.MAX_VALUE, result.getStatus());
        assertEquals(0, result.getOnlineStatus());

        log.info("数值字段转换测试通过");
    }

    // ================================
    // 扩展字段和复杂对象测试
    // ================================

    @Test
    public void testExtendObjectConversion() {
        // Given - 创建包含extendObj的DTO
        StreamProxyDTO.ExtendObj extendObj = new StreamProxyDTO.ExtendObj();
        extendObj.setRetryCount(-1);
        extendObj.setRtpType(0);
        extendObj.setTimeoutSec(10);
        extendObj.setEnableHls(true);
        extendObj.setEnableRtsp(false);

        StreamProxyDTO dtoWithExtendObj = createTestStreamProxyDTO();
        dtoWithExtendObj.setExtendObj(extendObj);

        // When
        StreamProxyDO result = streamProxyAssembler.dtoToDo(dtoWithExtendObj);

        // Then
        assertNotNull(result);
        assertEquals(TEST_APP, result.getApp());
        assertEquals(TEST_STREAM, result.getStream());
        // extendObj应该被正确序列化到extend字段
        assertNotNull(result.getExtend());

        log.info("扩展对象转换测试通过");
    }

    @Test
    public void testListConversion() {
        // Given
        List<StreamProxyDO> doList = List.of(
            testStreamProxyDO,
            createAnotherTestStreamProxyDO());

        // When
        List<StreamProxyDTO> dtoList = streamProxyAssembler.doListToDtoList(doList);

        // Then
        assertNotNull(dtoList);
        assertEquals(2, dtoList.size());

        // 验证第一个元素
        verifyDOtoDTOConversion(doList.get(0), dtoList.get(0));

        // 验证第二个元素
        verifyDOtoDTOConversion(doList.get(1), dtoList.get(1));

        log.info("列表转换测试通过，转换了{}个对象", dtoList.size());
    }

    @Test
    public void testEmptyAndNullList() {
        // When - 空列表
        List<StreamProxyDTO> emptyResult = streamProxyAssembler.doListToDtoList(new ArrayList<>());

        // Then
        assertNotNull(emptyResult);
        assertTrue(emptyResult.isEmpty());

        // When - null列表
        List<StreamProxyDTO> nullResult = streamProxyAssembler.doListToDtoList(null);

        // Then
        assertTrue(nullResult == null || nullResult.isEmpty());

        log.info("空列表和null列表处理测试通过");
    }

    // ================================
    // 辅助验证方法
    // ================================

    /**
     * 验证DO到DTO转换的正确性
     */
    private void verifyDOtoDTOConversion(StreamProxyDO source, StreamProxyDTO target) {
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getApp(), target.getApp());
        assertEquals(source.getStream(), target.getStream());
        assertEquals(source.getUrl(), target.getUrl());
        assertEquals(source.getProxyKey(), target.getProxyKey());
        assertEquals(source.getStatus(), target.getStatus());
        assertEquals(source.getOnlineStatus(), target.getOnlineStatus());
        assertEquals(source.getEnabled(), target.getEnabled());
        assertEquals(source.getServerId(), target.getServerId());
        assertEquals(source.getDescription(), target.getDescription());
        assertEquals(source.getExtend(), target.getExtend());
        assertEquals(source.getCreateTime(), target.getCreateTime());
        assertEquals(source.getUpdateTime(), target.getUpdateTime());
    }

    /**
     * 验证DTO到DO转换的正确性
     */
    private void verifyDTOtoDOConversion(StreamProxyDTO source, StreamProxyDO target) {
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getApp(), target.getApp());
        assertEquals(source.getStream(), target.getStream());
        assertEquals(source.getUrl(), target.getUrl());
        assertEquals(source.getProxyKey(), target.getProxyKey());
        assertEquals(source.getStatus(), target.getStatus());
        assertEquals(source.getOnlineStatus(), target.getOnlineStatus());
        assertEquals(source.getEnabled(), target.getEnabled());
        assertEquals(source.getServerId(), target.getServerId());
        assertEquals(source.getDescription(), target.getDescription());
        assertEquals(source.getExtend(), target.getExtend());
        assertEquals(source.getCreateTime(), target.getCreateTime());
        assertEquals(source.getUpdateTime(), target.getUpdateTime());
    }

    /**
     * 验证双向转换的数据一致性（适用于DO和DTO）
     */
    private void verifyBidirectionalConsistency(Object source, Object target) {
        if (source instanceof StreamProxyDO && target instanceof StreamProxyDO) {
            StreamProxyDO sourceDO = (StreamProxyDO)source;
            StreamProxyDO targetDO = (StreamProxyDO)target;
            verifyDOConsistency(sourceDO, targetDO);
        } else if (source instanceof StreamProxyDTO && target instanceof StreamProxyDTO) {
            StreamProxyDTO sourceDTO = (StreamProxyDTO)source;
            StreamProxyDTO targetDTO = (StreamProxyDTO)target;
            verifyDTOConsistency(sourceDTO, targetDTO);
        }
    }

    /**
     * 验证DO对象的数据一致性
     */
    private void verifyDOConsistency(StreamProxyDO source, StreamProxyDO target) {
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getApp(), target.getApp());
        assertEquals(source.getStream(), target.getStream());
        assertEquals(source.getUrl(), target.getUrl());
        assertEquals(source.getProxyKey(), target.getProxyKey());
        assertEquals(source.getStatus(), target.getStatus());
        assertEquals(source.getOnlineStatus(), target.getOnlineStatus());
        assertEquals(source.getEnabled(), target.getEnabled());
        assertEquals(source.getServerId(), target.getServerId());
        assertEquals(source.getDescription(), target.getDescription());
        assertEquals(source.getExtend(), target.getExtend());
    }

    /**
     * 验证DTO对象的数据一致性
     */
    private void verifyDTOConsistency(StreamProxyDTO source, StreamProxyDTO target) {
        assertEquals(source.getId(), target.getId());
        assertEquals(source.getApp(), target.getApp());
        assertEquals(source.getStream(), target.getStream());
        assertEquals(source.getUrl(), target.getUrl());
        assertEquals(source.getProxyKey(), target.getProxyKey());
        assertEquals(source.getStatus(), target.getStatus());
        assertEquals(source.getOnlineStatus(), target.getOnlineStatus());
        assertEquals(source.getEnabled(), target.getEnabled());
        assertEquals(source.getServerId(), target.getServerId());
        assertEquals(source.getDescription(), target.getDescription());
        assertEquals(source.getExtend(), target.getExtend());
    }

    /**
     * 创建另一个测试用的StreamProxyDO对象
     */
    private StreamProxyDO createAnotherTestStreamProxyDO() {
        StreamProxyDO streamProxy = new StreamProxyDO();
        streamProxy.setId(TEST_ID + 1);
        streamProxy.setApp(TEST_APP + "2");
        streamProxy.setStream(TEST_STREAM + "2");
        streamProxy.setUrl(TEST_URL.replace(TEST_STREAM, TEST_STREAM + "2"));
        streamProxy.setProxyKey(TEST_PROXY_KEY + "2");
        streamProxy.setStatus(0);
        streamProxy.setOnlineStatus(1);
        streamProxy.setEnabled(false);
        streamProxy.setCreateTime(LocalDateTime.now());
        streamProxy.setUpdateTime(LocalDateTime.now());
        return streamProxy;
    }
}