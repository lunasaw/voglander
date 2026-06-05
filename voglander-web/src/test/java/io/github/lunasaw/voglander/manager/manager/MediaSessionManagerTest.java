package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import io.github.lunasaw.voglander.BaseTest;
import io.github.lunasaw.voglander.common.constant.media.MediaSessionConstant;
import io.github.lunasaw.voglander.manager.assembler.MediaSessionAssembler;
import io.github.lunasaw.voglander.manager.domaon.dto.MediaSessionDTO;
import io.github.lunasaw.voglander.manager.service.MediaSessionService;
import io.github.lunasaw.voglander.repository.entity.MediaSessionDO;
import lombok.extern.slf4j.Slf4j;

/**
 * MediaSessionManager 集成测试类
 *
 * <p>
 * 测试策略：
 * </p>
 * <ul>
 * <li>继承 BaseTest 集成测试，使用真实 Spring 上下文与数据库</li>
 * <li>MediaSessionService 使用 @Autowired（基础数据访问层）</li>
 * <li>MediaSessionAssembler 使用 @MockitoBean（业务转换层）</li>
 * </ul>
 *
 * <p>
 * 测试覆盖：CRUD 模板方法 + 业务事件方法（onInviteOk/onInviteFailure/onBye/onAck/onMediaStatus）+ 完整生命周期。
 * </p>
 *
 * @author luna
 * @since 2025-05-29
 */
@Slf4j
public class MediaSessionManagerTest extends BaseTest {

    private static final String   TEST_CALL_ID    = "call-id-test-0001";
    private static final String   TEST_CALL_ID_2  = "call-id-test-0002";
    private static final String   TEST_DEVICE_ID  = "34020000001320000001";
    private static final String   TEST_CHANNEL_ID = "34020000001310000001";

    @Autowired
    private MediaSessionManager   mediaSessionManager;

    @Autowired
    private CacheManager          cacheManager;

    @Autowired
    private MediaSessionService   mediaSessionService;

    @MockitoBean
    private MediaSessionAssembler mediaSessionAssembler;

    @BeforeEach
    public void setUp() {
        cleanupTestData();
        setupAssemblerMocks();
    }

    @AfterEach
    public void tearDown() {
        cleanupTestData();
    }

    private void cleanupTestData() {
        try {
            QueryWrapper<MediaSessionDO> wrapper = new QueryWrapper<>();
            wrapper.in("call_id", TEST_CALL_ID, TEST_CALL_ID_2)
                .or().eq("device_id", TEST_DEVICE_ID);
            mediaSessionService.remove(wrapper);

            Cache cache = cacheManager.getCache("mediaSession");
            if (cache != null) {
                cache.clear();
            }
        } catch (Exception e) {
            log.warn("清理测试数据时发生异常: {}", e.getMessage());
        }
    }

    /**
     * 设置 Assembler 的模拟行为（双向全字段复制）。
     */
    private void setupAssemblerMocks() {
        when(mediaSessionAssembler.dtoToDo(any(MediaSessionDTO.class)))
            .thenAnswer(invocation -> {
                MediaSessionDTO dto = invocation.getArgument(0);
                MediaSessionDO did = new MediaSessionDO();
                did.setId(dto.getId());
                did.setCreateTime(dto.getCreateTime());
                did.setUpdateTime(dto.getUpdateTime());
                did.setCallId(dto.getCallId());
                did.setDeviceId(dto.getDeviceId());
                did.setChannelId(dto.getChannelId());
                did.setSsrc(dto.getSsrc());
                did.setStream(dto.getStream());
                did.setStatus(dto.getStatus());
                did.setSessionType(dto.getSessionType());
                did.setExtend(dto.getExtend());
                did.setStreamId(dto.getStreamId());
                did.setNodeServerId(dto.getNodeServerId());
                did.setRefCount(dto.getRefCount());
                return did;
            });

        when(mediaSessionAssembler.doToDto(any(MediaSessionDO.class)))
            .thenAnswer(invocation -> {
                MediaSessionDO did = invocation.getArgument(0);
                MediaSessionDTO dto = new MediaSessionDTO();
                dto.setId(did.getId());
                dto.setCreateTime(did.getCreateTime());
                dto.setUpdateTime(did.getUpdateTime());
                dto.setCallId(did.getCallId());
                dto.setDeviceId(did.getDeviceId());
                dto.setChannelId(did.getChannelId());
                dto.setSsrc(did.getSsrc());
                dto.setStream(did.getStream());
                dto.setStatus(did.getStatus());
                dto.setSessionType(did.getSessionType());
                dto.setExtend(did.getExtend());
                dto.setStreamId(did.getStreamId());
                dto.setNodeServerId(did.getNodeServerId());
                dto.setRefCount(did.getRefCount());
                return dto;
            });

        when(mediaSessionAssembler.doListToDtoList(anyList()))
            .thenAnswer(invocation -> {
                List<MediaSessionDO> doList = invocation.getArgument(0);
                if (doList == null || doList.isEmpty()) {
                    return new ArrayList<>();
                }
                List<MediaSessionDTO> result = new ArrayList<>();
                for (MediaSessionDO did : doList) {
                    MediaSessionDTO dto = new MediaSessionDTO();
                    dto.setId(did.getId());
                    dto.setCallId(did.getCallId());
                    dto.setDeviceId(did.getDeviceId());
                    dto.setChannelId(did.getChannelId());
                    dto.setStatus(did.getStatus());
                    dto.setStreamId(did.getStreamId());
                    dto.setNodeServerId(did.getNodeServerId());
                    dto.setRefCount(did.getRefCount());
                    result.add(dto);
                }
                return result;
            });
    }

