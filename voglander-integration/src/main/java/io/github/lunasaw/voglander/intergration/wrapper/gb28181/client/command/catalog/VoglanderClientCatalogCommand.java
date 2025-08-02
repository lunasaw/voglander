package io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.catalog;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.luna.common.dto.ResultDTO;

import io.github.lunasaw.gb28181.common.entity.response.DeviceItem;
import io.github.lunasaw.gb28181.common.entity.response.DeviceResponse;
import io.github.lunasaw.gbproxy.client.transmit.cmd.ClientCommandSender;
import io.github.lunasaw.voglander.intergration.wrapper.gb28181.client.command.AbstractVoglanderClientCommand;

/**
 * GB28181客户端设备目录查询指令实现类
 * <p>
 * 提供设备目录查询相关的指令发送功能，包括设备目录响应、设备列表响应等操作。
 * 继承AbstractVoglanderClientCommand获得统一的异常处理和日志记录能力。
 * </p>
 * 
 * <h3>支持的目录操作</h3>
 * <ul>
 * <li>设备目录响应 - {@link DeviceResponse}</li>
 * <li>设备列表响应 - {@link DeviceItem}</li>
 * <li>单个设备项响应</li>
 * <li>设备通道更新通知</li>
 * </ul>
 * 
 * <h3>使用示例</h3>
 * 
 * <pre>
 * {@code @Autowired
 * private VoglanderClientCatalogCommand catalogCommand;
 * 
 * // 发送设备目录响应
 * DeviceResponse deviceResponse = new DeviceResponse();
 * deviceResponse.setName("目录查询");
 * deviceResponse.setSumNum(5);
 * ResultDTO<Void> result = catalogCommand.sendCatalogCommand("34020000001320000001", deviceResponse);
 * 
 * // 发送设备列表
 * List<DeviceItem> deviceItems = Arrays.asList(deviceItem1, deviceItem2);
 * ResultDTO<Void> result2 = catalogCommand.sendDeviceItemsCommand("34020000001320000001", deviceItems);
 * }
 * </pre>
 * 
 * @author luna
 * @since 2025/8/1
 * @version 1.0
 */
@Component
@ConditionalOnProperty(name = "sip.client.enabled", havingValue = "true")
public class VoglanderClientCatalogCommand extends AbstractVoglanderClientCommand {

    /**
     * 发送设备目录响应指令
     * <p>
     * 向平台发送设备目录查询的响应信息，包含设备统计和详细信息。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceResponse 设备响应对象，包含目录查询结果
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或设备响应对象为空时抛出
     */
    public ResultDTO<Void> sendCatalogCommand(String deviceId, DeviceResponse deviceResponse) {
        validateDeviceId(deviceId, "发送设备目录响应指令时设备ID不能为空");
        validateNotNull(deviceResponse, "设备响应对象不能为空");

        return executeCommand("sendCatalogCommand", deviceId,
            () -> ClientCommandSender.sendCatalogCommand(getClientFromDevice(), getToDevice(deviceId), deviceResponse),
            deviceResponse);
    }

    /**
     * 发送设备列表响应指令
     * <p>
     * 对外只能使用 {@link VoglanderClientCatalogCommand#sendCatalogCommand(String, DeviceResponse)}
     * 向平台发送设备列表，通常用于目录查询的详细响应。
     * 向下传递必须要外包装 @XmlRootElement(name = "Response")
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceItems 设备列表，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或设备列表为空时抛出
     */
    public ResultDTO<Void> sendDeviceItemsCommand(String deviceId, List<DeviceItem> deviceItems) {
        validateDeviceId(deviceId, "发送设备列表响应指令时设备ID不能为空");
        validateNotNull(deviceItems, "设备列表不能为空");

        // 包装为DeviceResponse以确保正确的XML根元素注解
        DeviceResponse deviceResponse = createDeviceResponse(deviceId, "目录查询", deviceItems.size(), deviceItems);
        return sendCatalogCommand(deviceId, deviceResponse);
    }

    /**
     * 发送单个设备项响应指令
     * <p>
     * 向平台发送单个设备项信息。
     * 向下传递必须要外包装 @XmlRootElement(name = "Response")
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param deviceItem 设备项对象，不能为空
     * @return ResultDTO<Void> 指令执行结果
     * @throws IllegalArgumentException 当设备ID为空或设备项为空时抛出
     */
    public ResultDTO<Void> sendSingleDeviceItemCommand(String deviceId, DeviceItem deviceItem) {
        validateDeviceId(deviceId, "发送单个设备项响应指令时设备ID不能为空");
        validateNotNull(deviceItem, "设备项不能为空");

        // 包装为DeviceResponse以确保正确的XML根元素注解
        DeviceResponse deviceResponse = createDeviceResponse(deviceId, "设备状态通知", 1, List.of(deviceItem));
        return sendCatalogCommand(deviceId, deviceResponse);
    }

