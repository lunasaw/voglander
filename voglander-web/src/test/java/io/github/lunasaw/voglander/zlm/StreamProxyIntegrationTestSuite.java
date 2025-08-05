package io.github.lunasaw.voglander.zlm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * StreamProxy集成测试套件
 * 
 * 包含所有StreamProxy相关的集成测试：
 * 1. 端到端流程测试
 * 2. HTTP API测试
 * 3. 性能和并发测试
 * 4. Hook回调处理测试
 * 5. 缓存一致性测试
 * 6. 异常处理测试
 *
 * @author luna
 * @date 2025-01-23
 */
@Suite
@SelectClasses({
    StreamProxyEndToEndIntegrationTest.class,
    StreamProxyHttpApiIntegrationTest.class,
    StreamProxyPerformanceIntegrationTest.class
})
@DisplayName("StreamProxy集成测试套件")
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class StreamProxyIntegrationTestSuite {

    // 测试套件入口类，包含所有StreamProxy相关的集成测试
    // 运行此类将执行所有相关的集成测试
}