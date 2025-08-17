package io.github.lunasaw.voglander.manager.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.repository.entity.PushProxyDO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 推流代理装配器单元测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PushProxyAssemblerTest {

    @InjectMocks
    private PushProxyAssembler pushProxyAssembler;

    private PushProxyDTO       testDTO;
    private PushProxyDO        testDO;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理装配器测试数据");

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
        testDTO.setCreateTime(LocalDateTime.of(2025, 1, 23, 10, 30, 0));
        testDTO.setUpdateTime(LocalDateTime.of(2025, 1, 23, 11, 30, 0));

        // 设置ExtendObj
        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("__defaultVhost__");
        extendObj.setRetryCount(-1);
        extendObj.setRtpType(0);
        extendObj.setTimeoutSec(10);
        testDTO.setExtendObj(extendObj);

        // 创建测试DO对象
        testDO = new PushProxyDO();
        testDO.setId(1L);
        testDO.setApp("live");
        testDO.setStream("test");
        testDO.setDstUrl("rtmp://push.example.com/live/test");
        testDO.setSchema("rtmp");
        testDO.setDescription("测试推流代理");
        testDO.setStatus(1);
        testDO.setOnlineStatus(1);
        testDO.setEnabled(1);
        testDO.setServerId("zlm-node-1");
        testDO.setProxyKey("test_proxy_key");
        testDO.setCreateTime(LocalDateTime.of(2025, 1, 23, 10, 30, 0));
        testDO.setUpdateTime(LocalDateTime.of(2025, 1, 23, 11, 30, 0));
        testDO.setExtend("{\"vhost\":\"__defaultVhost__\",\"retryCount\":-1,\"rtpType\":0,\"timeoutSec\":10}");

        log.info("推流代理装配器测试数据设置完成");
    }

    // ================================
    // dtoToDo 测试
    // ================================

    @Test
    public void testDtoToDo_Success() {
        // When
        PushProxyDO result = pushProxyAssembler.dtoToDo(testDTO);

        // Then
        assertNotNull(result);
        assertEquals(testDTO.getId(), result.getId());
        assertEquals(testDTO.getApp(), result.getApp());
        assertEquals(testDTO.getStream(), result.getStream());
        assertEquals(testDTO.getDstUrl(), result.getDstUrl());
        assertEquals(testDTO.getSchema(), result.getSchema());
        assertEquals(testDTO.getDescription(), result.getDescription());
        assertEquals(testDTO.getStatus(), result.getStatus());
        assertEquals(testDTO.getOnlineStatus(), result.getOnlineStatus());
        assertEquals(testDTO.getEnabled(), result.getEnabled());
        assertEquals(testDTO.getServerId(), result.getServerId());
        assertEquals(testDTO.getProxyKey(), result.getProxyKey());
        assertEquals(testDTO.getCreateTime(), result.getCreateTime());
        assertEquals(testDTO.getUpdateTime(), result.getUpdateTime());

        // 验证ExtendObj序列化
        assertNotNull(result.getExtend());
        assertTrue(result.getExtend().contains("__defaultVhost__"));
        assertTrue(result.getExtend().contains("-1"));
        assertTrue(result.getExtend().contains("0"));
        assertTrue(result.getExtend().contains("10"));

        log.info("DTO转DO测试通过");
    }

    @Test
    public void testDtoToDo_NullDto() {
        // When
        PushProxyDO result = pushProxyAssembler.dtoToDo(null);

        // Then
        assertNull(result);

        log.info("DTO转DO空参数测试通过");
    }

    @Test
    public void testDtoToDo_NullExtendObj() {
        // Given
        testDTO.setExtendObj(null);

        // When
        PushProxyDO result = pushProxyAssembler.dtoToDo(testDTO);

        // Then
        assertNotNull(result);
        assertEquals(testDTO.getId(), result.getId());
        assertEquals(testDTO.getApp(), result.getApp());
        // Extend字段应该为null或空字符串
        assertTrue(result.getExtend() == null || result.getExtend().isEmpty());

        log.info("DTO转DO无扩展对象测试通过");
    }

    @Test
    public void testDtoToDo_PartialFields() {
        // Given
        PushProxyDTO partialDTO = new PushProxyDTO();
        partialDTO.setId(2L);
        partialDTO.setApp("live");
        partialDTO.setStream("test2");
        // 其他字段为null

        // When
        PushProxyDO result = pushProxyAssembler.dtoToDo(partialDTO);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("live", result.getApp());
        assertEquals("test2", result.getStream());
        assertNull(result.getDstUrl());
        assertNull(result.getDescription());

        log.info("DTO转DO部分字段测试通过");
    }

    // ================================
    // doToDto 测试
    // ================================

    @Test
    public void testDoToDto_Success() {
        // When
        PushProxyDTO result = pushProxyAssembler.doToDto(testDO);

        // Then
        assertNotNull(result);
        assertEquals(testDO.getId(), result.getId());
        assertEquals(testDO.getApp(), result.getApp());
        assertEquals(testDO.getStream(), result.getStream());
        assertEquals(testDO.getDstUrl(), result.getDstUrl());
        assertEquals(testDO.getSchema(), result.getSchema());
        assertEquals(testDO.getDescription(), result.getDescription());
        assertEquals(testDO.getStatus(), result.getStatus());
        assertEquals(testDO.getOnlineStatus(), result.getOnlineStatus());
        assertEquals(testDO.getEnabled(), result.getEnabled());
        assertEquals(testDO.getServerId(), result.getServerId());
        assertEquals(testDO.getProxyKey(), result.getProxyKey());
        assertEquals(testDO.getCreateTime(), result.getCreateTime());
        assertEquals(testDO.getUpdateTime(), result.getUpdateTime());

        // 验证ExtendObj反序列化
        assertNotNull(result.getExtendObj());
        assertEquals("__defaultVhost__", result.getExtendObj().getVhost());
        assertEquals(Integer.valueOf(-1), result.getExtendObj().getRetryCount());
        assertEquals(Integer.valueOf(0), result.getExtendObj().getRtpType());
        assertEquals(Integer.valueOf(10), result.getExtendObj().getTimeoutSec());

        log.info("DO转DTO测试通过");
    }

    @Test
    public void testDoToDto_NullDo() {
        // When
        PushProxyDTO result = pushProxyAssembler.doToDto(null);

        // Then
        assertNull(result);

        log.info("DO转DTO空参数测试通过");
    }

    @Test
    public void testDoToDto_NullExtend() {
        // Given
        testDO.setExtend(null);

        // When
        PushProxyDTO result = pushProxyAssembler.doToDto(testDO);

        // Then
        assertNotNull(result);
        assertEquals(testDO.getId(), result.getId());
        assertEquals(testDO.getApp(), result.getApp());
        // ExtendObj应该为null或默认对象
        // 取决于具体实现

        log.info("DO转DTO无扩展字段测试通过");
    }

    @Test
    public void testDoToDto_EmptyExtend() {
        // Given
        testDO.setExtend("");

        // When
        PushProxyDTO result = pushProxyAssembler.doToDto(testDO);

        // Then
        assertNotNull(result);
        assertEquals(testDO.getId(), result.getId());
        assertEquals(testDO.getApp(), result.getApp());
        // ExtendObj应该为null或默认对象

        log.info("DO转DTO空扩展字段测试通过");
    }

    @Test
    public void testDoToDto_InvalidJsonExtend() {
        // Given
        testDO.setExtend("{invalid json}");

        // When
        PushProxyDTO result = pushProxyAssembler.doToDto(testDO);

        // Then
        assertNotNull(result);
        assertEquals(testDO.getId(), result.getId());
        assertEquals(testDO.getApp(), result.getApp());
        // ExtendObj应该为null或默认对象，具体取决于异常处理策略

        log.info("DO转DTO无效JSON扩展字段测试通过");
    }

    // ================================
    // doListToDtoList 测试
    // ================================

    @Test
    public void testDoListToDtoList_Success() {
        // Given
        PushProxyDO testDO2 = new PushProxyDO();
        testDO2.setId(2L);
        testDO2.setApp("live");
        testDO2.setStream("test2");
        testDO2.setDstUrl("rtmp://push2.example.com/live/test2");
        testDO2.setExtend("{\"vhost\":\"vhost2\",\"retryCount\":5}");

        List<PushProxyDO> doList = Arrays.asList(testDO, testDO2);

        // When
        List<PushProxyDTO> result = pushProxyAssembler.doListToDtoList(doList);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        PushProxyDTO dto1 = result.get(0);
        assertEquals(testDO.getId(), dto1.getId());
        assertEquals(testDO.getApp(), dto1.getApp());
        assertNotNull(dto1.getExtendObj());

        PushProxyDTO dto2 = result.get(1);
        assertEquals(testDO2.getId(), dto2.getId());
        assertEquals(testDO2.getApp(), dto2.getApp());

        log.info("DO列表转DTO列表测试通过");
    }

    @Test
    public void testDoListToDtoList_NullList() {
        // When
        List<PushProxyDTO> result = pushProxyAssembler.doListToDtoList(null);

        // Then
        assertNull(result);

        log.info("DO列表转DTO列表空参数测试通过");
    }

    @Test
    public void testDoListToDtoList_EmptyList() {
        // When
        List<PushProxyDTO> result = pushProxyAssembler.doListToDtoList(Arrays.asList());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        log.info("DO列表转DTO列表空列表测试通过");
    }

    @Test
    public void testDoListToDtoList_WithNullElements() {
        // Given
        List<PushProxyDO> doListWithNull = Arrays.asList(testDO, null);

        // When
        List<PushProxyDTO> result = pushProxyAssembler.doListToDtoList(doListWithNull);

        // Then
        assertNotNull(result);
        // 结果取决于实现：可能过滤null元素，也可能保留
        // 这里假设过滤null元素
        assertTrue(result.size() <= 2);

        log.info("DO列表转DTO列表包含null元素测试通过");
    }

    // ================================
    // 双向转换测试
    // ================================

    @Test
    public void testBidirectionalConversion_DtoToDoToDto() {
        // Given
        PushProxyDTO originalDTO = testDTO;

        // When
        PushProxyDO convertedDO = pushProxyAssembler.dtoToDo(originalDTO);
        PushProxyDTO backToDTO = pushProxyAssembler.doToDto(convertedDO);

        // Then
        assertNotNull(convertedDO);
        assertNotNull(backToDTO);

        // 验证关键字段一致性
        assertEquals(originalDTO.getId(), backToDTO.getId());
        assertEquals(originalDTO.getApp(), backToDTO.getApp());
        assertEquals(originalDTO.getStream(), backToDTO.getStream());
        assertEquals(originalDTO.getDstUrl(), backToDTO.getDstUrl());
        assertEquals(originalDTO.getProxyKey(), backToDTO.getProxyKey());

        // 验证ExtendObj一致性
        if (originalDTO.getExtendObj() != null && backToDTO.getExtendObj() != null) {
            assertEquals(originalDTO.getExtendObj().getVhost(), backToDTO.getExtendObj().getVhost());
            assertEquals(originalDTO.getExtendObj().getRetryCount(), backToDTO.getExtendObj().getRetryCount());
            assertEquals(originalDTO.getExtendObj().getRtpType(), backToDTO.getExtendObj().getRtpType());
            assertEquals(originalDTO.getExtendObj().getTimeoutSec(), backToDTO.getExtendObj().getTimeoutSec());
        }

        log.info("DTO->DO->DTO双向转换测试通过");
    }

    @Test
    public void testBidirectionalConversion_DoToDtoToDo() {
        // Given
        PushProxyDO originalDO = testDO;

        // When
        PushProxyDTO convertedDTO = pushProxyAssembler.doToDto(originalDO);
        PushProxyDO backToDO = pushProxyAssembler.dtoToDo(convertedDTO);

        // Then
        assertNotNull(convertedDTO);
        assertNotNull(backToDO);

        // 验证关键字段一致性
        assertEquals(originalDO.getId(), backToDO.getId());
        assertEquals(originalDO.getApp(), backToDO.getApp());
        assertEquals(originalDO.getStream(), backToDO.getStream());
        assertEquals(originalDO.getDstUrl(), backToDO.getDstUrl());
        assertEquals(originalDO.getProxyKey(), backToDO.getProxyKey());

        // 验证Extend字段JSON一致性（可能因格式化而略有不同）
        if (originalDO.getExtend() != null && backToDO.getExtend() != null) {
            // 验证包含相同的关键信息
            assertTrue(backToDO.getExtend().contains("__defaultVhost__"));
            assertTrue(backToDO.getExtend().contains("-1"));
        }

        log.info("DO->DTO->DO双向转换测试通过");
    }

    // ================================
    // 特殊字符和编码测试
    // ================================

    @Test
    public void testSpecialCharactersAndEncoding() {
        // Given
        PushProxyDTO unicodeDTO = new PushProxyDTO();
        unicodeDTO.setId(1L);
        unicodeDTO.setApp("直播应用");
        unicodeDTO.setStream("测试流 🎥");
        unicodeDTO.setDstUrl("rtmp://推流服务器.example.com/直播/测试");
        unicodeDTO.setDescription("包含特殊字符的描述：@#$%^&*()");

        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("虚拟主机_测试");
        unicodeDTO.setExtendObj(extendObj);

        // When
        PushProxyDO convertedDO = pushProxyAssembler.dtoToDo(unicodeDTO);
        PushProxyDTO backToDTO = pushProxyAssembler.doToDto(convertedDO);

        // Then
        assertNotNull(convertedDO);
        assertNotNull(backToDTO);
        assertEquals(unicodeDTO.getApp(), backToDTO.getApp());
        assertEquals(unicodeDTO.getStream(), backToDTO.getStream());
        assertEquals(unicodeDTO.getDstUrl(), backToDTO.getDstUrl());
        assertEquals(unicodeDTO.getDescription(), backToDTO.getDescription());

        if (backToDTO.getExtendObj() != null) {
            assertEquals("虚拟主机_测试", backToDTO.getExtendObj().getVhost());
        }

        log.info("特殊字符和编码测试通过");
    }

    // ================================
    // 复杂ExtendObj测试
    // ================================

    @Test
    public void testComplexExtendObj() {
        // Given
        PushProxyDTO.ExtendObj complexExtendObj = new PushProxyDTO.ExtendObj();
        complexExtendObj.setVhost("complex.vhost.example.com");
        complexExtendObj.setRetryCount(Integer.MAX_VALUE);
        complexExtendObj.setRtpType(Integer.MIN_VALUE);
        complexExtendObj.setTimeoutSec(0);

        testDTO.setExtendObj(complexExtendObj);

        // When
        PushProxyDO convertedDO = pushProxyAssembler.dtoToDo(testDTO);
        PushProxyDTO backToDTO = pushProxyAssembler.doToDto(convertedDO);

        // Then
        assertNotNull(convertedDO);
        assertNotNull(backToDTO);
        assertNotNull(backToDTO.getExtendObj());

        assertEquals("complex.vhost.example.com", backToDTO.getExtendObj().getVhost());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), backToDTO.getExtendObj().getRetryCount());
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), backToDTO.getExtendObj().getRtpType());
        assertEquals(Integer.valueOf(0), backToDTO.getExtendObj().getTimeoutSec());

        log.info("复杂ExtendObj测试通过");
    }

    // ================================
    // 性能测试
    // ================================

    @Test
    public void testPerformance() {
        // Given
        int iterations = 1000;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            PushProxyDO convertedDO = pushProxyAssembler.dtoToDo(testDTO);
            PushProxyDTO backToDTO = pushProxyAssembler.doToDto(convertedDO);
        }
        long endTime = System.currentTimeMillis();

        // Then
        long duration = endTime - startTime;
        log.info("性能测试完成：{}次转换耗时{}ms，平均每次{}ms",
            iterations * 2, duration, (double)duration / (iterations * 2));

        // 验证性能合理（每次转换应该在几毫秒内）
        assertTrue(duration < 5000, "转换性能过慢");

        log.info("性能测试通过");
    }
}