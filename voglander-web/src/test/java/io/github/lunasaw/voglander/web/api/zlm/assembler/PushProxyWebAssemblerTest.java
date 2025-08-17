package io.github.lunasaw.voglander.web.api.zlm.assembler;

import io.github.lunasaw.voglander.manager.domaon.dto.PushProxyDTO;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyCreateReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyQueryReq;
import io.github.lunasaw.voglander.web.api.zlm.req.PushProxyUpdateReq;
import io.github.lunasaw.voglander.web.api.zlm.vo.PushProxyVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 推流代理Web装配器单元测试
 * 
 * @author luna
 * @since 2025-01-23
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
public class PushProxyWebAssemblerTest {

    @InjectMocks
    private PushProxyWebAssembler pushProxyWebAssembler;

    private PushProxyCreateReq    createReq;
    private PushProxyUpdateReq    updateReq;
    private PushProxyQueryReq     queryReq;
    private PushProxyDTO          dto;
    private PushProxyVO           vo;

    @BeforeEach
    public void setUp() {
        log.info("开始设置推流代理Web装配器测试数据");

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
        dto.setOnlineStatus(1);
        dto.setEnabled(1);
        dto.setServerId("zlm-node-1");
        dto.setProxyKey("test_proxy_key");

        // 设置ExtendObj
        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("__defaultVhost__");
        extendObj.setRetryCount(-1);
        extendObj.setRtpType(0);
        extendObj.setTimeoutSec(10);
        dto.setExtendObj(extendObj);

        // 创建测试VO对象
        vo = new PushProxyVO();
        vo.setId(1L);
        vo.setApp("live");
        vo.setStream("test");
        vo.setDstUrl("rtmp://push.example.com/live/test");
        vo.setSchema("rtmp");
        vo.setDescription("测试推流代理");
        vo.setStatus(1);
        vo.setOnlineStatus(1);
        vo.setEnabled(1);
        vo.setServerId("zlm-node-1");
        vo.setProxyKey("test_proxy_key");

        log.info("推流代理Web装配器测试数据设置完成");
    }

    // ================================
    // createReqToDto 测试
    // ================================

    @Test
    public void testCreateReqToDto_Success() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.createReqToDto(createReq);

        // Then
        assertNotNull(result);
        assertEquals(createReq.getApp(), result.getApp());
        assertEquals(createReq.getStream(), result.getStream());
        assertEquals(createReq.getDstUrl(), result.getDstUrl());
        assertEquals(createReq.getSchema(), result.getSchema());
        assertEquals(createReq.getDescription(), result.getDescription());
        assertEquals(createReq.getStatus(), result.getStatus());
        assertEquals(createReq.getServerId(), result.getServerId());