    private MediaSessionDTO createTestDTO() {
        MediaSessionDTO dto = new MediaSessionDTO();
        dto.setCallId(TEST_CALL_ID);
        dto.setDeviceId(TEST_DEVICE_ID);
        dto.setChannelId(TEST_CHANNEL_ID);
        dto.setStatus(MediaSessionConstant.Status.INVITING);
        dto.setSessionType(MediaSessionConstant.Type.PLAY);
        return dto;
    }

    // ================================
    // CRUD 模板方法
    // ================================

    @Test
    public void testAdd_Success() {
        Long id = mediaSessionManager.add(createTestDTO());

        assertNotNull(id);
        assertTrue(id > 0);
        MediaSessionDO saved = mediaSessionService.getById(id);
        assertNotNull(saved);
        assertEquals(TEST_CALL_ID, saved.getCallId());
        assertEquals(TEST_DEVICE_ID, saved.getDeviceId());
        verify(mediaSessionAssembler, atLeastOnce()).dtoToDo(any(MediaSessionDTO.class));
    }

    @Test
    public void testAdd_Validation() {
        assertThrows(IllegalArgumentException.class, () -> mediaSessionManager.add(null));

        MediaSessionDTO noCallId = new MediaSessionDTO();
        noCallId.setDeviceId(TEST_DEVICE_ID);
        assertThrows(IllegalArgumentException.class, () -> mediaSessionManager.add(noCallId));
    }

    @Test
    public void testGetByCallId() {
        mediaSessionManager.add(createTestDTO());

        MediaSessionDTO found = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertNotNull(found);
        assertEquals(TEST_CALL_ID, found.getCallId());
        assertEquals(MediaSessionConstant.Status.INVITING, found.getStatus());
    }

    @Test
    public void testUpdateById() {
        Long id = mediaSessionManager.add(createTestDTO());

        MediaSessionDTO update = new MediaSessionDTO();
        update.setStatus(MediaSessionConstant.Status.ACTIVE);
        update.setStream("live/test");
        Long updatedId = mediaSessionManager.updateById(id, update);

        assertEquals(id, updatedId);
        MediaSessionDO after = mediaSessionService.getById(id);
        assertEquals(MediaSessionConstant.Status.ACTIVE, after.getStatus());
        assertEquals("live/test", after.getStream());
    }

    @Test
    public void testGetPage() {
        mediaSessionManager.add(createTestDTO());

        Page<MediaSessionDTO> page = mediaSessionManager.getPage(new MediaSessionDTO(), 1, 10);
        assertNotNull(page);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    public void testDeleteOne() {
        Long id = mediaSessionManager.add(createTestDTO());

        MediaSessionDTO delQuery = new MediaSessionDTO();
        delQuery.setCallId(TEST_CALL_ID);
        Boolean deleted = mediaSessionManager.deleteOne(delQuery);

        assertTrue(deleted);
        assertNull(mediaSessionService.getById(id));
    }

    // ================================
    // 业务事件方法
    // ================================

    @Test
    public void testOnInviteOk_CreatesActiveSession() {
        Long id = mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID);

        assertNotNull(id);
        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertNotNull(session);
        assertEquals(MediaSessionConstant.Status.ACTIVE, session.getStatus());
        assertEquals(TEST_DEVICE_ID, session.getDeviceId());
    }