    /**
     * 创建设备响应对象
     * <p>
     * 根据基础参数快速构建设备响应对象的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param name 查询名称
     * @param sumNum 设备总数
     * @param deviceItems 设备列表
     * @return DeviceResponse 设备响应对象
     */
    public DeviceResponse createDeviceResponse(String deviceId, String name, Integer sumNum, List<DeviceItem> deviceItems) {
        DeviceResponse deviceResponse = new DeviceResponse();
        deviceResponse.setDeviceId(deviceId);
        deviceResponse.setName(name != null ? name : "目录查询");
        deviceResponse.setSumNum(sumNum != null ? sumNum : (deviceItems != null ? deviceItems.size() : 0));
        deviceResponse.setCmdType("Catalog"); // 设置命令类型为Catalog，确保XML包含CmdType元素

        if (deviceItems != null && !deviceItems.isEmpty()) {
            deviceResponse.setDeviceList(deviceItems);
        }

        return deviceResponse;
    }

    /**
     * 创建设备项
     * <p>
     * 根据基础参数快速构建设备项的工具方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param name 设备名称
     * @param manufacturer 制造商
     * @param model 设备型号
     * @param owner 设备归属
     * @param civilCode 行政区域
     * @param address 安装地址
     * @param parental 是否有子设备（0-否，1-是）
     * @param parentId 父设备ID
     * @param safetyWay 信令安全模式（0-不采用，2-S/MIME签名方式，3-S/MIME加密签名同时采用方式，4-数字摘要方式）
     * @param registerWay 注册方式（1-符合IETF标准的认证注册模式，2-基于口令的双向认证注册模式，3-基于数字证书的双向认证注册模式）
     * @param secrecy 保密属性（0-不涉密，1-涉密）
     * @param status 设备状态（ON-在线，OFF-离线）
     * @return DeviceItem 设备项
     */
    public DeviceItem createDeviceItem(String deviceId, String name, String manufacturer, String model,
        String owner, String civilCode, String address, Integer parental,
        String parentId, Integer safetyWay, Integer registerWay,
        Integer secrecy, String status) {
        DeviceItem deviceItem = new DeviceItem();
        deviceItem.setDeviceId(deviceId);
        deviceItem.setName(name);
        deviceItem.setManufacturer(manufacturer);
        deviceItem.setModel(model);
        deviceItem.setOwner(owner);
        deviceItem.setCivilCode(civilCode);
        deviceItem.setAddress(address);
        deviceItem.setParental(parental != null ? parental : 0);
        deviceItem.setParentId(parentId);
        deviceItem.setSafetyWay(safetyWay != null ? safetyWay : 0);
        deviceItem.setRegisterWay(registerWay != null ? registerWay : 1);
        deviceItem.setSecrecy(secrecy != null ? secrecy : 0);
        deviceItem.setStatus(status != null ? status : "ON");
        return deviceItem;
    }

    /**
     * 创建简单设备项
     * <p>
     * 使用最少参数创建设备项的简化方法。
     * </p>
     * 
     * @param deviceId 设备ID
     * @param name 设备名称
     * @param status 设备状态
     * @return DeviceItem 设备项
     */
    public DeviceItem createSimpleDeviceItem(String deviceId, String name, String status) {
        return createDeviceItem(deviceId, name, null, null, null, null, null,
            null, null, null, null, null, status);
    }

    /**
     * 发送简化目录响应指令
     * <p>
     * 根据基础参数快速发送目录响应，适用于简单场景。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param queryName 查询名称
     * @param deviceItems 设备列表
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendSimpleCatalogResponse(String deviceId, String queryName, List<DeviceItem> deviceItems) {
        DeviceResponse deviceResponse = createDeviceResponse(deviceId, queryName, null, deviceItems);
        return sendCatalogCommand(deviceId, deviceResponse);
    }

    /**
     * 发送空目录响应指令
     * <p>
     * 发送一个表示没有设备的响应，通常用于查询无结果的情况。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param queryName 查询名称
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendEmptyCatalogResponse(String deviceId, String queryName) {
        DeviceResponse deviceResponse = createDeviceResponse(deviceId, queryName, 0, null);
        return sendCatalogCommand(deviceId, deviceResponse);
    }

    /**
     * 发送设备在线状态通知
     * <p>
     * 快速发送设备在线状态变更通知。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称
     * @param status 设备状态（ON-在线，OFF-离线）
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendDeviceStatusNotify(String deviceId, String name, String status) {
        DeviceItem deviceItem = createSimpleDeviceItem(deviceId, name, status);
        return sendSingleDeviceItemCommand(deviceId, deviceItem);
    }

    /**
     * 发送设备上线通知
     * <p>
     * 快捷方法，用于通知平台设备已上线。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendDeviceOnlineNotify(String deviceId, String name) {
        return sendDeviceStatusNotify(deviceId, name, "ON");
    }

    /**
     * 发送设备离线通知
     * <p>
     * 快捷方法，用于通知平台设备已离线。
     * </p>
     * 
     * @param deviceId 设备ID，不能为空
     * @param name 设备名称
     * @return ResultDTO<Void> 指令执行结果
     */
    public ResultDTO<Void> sendDeviceOfflineNotify(String deviceId, String name) {
        return sendDeviceStatusNotify(deviceId, name, "OFF");
    }
}