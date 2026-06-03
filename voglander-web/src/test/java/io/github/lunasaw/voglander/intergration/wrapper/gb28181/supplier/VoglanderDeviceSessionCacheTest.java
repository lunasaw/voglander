package io.github.lunasaw.voglander.intergration.wrapper.gb28181.supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.lunasaw.sip.common.entity.ToDevice;

/**
 * TC-05：VoglanderDeviceSessionCache 单元测试（零依赖）。
 */
@ExtendWith(MockitoExtension.class)
class VoglanderDeviceSessionCacheTest {

    @Mock
    private VoglanderServerDeviceSupplier supplier;

    @InjectMocks
    private VoglanderDeviceSessionCache cache;

    @Test
    void getToDevice_delegates_to_supplier() {
        ToDevice expected = new ToDevice();
        expected.setUserId("34020000001310000001");
        when(supplier.getToDevice("34020000001310000001")).thenReturn(expected);

        assertThat(cache.getToDevice("34020000001310000001")).isSameAs(expected);
    }

    @Test
    void getToDevice_returns_null_for_unregistered_device() {
        when(supplier.getToDevice("unknown")).thenReturn(null);
        assertThat(cache.getToDevice("unknown")).isNull();
    }
}
