package io.github.lunasaw.voglander.intergration.wrapper.gb28181.lab;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LabSessionHolder 单元测试：apply/current/reset、空回退、hasOverride 各字段判定。
 */
class LabSessionHolderTest {

    private final LabSessionHolder holder = new LabSessionHolder();

    @Test
    @DisplayName("初始 current() 为 null（自环）")
    void initial_current_null() {
        assertThat(holder.current()).isNull();
    }

    @Test
    @DisplayName("apply 后 current() 返回该快照")
    void apply_then_current() {
        LabSessionHolder.Snapshot s = new LabSessionHolder.Snapshot(
            "srv", "1.2.3.4", 5060, "domain", "TCP", "cli", "pwd");
        holder.apply(s);
        assertThat(holder.current()).isSameAs(s);
        assertThat(holder.current().getServerId()).isEqualTo("srv");
        assertThat(holder.current().getServerPort()).isEqualTo(5060);
        assertThat(holder.current().getTransport()).isEqualTo("TCP");
        assertThat(holder.current().getClientId()).isEqualTo("cli");
        assertThat(holder.current().getClientPassword()).isEqualTo("pwd");
    }

    @Test
    @DisplayName("reset 后回 null")
    void reset_then_null() {
        holder.apply(new LabSessionHolder.Snapshot("a", null, null, null, null, null, null));
        holder.reset();
        assertThat(holder.current()).isNull();
    }

    @Test
    @DisplayName("hasOverride：全空 → false")
    void hasOverride_allBlank_false() {
        assertThat(LabSessionHolder.hasOverride(null, null, null, null, null, null, null)).isFalse();
        assertThat(LabSessionHolder.hasOverride("", "  ", null, "", " ", "", "")).isFalse();
    }

    @Test
    @DisplayName("hasOverride：任一字段非空 → true")
    void hasOverride_anyField_true() {
        assertThat(LabSessionHolder.hasOverride("srv", null, null, null, null, null, null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, "ip", null, null, null, null, null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, null, 5060, null, null, null, null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, null, null, "dom", null, null, null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, null, null, null, "UDP", null, null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, null, null, null, null, "cli", null)).isTrue();
        assertThat(LabSessionHolder.hasOverride(null, null, null, null, null, null, "pwd")).isTrue();
    }
}
