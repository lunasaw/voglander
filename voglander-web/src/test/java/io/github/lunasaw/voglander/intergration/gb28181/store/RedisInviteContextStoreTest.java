package io.github.lunasaw.voglander.intergration.gb28181.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import io.github.lunasaw.sipgateway.gb28181.store.InviteContext;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.store.RedisInviteContextStore;

/**
 * RedisInviteContextStore 单元测试
 *
 * <p>
 * 验证以下契约：
 * </p>
 * <ul>
 * <li>save：序列化为 JSON，按 ttl 设置到 Redis</li>
 * <li>find：缺失返回 null（业务回包 410），后端故障抛 503</li>
 * <li>remove：删除失败仅告警（依赖 TTL 兜底），不抛异常</li>
 * <li>序列化损坏：抛 503</li>
 * </ul>
 *
 * @author luna
 * @since 2026/06/01
 */
@DisplayName("RedisInviteContextStore 单元测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RedisInviteContextStoreTest {

    private static final String      CALL_ID    = "test-call-id-12345";
    private static final String      KEY        = "sip:invite:ctx:" + CALL_ID;
    private static final String      NODE_ID    = "node-1";
    private static final String      CTX_KEY    = "ctx-key-abc";
    private static final long        TTL_MS     = 30000L;

    @Mock
    private StringRedisTemplate      redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private RedisInviteContextStore  store;

    @BeforeEach
    public void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    @DisplayName("save → JSON 序列化 + TTL 写入 Redis")
    public void saveSerializesAndSetsTtl() {
        InviteContext ctx = new InviteContext(NODE_ID, CTX_KEY);

        store.save(CALL_ID, ctx, TTL_MS);

        verify(valueOps).set(eq(KEY), any(String.class), eq(Duration.ofMillis(TTL_MS)));
    }

    @Test
    @DisplayName("save Redis 后端故障 → 抛 503 ResponseStatusException")
    public void saveRedisFailureThrowsServiceUnavailable() {
        doThrow(new RedisConnectionFailureException("redis down"))
            .when(valueOps).set(any(), any(), any(Duration.class));

        InviteContext ctx = new InviteContext(NODE_ID, CTX_KEY);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> store.save(CALL_ID, ctx, TTL_MS));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("find 命中 → 反序列化为 InviteContext")
    public void findHitDeserializes() {
        // FastJSON2 record 序列化结果
        String json = "{\"nodeId\":\"" + NODE_ID + "\",\"ctxKey\":\"" + CTX_KEY + "\"}";
        when(valueOps.get(KEY)).thenReturn(json);

        InviteContext result = store.find(CALL_ID);

        assertNotNull(result);
        assertEquals(NODE_ID, result.nodeId());
        assertEquals(CTX_KEY, result.ctxKey());
    }

    @Test
    @DisplayName("find 缺失 → 返回 null（业务回包 410）")
    public void findMissReturnsNull() {
        when(valueOps.get(KEY)).thenReturn(null);

        InviteContext result = store.find(CALL_ID);
        assertNull(result);
    }

    @Test
    @DisplayName("find Redis 后端故障 → 抛 503")
    public void findRedisFailureThrowsServiceUnavailable() {
        when(valueOps.get(KEY))
            .thenThrow(new RedisConnectionFailureException("redis down"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> store.find(CALL_ID));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("find 数据损坏 → 抛 503")
    public void findCorruptedDataThrows() {
        when(valueOps.get(KEY)).thenReturn("not-a-valid-json");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> store.find(CALL_ID));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    }

    @Test
    @DisplayName("remove 删除指定 key")
    public void removeDeletesKey() {
        store.remove(CALL_ID);
        verify(redisTemplate).delete(KEY);
    }

    @Test
    @DisplayName("remove 删除失败 → 仅告警，不抛异常（TTL 兜底）")
    public void removeFailureDoesNotThrow() {
        when(redisTemplate.delete(KEY))
            .thenThrow(new RedisConnectionFailureException("redis down"));

        // 不应抛异常
        store.remove(CALL_ID);
    }
}
