package io.github.lunasaw.voglander.config;

import com.luna.common.text.RandomStrUtil;
import io.github.lunasaw.gb28181.common.entity.control.DeviceControlPtz;
import io.github.lunasaw.gb28181.common.entity.enums.CmdTypeEnum;
import io.github.lunasaw.gb28181.common.entity.query.DeviceQuery;
import io.github.lunasaw.gb28181.common.entity.response.DeviceInfo;
import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.api.ClientGb28181Adapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 测试用 mock 设备适配器。
 * 响应平台发来的 DeviceInfo / Catalog 查询，捕获 PTZ 控制命令。
 */
@Slf4j
@Component
public class MockDeviceAdapter extends ClientGb28181Adapter {

    public static final String MOCK_DEVICE_NAME  = "MockCamera";
    public static final String MOCK_MANUFACTURER = "MockCorp";
    public static final String MOCK_CHANNEL_ID   = "34020000001310000001";
    public static final String MOCK_CHANNEL_NAME = "MockChannel";

    /** PTZ 命令队列；测试调用 pollPtz() 阻塞等待下一条 */
    private final LinkedBlockingQueue<DeviceControlPtz> ptzQueue = new LinkedBlockingQueue<>();

    /** 清空队列（在 ptzControl 发出前调用，丢弃任何历史重传残留） */
    public void drainPtzQueue() {
        ptzQueue.clear();
    }

    /** 等待下一条 PTZ 命令，超时返回 null */
    public DeviceControlPtz pollPtz(long timeout, TimeUnit unit) throws InterruptedException {
        return ptzQueue.poll(timeout, unit);
    }

    @Override
    public DeviceInfo onDeviceInfoQuery(String platformId, DeviceQuery query) {
        log.info("MockDeviceAdapter: 收到 DeviceInfo 查询, deviceId={}", query.getDeviceId());
        DeviceInfo info = new DeviceInfo(CmdTypeEnum.DEVICE_INFO.getType(),
                RandomStrUtil.getValidationCode(), query.getDeviceId());
        info.setDeviceName(MOCK_DEVICE_NAME);
        info.setManufacturer(MOCK_MANUFACTURER);
        info.setResult("OK");
        return info;
    }

    @Override
    public DeviceResponse onCatalogQuery(String platformId, DeviceQuery query) {
        log.info("MockDeviceAdapter: 收到 Catalog 查询, deviceId={}", query.getDeviceId());
        DeviceItem item = new DeviceItem();
        item.setDeviceId(MOCK_CHANNEL_ID);
        item.setName(MOCK_CHANNEL_NAME);
        DeviceResponse response = new DeviceResponse(CmdTypeEnum.CATALOG.getType(),
                RandomStrUtil.getValidationCode(), query.getDeviceId());
        response.setDeviceItemList(List.of(item));
        return response;
    }

    @Override
    public void onPtzControl(String platformId, DeviceControlPtz cmd) {
        log.info("MockDeviceAdapter: 收到 PTZ 控制, platformId={}, hex={}", platformId, cmd.getPtzCmd());
        ptzQueue.offer(cmd);
    }
}
