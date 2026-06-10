package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LabChannelHolder 单元测试：默认 4/Lab-ch、update 取值、count≤0 与空白前缀回落默认。
 */
class LabChannelHolderTest {

    private final LabChannelHolder holder = new LabChannelHolder();

    @Test
    @DisplayName("初始默认 4 通道 / Lab-ch 前缀")
    void default_config() {
        LabChannelHolder.Config c = holder.current();
        assertThat(c.getCount()).isEqualTo(LabChannelHolder.DEFAULT_COUNT).isEqualTo(4);
        assertThat(c.getNamePrefix()).isEqualTo(LabChannelHolder.DEFAULT_NAME_PREFIX).isEqualTo("Lab-ch");
    }

    @Test
    @DisplayName("update 正常值后取值生效")
    void update_normal() {
        holder.update(8, "Cam");
        assertThat(holder.current().getCount()).isEqualTo(8);
        assertThat(holder.current().getNamePrefix()).isEqualTo("Cam");
    }

    @Test
    @DisplayName("count<=0 或 null → 回落默认 4")
    void update_count_fallback() {
        holder.update(0, "X");
        assertThat(holder.current().getCount()).isEqualTo(4);
        holder.update(-1, "X");
        assertThat(holder.current().getCount()).isEqualTo(4);
        holder.update(null, "X");
        assertThat(holder.current().getCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("namePrefix 空白 → 回落默认 Lab-ch")
    void update_prefix_fallback() {
        holder.update(3, null);
        assertThat(holder.current().getNamePrefix()).isEqualTo("Lab-ch");
        holder.update(3, "   ");
        assertThat(holder.current().getNamePrefix()).isEqualTo("Lab-ch");
    }
}