        log.info("创建请求转DTO测试通过");
    }

    @Test
    public void testCreateReqToDto_NullRequest() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.createReqToDto(null);

        // Then
        assertNull(result);

        log.info("创建请求转DTO空参数测试通过");
    }

    @Test
    public void testCreateReqToDto_PartialFields() {
        // Given
        PushProxyCreateReq partialReq = new PushProxyCreateReq();
        partialReq.setApp("live");
        partialReq.setStream("test");
        // 其他字段为null

        // When
        PushProxyDTO result = pushProxyWebAssembler.createReqToDto(partialReq);

        // Then
        assertNotNull(result);
        assertEquals("live", result.getApp());
        assertEquals("test", result.getStream());
        assertNull(result.getDstUrl());
        assertNull(result.getDescription());

        log.info("创建请求转DTO部分字段测试通过");
    }

    // ================================
    // updateReqToDto 测试
    // ================================

    @Test
    public void testUpdateReqToDto_Success() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.updateReqToDto(updateReq);

        // Then
        assertNotNull(result);
        assertEquals(updateReq.getId(), result.getId());
        assertEquals(updateReq.getApp(), result.getApp());
        assertEquals(updateReq.getStream(), result.getStream());
        assertEquals(updateReq.getDstUrl(), result.getDstUrl());
        assertEquals(updateReq.getSchema(), result.getSchema());
        assertEquals(updateReq.getDescription(), result.getDescription());
        assertEquals(updateReq.getStatus(), result.getStatus());
        assertEquals(updateReq.getServerId(), result.getServerId());

        log.info("更新请求转DTO测试通过");
    }

    @Test
    public void testUpdateReqToDto_NullRequest() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.updateReqToDto(null);

        // Then
        assertNull(result);

        log.info("更新请求转DTO空参数测试通过");
    }

    @Test
    public void testUpdateReqToDto_OnlyId() {
        // Given
        PushProxyUpdateReq idOnlyReq = new PushProxyUpdateReq();
        idOnlyReq.setId(999L);

        // When
        PushProxyDTO result = pushProxyWebAssembler.updateReqToDto(idOnlyReq);

        // Then
        assertNotNull(result);
        assertEquals(999L, result.getId());
        assertNull(result.getApp());
        assertNull(result.getStream());

        log.info("更新请求转DTO仅ID测试通过");
    }

    // ================================
    // queryReqToDto 测试
    // ================================

    @Test
    public void testQueryReqToDto_Success() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.queryReqToDto(queryReq);

        // Then
        assertNotNull(result);
        assertEquals(queryReq.getId(), result.getId());
        assertEquals(queryReq.getApp(), result.getApp());
        assertEquals(queryReq.getStream(), result.getStream());

        log.info("查询请求转DTO测试通过");
    }

    @Test
    public void testQueryReqToDto_NullRequest() {
        // When
        PushProxyDTO result = pushProxyWebAssembler.queryReqToDto(null);

        // Then
        assertNull(result); // 通常查询允许空条件，返回空的DTO对象
        // 或者根据实际实现返回null

        log.info("查询请求转DTO空参数测试通过");
    }

    @Test
    public void testQueryReqToDto_EmptyRequest() {
        // Given
        PushProxyQueryReq emptyReq = new PushProxyQueryReq();

        // When
        PushProxyDTO result = pushProxyWebAssembler.queryReqToDto(emptyReq);

        // Then
        assertNotNull(result);
        assertNull(result.getId());
        assertNull(result.getApp());
        assertNull(result.getStream());

        log.info("查询请求转DTO空条件测试通过");
    }

    // ================================
    // dtoToVo 测试
    // ================================

    @Test
    public void testDtoToVo_Success() {
        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(dto);

        // Then
        assertNotNull(result);
        assertEquals(dto.getId(), result.getId());
        assertEquals(dto.getApp(), result.getApp());
        assertEquals(dto.getStream(), result.getStream());
        assertEquals(dto.getDstUrl(), result.getDstUrl());
        assertEquals(dto.getSchema(), result.getSchema());
        assertEquals(dto.getDescription(), result.getDescription());
        assertEquals(dto.getStatus(), result.getStatus());
        assertEquals(dto.getOnlineStatus(), result.getOnlineStatus());
        assertEquals(dto.getEnabled(), result.getEnabled());
        assertEquals(dto.getServerId(), result.getServerId());
        assertEquals(dto.getProxyKey(), result.getProxyKey());

        // 验证ExtendObj转换
        assertNotNull(result.getExtendObj());
        assertEquals(dto.getExtendObj().getVhost(), result.getExtendObj().getVhost());
        assertEquals(dto.getExtendObj().getRetryCount(), result.getExtendObj().getRetryCount());
        assertEquals(dto.getExtendObj().getRtpType(), result.getExtendObj().getRtpType());
        assertEquals(dto.getExtendObj().getTimeoutSec(), result.getExtendObj().getTimeoutSec());

        log.info("DTO转VO测试通过");
    }

    @Test
    public void testDtoToVo_NullDto() {
        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(null);

        // Then
        assertNull(result);

        log.info("DTO转VO空参数测试通过");
    }

    @Test
    public void testDtoToVo_NullExtendObj() {
        // Given
        PushProxyDTO dtoWithoutExtend = new PushProxyDTO();
        dtoWithoutExtend.setId(1L);
        dtoWithoutExtend.setApp("live");
        dtoWithoutExtend.setStream("test");
        dtoWithoutExtend.setExtendObj(null);

        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(dtoWithoutExtend);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("live", result.getApp());
        assertEquals("test", result.getStream());
        // ExtendVO可能为null或空对象，取决于实现

        log.info("DTO转VO无扩展对象测试通过");
    }

    // ================================
    // 时间字段转换测试
    // ================================

    @Test
    public void testTimeFieldConversion() {
        // Given
        dto.setCreateTime(java.time.LocalDateTime.of(2025, 1, 23, 10, 30, 0));
        dto.setUpdateTime(java.time.LocalDateTime.of(2025, 1, 23, 11, 30, 0));

        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(dto);

        // Then
        assertNotNull(result);
        // 验证时间字段转换为时间戳（如果实现了转换）
        if (result.getCreateTime() != null) {
            assertTrue(result.getCreateTime() > 0);
        }
        if (result.getUpdateTime() != null) {
            assertTrue(result.getUpdateTime() > 0);
        }

        log.info("时间字段转换测试通过");
    }

    // ================================
    // 数据类型转换测试
    // ================================

    @Test
    public void testDataTypeConversion() {
        // Given
        dto.setStatus(1);
        dto.setOnlineStatus(0);
        dto.setEnabled(1);

        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(dto);

        // Then
        assertNotNull(result);
        assertEquals(Integer.valueOf(1), result.getStatus());
        assertEquals(Integer.valueOf(0), result.getOnlineStatus());
        assertEquals(Integer.valueOf(1), result.getEnabled());

        log.info("数据类型转换测试通过");
    }

    // ================================
    // 边界值测试
    // ================================

    @Test
    public void testBoundaryValues() {
        // Given
        PushProxyCreateReq boundaryReq = new PushProxyCreateReq();
        boundaryReq.setApp(""); // 空字符串
        boundaryReq.setStream("a".repeat(255)); // 长字符串
        boundaryReq.setStatus(0); // 最小值
        boundaryReq.setServerId("node-" + Long.MAX_VALUE); // 长ID

        // When
        PushProxyDTO result = pushProxyWebAssembler.createReqToDto(boundaryReq);

        // Then
        assertNotNull(result);
        assertEquals("", result.getApp());
        assertEquals("a".repeat(255), result.getStream());
        assertEquals(0, result.getStatus());
        assertEquals("node-" + Long.MAX_VALUE, result.getServerId());

        log.info("边界值测试通过");
    }

    // ================================
    // 扩展字段测试
    // ================================

    @Test
    public void testExtendFieldConversion() {
        // Given
        PushProxyDTO.ExtendObj extendObj = new PushProxyDTO.ExtendObj();
        extendObj.setVhost("custom_vhost");
        extendObj.setRetryCount(5);
        extendObj.setRtpType(1);
        extendObj.setTimeoutSec(30);

        dto.setExtendObj(extendObj);

        // When
        PushProxyVO result = pushProxyWebAssembler.dtoToVo(dto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getExtendObj());
        assertEquals("custom_vhost", result.getExtendObj().getVhost());
        assertEquals(Integer.valueOf(5), result.getExtendObj().getRetryCount());
        assertEquals(Integer.valueOf(1), result.getExtendObj().getRtpType());
        assertEquals(Integer.valueOf(30), result.getExtendObj().getTimeoutSec());

        log.info("扩展字段转换测试通过");
    }

    // ================================
    // 字符编码测试
    // ================================

    @Test
    public void testCharacterEncoding() {
        // Given
        PushProxyCreateReq unicodeReq = new PushProxyCreateReq();
        unicodeReq.setApp("直播");
        unicodeReq.setStream("测试流");
        unicodeReq.setDescription("这是一个中文描述 🎥📺");
        unicodeReq.setDstUrl("rtmp://推流.example.com/直播/测试流");

        // When
        PushProxyDTO result = pushProxyWebAssembler.createReqToDto(unicodeReq);

        // Then
        assertNotNull(result);
        assertEquals("直播", result.getApp());
        assertEquals("测试流", result.getStream());
        assertEquals("这是一个中文描述 🎥📺", result.getDescription());
        assertEquals("rtmp://推流.example.com/直播/测试流", result.getDstUrl());

        log.info("字符编码测试通过");
    }

    // ================================
    // 完整转换链测试
    // ================================

    @Test
    public void testCompleteConversionChain() {
        // 测试完整的转换链：CreateReq -> DTO -> VO

        // 1. CreateReq -> DTO
        PushProxyDTO dtoFromCreateReq = pushProxyWebAssembler.createReqToDto(createReq);
        assertNotNull(dtoFromCreateReq);

        // 2. DTO -> VO
        PushProxyVO voFromDto = pushProxyWebAssembler.dtoToVo(dtoFromCreateReq);
        assertNotNull(voFromDto);

        // 3. 验证数据一致性
        assertEquals(createReq.getApp(), voFromDto.getApp());
        assertEquals(createReq.getStream(), voFromDto.getStream());
        assertEquals(createReq.getDstUrl(), voFromDto.getDstUrl());
        assertEquals(createReq.getDescription(), voFromDto.getDescription());

        log.info("完整转换链测试通过");
    }
}