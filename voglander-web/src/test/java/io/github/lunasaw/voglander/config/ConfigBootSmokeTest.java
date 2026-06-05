package io.github.lunasaw.voglander.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * D1/D2 启动阻断回归红线（确定性，不绑端口/不依赖 Redis/SIP 上下文）。
 * <p>
 * 通过 Spring Boot 真实启动所用的 {@link YamlPropertySourceLoader} 加载 {@code application-inte.yml}：
 * 该 loader 内部 {@code allowDuplicateKeys=false}，与应用启动期 YAML 解析行为一致。
 * <ul>
 *   <li><b>D1</b>：若存在重复顶层键（{@code zlm:}/{@code gateway:}），加载即抛 {@code DuplicateKeyException}；
 *       合并修复后应正常加载。</li>
 *   <li><b>D2</b>：原 {@code gateway.nodes: ${local.gateway.nodes:{}}} 死配置行（占位符默认 "{}" 无法绑定
 *       {@code Map<String,String>} → {@code ConverterNotFoundException}）已删除；断言该键<b>不存在</b>。</li>
 *   <li>合并正确性：合并后的 {@code zlm.hook.auth.*} 与 {@code gateway.internal-auth.*} 仍在单一父键下可见。</li>
 * </ul>
 *
 * @author luna
 */
@DisplayName("D1/D2 — application-inte.yml 重复键 + gateway.nodes 绑定")
class ConfigBootSmokeTest {

    private static final String INTE_YAML = "application-inte.yml";

    private PropertySource<?> loadInteYaml() throws Exception {
        // 与 Spring Boot 启动同源的 loader（allowDuplicateKeys=false）；重复键在此即抛
        List<PropertySource<?>> sources =
            new YamlPropertySourceLoader().load("inte-test", new ClassPathResource(INTE_YAML));
        assertNotNull(sources, "application-inte.yml 应可加载");
        assertFalse(sources.isEmpty(), "应解析出至少一个 PropertySource");
        return sources.get(0);
    }

    @Test
    @DisplayName("D1：application-inte.yml 无重复顶层键，YAML 解析不抛 DuplicateKeyException")
    void inteYaml_noDuplicateKeys_loadsClean() {
        assertDoesNotThrow(this::loadInteYaml,
            "application-inte.yml 含重复 zlm:/gateway: 键会抛 DuplicateKeyException（D1 启动阻断回归）");
    }

    @Test
    @DisplayName("D2：gateway.nodes 死配置行已删除（无不可绑定的 \"{}\" 占位符）")
    void inteYaml_gatewayNodesPlaceholderRemoved() throws Exception {
        PropertySource<?> ps = loadInteYaml();
        assertFalse(ps.containsProperty("gateway.nodes"),
            "gateway.nodes 死配置行应删除，避免 ${local.gateway.nodes:{}} 绑定 Map 抛 ConverterNotFoundException（D2）");
    }

    @Test
    @DisplayName("合并正确性：zlm.hook.auth 与 gateway.internal-auth 在单一父键下均可见")
    void inteYaml_mergedBlocksPresent() throws Exception {
        PropertySource<?> ps = loadInteYaml();
        // D1 合并自原重复 zlm: 块
        assertTrue(ps.containsProperty("zlm.hook.auth.enabled"),
            "合并后 zlm.hook.auth.enabled 应可见（原重复 zlm: 块已并入单一 zlm:）");
        // 原 zlm: 主块内容仍在
        assertTrue(ps.containsProperty("zlm.enable"), "zlm.enable 应仍可见");
        // D1 合并自原重复 gateway: 块
        assertTrue(ps.containsProperty("gateway.internal-auth.shared-secret"),
            "合并后 gateway.internal-auth.shared-secret 应可见（原重复 gateway: 块已并入单一 gateway:）");
        // 原 gateway: 主块内容仍在
        assertTrue(ps.containsProperty("gateway.node-id"), "gateway.node-id 应仍可见");
    }
}
