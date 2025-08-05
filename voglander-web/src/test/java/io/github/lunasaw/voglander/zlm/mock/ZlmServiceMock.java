package io.github.lunasaw.voglander.zlm.mock;

import io.github.lunasaw.zlm.entity.ServerResponse;
import io.github.lunasaw.zlm.entity.StreamKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;

/**
 * ZLM服务Mock配置
 * 用于集成测试中模拟ZLMediaKit外部服务调用
 *
 * 注意：ZlmRestService是静态方法类，无法直接Mock
 * 如果需要Mock外部ZLM调用，需要创建wrapper接口或使用PowerMock
 *
 * @author luna
 * @date 2025-01-23
 */
@Slf4j
@TestConfiguration
public class ZlmServiceMock {

    /**
     * 创建成功的添加代理响应对象（用于测试数据构造）
     */
    public static ServerResponse<StreamKey> createSuccessResponse() {
        ServerResponse<StreamKey> response = new ServerResponse<>();
        response.setCode(0);
        response.setMsg("success");

        StreamKey streamKey = new StreamKey();
        streamKey.setKey("test-proxy-key-123");
        response.setData(streamKey);

        return response;
    }

    /**
     * 创建删除成功的响应对象（用于测试数据构造）
     */
    public static ServerResponse<StreamKey.StringDelFlag> createDeleteSuccessResponse() {
        ServerResponse<StreamKey.StringDelFlag> response = new ServerResponse<>();
        response.setCode(0);
        response.setMsg("success");

        StreamKey.StringDelFlag delFlag = new StreamKey.StringDelFlag();
        delFlag.setFlag("1"); // 表示删除了1个代理
        response.setData(delFlag);

        return response;
    }
}