    @Test
    public void testOnInviteOk_UpdatesExistingInvitingSession() {
        // 先有一条 INVITING 会话
        mediaSessionManager.add(createTestDTO());

        mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID);

        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.ACTIVE, session.getStatus());
        // 仍然只有一条记录（更新而非新增）
        QueryWrapper<MediaSessionDO> w = new QueryWrapper<>();
        w.eq("call_id", TEST_CALL_ID);
        assertEquals(1, mediaSessionService.count(w));
    }

    @Test
    public void testOnInviteOk_BackfillsPlaceholderByDeviceChannel() {
        // startLive 预写占位行：callId 暂用 streamId，状态 INVITING
        String streamId = "gb_live_" + TEST_DEVICE_ID + "_" + TEST_CHANNEL_ID;
        MediaSessionDTO placeholder = new MediaSessionDTO();
        placeholder.setCallId(streamId);
        placeholder.setStreamId(streamId);
        placeholder.setDeviceId(TEST_DEVICE_ID);
        placeholder.setChannelId(TEST_CHANNEL_ID);
        placeholder.setStatus(MediaSessionConstant.Status.INVITING);
        placeholder.setSessionType(MediaSessionConstant.Type.PLAY);
        Long placeholderId = mediaSessionManager.add(placeholder);

        // 真实 SIP callId 通过 InviteOk 回来，按 (deviceId, channelId, INVITING) 匹配占位行回填
        Long id = mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID, TEST_CHANNEL_ID);

        // 回填到同一行（未新建）
        assertEquals(placeholderId, id);
        MediaSessionDO after = mediaSessionService.getById(placeholderId);
        assertEquals(TEST_CALL_ID, after.getCallId());
        assertEquals(MediaSessionConstant.Status.ACTIVE, after.getStatus());
        assertEquals(streamId, after.getStreamId());

        // 总记录数仍为 1
        QueryWrapper<MediaSessionDO> w = new QueryWrapper<>();
        w.eq("device_id", TEST_DEVICE_ID);
        assertEquals(1, mediaSessionService.count(w));
    }

    @Test
    public void testOnInviteOk_CreatesNewWhenNoPlaceholder() {
        // 无占位行，channelId 给定但库中无 INVITING 行 → 兜底新建 ACTIVE 行
        Long id = mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID, TEST_CHANNEL_ID);
        assertNotNull(id);
        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.ACTIVE, session.getStatus());
        assertEquals(TEST_CHANNEL_ID, session.getChannelId());
    }

    @Test
    public void testOnInviteFailure() {
        mediaSessionManager.add(createTestDTO());

        mediaSessionManager.onInviteFailure(TEST_CALL_ID, 486);

        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.FAILED, session.getStatus());
    }

    @Test
    public void testOnAck_SetsActive() {
        mediaSessionManager.add(createTestDTO());

        Long id = mediaSessionManager.onAck(TEST_CALL_ID);

        assertNotNull(id);
        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.ACTIVE, session.getStatus());
    }

    @Test
    public void testOnBye_ClosesActiveSessions() {
        mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID);

        mediaSessionManager.onBye(TEST_DEVICE_ID);

        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.CLOSED, session.getStatus());
    }

    @Test
    public void testOnMediaStatus_ClosesSession() {
        mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID);

        mediaSessionManager.onMediaStatus(TEST_DEVICE_ID, "121");

        MediaSessionDTO session = mediaSessionManager.getByCallId(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.CLOSED, session.getStatus());
    }

    // ================================
    // 完整生命周期
    // ================================

    @Test
    public void testCompleteLifecycle() {
        // 1. INVITE OK -> ACTIVE
        Long id = mediaSessionManager.onInviteOk(TEST_CALL_ID, TEST_DEVICE_ID);
        assertNotNull(id);
        assertEquals(MediaSessionConstant.Status.ACTIVE, mediaSessionManager.getByCallId(TEST_CALL_ID).getStatus());

        // 2. ACK -> 保持 ACTIVE
        mediaSessionManager.onAck(TEST_CALL_ID);
        assertEquals(MediaSessionConstant.Status.ACTIVE, mediaSessionManager.getByCallId(TEST_CALL_ID).getStatus());

        // 3. BYE -> CLOSED
        mediaSessionManager.onBye(TEST_DEVICE_ID);
        assertEquals(MediaSessionConstant.Status.CLOSED, mediaSessionManager.getByCallId(TEST_CALL_ID).getStatus());
    }
}
