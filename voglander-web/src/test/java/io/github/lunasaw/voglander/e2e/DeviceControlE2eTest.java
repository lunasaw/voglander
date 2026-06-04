package io.github.lunasaw.voglander.e2e;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;

import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.client.domain.device.qo.DevicePtzReq;
import io.github.lunasaw.voglander.config.MockDeviceAdapter;
import io.github.lunasaw.voglander.repository.entity.DeviceDO;
import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import io.github.lunasaw.voglander.service.command.impl.GbDeviceCommandService;
import lombok.extern.slf4j.Slf4j;

/**
 * TC-PTZ-01/02：设备 PTZ 控制端到端测试（真实 SIP 协议栈）。
 * 链路：GbDeviceCommandService.ptzControl → ServerCommandSender → SIP/UDP → MockDeviceAdapter.onPtzControl
 */
@Slf4j
class DeviceControlE2eTest extends BaseE2eTest {

    private static final String CLIENT_ID  = "34020000001320000001";
    private static final String SERVER_ID  = "34020000002000000001";
    private static final String PASSWORD   = "123456";
    private static final String CHANNEL_ID = "34020000131600000003";

    @Autowired private GbDeviceCommandService commandService;
    @Autowired private DeviceMapper           deviceMapper;
    @Autowired private MockDeviceAdapter      mockDevice;

    @BeforeEach
    void register() throws InterruptedException {
        ClientCommandSender.sendRegisterCommand(from(), to(), 3600);
        await().atMost(5, SECONDS).until(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            return d != null && d.getStatus() == 1;
        });
        // 等异步 DeviceInfo/Catalog 回写完成
        Thread.sleep(1000);
        // 强制修正 port=5061、transport=UDP，并循环确认没被异步线程覆盖
        await().atMost(3, SECONDS).pollInterval(200, java.util.concurrent.TimeUnit.MILLISECONDS).until(() -> {
            DeviceDO d = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            if (d == null) return false;
            d.setPort(5061);
            String ext = d.getExtend();
            if (ext != null) ext = ext.replace("\"transport\":\"TCP\"", "\"transport\":\"UDP\"");
            d.setExtend(ext);
            deviceMapper.updateById(d);
            // 再读一次确认写入持久
            DeviceDO check = deviceMapper.selectOne(
                Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
            return check != null && check.getPort() == 5061;
        });
        mockDevice.drainPtzQueue();
    }

    @AfterEach
    void cleanup() {
        deviceMapper.delete(Wrappers.<DeviceDO>lambdaQuery().eq(DeviceDO::getDeviceId, CLIENT_ID));
    }

    @Test
    @DisplayName("TC-PTZ-01 服务端发 PTZ UP → MockDevice 收到 PTZ 命令")
    void ptzControl_up_receivedByDevice() throws InterruptedException {
        mockDevice.drainPtzQueue(); // 丢弃历史 UDP 重传残留

        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId(CLIENT_ID);
        req.setControl("TILT_UP");
        req.setSpeed(100);
        commandService.ptzControl(req);

        assertThat(mockDevice.pollPtz(5, TimeUnit.SECONDS)).as("MockDevice 未收到 PTZ 命令").isNotNull();
        log.info("✅ TC-PTZ-01 通过");
    }

    @Test
    @DisplayName("TC-PTZ-02 服务端发 PTZ STOP → MockDevice 收到 PTZ 命令")
    void ptzControl_stop_receivedByDevice() throws InterruptedException {
        mockDevice.drainPtzQueue();

        DevicePtzReq req = new DevicePtzReq();
        req.setDeviceId(CLIENT_ID);
        req.setControl("STOP");
        req.setSpeed(128);
        commandService.ptzControl(req);

        assertThat(mockDevice.pollPtz(5, TimeUnit.SECONDS)).as("MockDevice 未收到 PTZ STOP 命令").isNotNull();
        log.info("✅ TC-PTZ-02 通过");
    }

    private FromDevice from() { return FromDevice.getInstance(CLIENT_ID, "127.0.0.1", 5061); }

    private ToDevice to() {
        ToDevice t = ToDevice.getInstance(SERVER_ID, "127.0.0.1", 5060);
        t.setPassword(PASSWORD);
        return t;
    }
}
