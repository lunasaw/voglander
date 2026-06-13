package io.github.lunasaw.voglander.manager.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.voglander.repository.cache.redis.RedisCache;

/**
 * RecordInfoCacheManager 单元测试（S3，纯 Mockito）。
 *
 * @author luna
 */
@DisplayName("RecordInfoCacheManager 测试 (S3)")
@ExtendWith(MockitoExtension.class)
class RecordInfoCacheManagerTest {

    @InjectMocks
    RecordInfoCacheManager manager;

    @Mock
    RedisCache             redisCache;

    @Test
    @DisplayName("put 以 deviceId+sn 组键，带 10 分钟 TTL")
    void put_buildsKeyWithTtl() {
        manager.put("d1", "sn1", "{\"sumNum\":3}");
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(key.capture(), eq("{\"sumNum\":3}"), eq(10), eq(TimeUnit.MINUTES));
        assertTrue(key.getValue().contains("d1"));
        assertTrue(key.getValue().contains("sn1"));
    }

    @Test
    @DisplayName("put deviceId 为空跳过")
    void put_nullDeviceId_skips() {
        manager.put(null, "sn1", "{}");
        verifyNoInteractions(redisCache);
    }

    @Test
    @DisplayName("get 读对应键")
    void get_readsKey() {
        when(redisCache.getCacheObject(anyString())).thenReturn("{\"sumNum\":1}");
        String r = manager.get("d1", "sn1");
        assertEquals("{\"sumNum\":1}", r);
    }

    @Test
    @DisplayName("get/put sn 为空用占位符，键一致可回读")
    void nullSn_usesPlaceholderConsistently() {
        manager.put("d1", null, "{}");
        ArgumentCaptor<String> putKey = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(putKey.capture(), anyString(), anyInt(), any());

        manager.get("d1", null);
        ArgumentCaptor<String> getKey = ArgumentCaptor.forClass(String.class);
        verify(redisCache).getCacheObject(getKey.capture());

        assertEquals(putKey.getValue(), getKey.getValue(), "put/get 在 sn=null 时应生成相同 key");
    }
}
