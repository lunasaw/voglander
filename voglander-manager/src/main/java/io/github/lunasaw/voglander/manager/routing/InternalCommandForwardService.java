package io.github.lunasaw.voglander.manager.routing;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson2.JSON;
import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import com.luna.common.dto.constant.ResultCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 内部命令转发服务：将命令转发到目标节点。
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "voglander.command.affinity-route.enabled", havingValue = "true", matchIfMissing = false)
public class InternalCommandForwardService {

    private static final int RETRY_COUNT    = 3;
    private static final int RETRY_DELAY_MS = 200;

    @Autowired
    private NodeAliveService nodeAliveService;

    @Value("${gateway.internal-auth.shared-secret:CHANGE_ME_IN_PROD}")
    private String secret;

    @Value("#{${gateway.nodes:{}}}")
    private Map<String, String> nodes;

    private final RestTemplate restTemplate = new RestTemplate();

    public ResultDTO<Void> forward(String targetNodeId, Object envelope) {
        String hostPort = nodes.get(targetNodeId);
        if (hostPort == null) {
            log.warn("forward::目标节点地址未配置, targetNodeId={}", targetNodeId);
            return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "target node not configured");
        }
        String url = "http://" + hostPort + "/internal/sip/command";
        String body = JSON.toJSONString(envelope);

        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                long ts = System.currentTimeMillis();
                String sig = hmacSha256(secret, targetNodeId + ":" + ts);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-Internal-Sig", sig);
                headers.set("X-Internal-Ts", String.valueOf(ts));
                headers.set("X-Node-Id", nodeAliveService.getLocalNodeId());

                restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
                log.info("forward::命令转发成功, targetNodeId={}", targetNodeId);
                return ResultDTOUtils.success();
            } catch (Exception e) {
                log.warn("forward::命令转发失败(第{}次), targetNodeId={}, error={}", i + 1, targetNodeId, e.getMessage());
                if (i < RETRY_COUNT - 1) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }
            }
        }
        return ResultDTOUtils.failure(ResultCode.ERROR_SYSTEM_EXCEPTION, "forward failed after retries");
    }

    private static String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
