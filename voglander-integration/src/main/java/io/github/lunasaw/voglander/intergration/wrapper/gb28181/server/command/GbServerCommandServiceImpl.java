package io.github.lunasaw.voglander.intergration.wrapper.gb28181.server.command;

import com.luna.common.dto.ResultDTO;
import com.luna.common.dto.ResultDTOUtils;
import io.github.lunasaw.gbproxy.server.transimit.cmd.ServerSendCmd;
import io.github.lunasaw.gbproxy.server.user.SipUserGenerateServer;
import io.github.lunasaw.sip.common.entity.Device;
import io.github.lunasaw.sip.common.entity.FromDevice;
import io.github.lunasaw.sip.common.entity.ToDevice;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * GB28181设备命令服务实现类
 * 处理GB28181协议的设备命令操作，包括通道查询和设备查询
 *
 * @author luna
 * @date 2023/12/31
 */
@Slf4j
@Service(value = DeviceConstant.DeviceCommandService.DEVICE_AGREEMENT_SERVICE_NAME_GB28181)
public class GbServerCommandServiceImpl implements DeviceCommandService {

    @Autowired
    private SipUserGenerateServer sipUserGenerateServer;

    @Override
    public ResultDTO<Void> queryChannel(DeviceQueryReq deviceQueryReq) {
        try {
            log.info("GB28181设备通道查询开始，设备ID：{}", deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "null");

            // 参数校验
            if (deviceQueryReq == null) {
                log.error("GB28181设备通道查询失败：设备查询请求不能为空");
                return ResultDTOUtils.failure(ServiceExceptionEnum.PARAM_ERROR.getCode(), "设备查询请求不能为空");
            }

            if (deviceQueryReq.getDeviceId() == null || deviceQueryReq.getDeviceId().trim().isEmpty()) {
                log.error("GB28181设备通道查询失败：设备ID不能为空");
                return ResultDTOUtils.failure(ServiceExceptionEnum.PARAM_ERROR.getCode(), "设备ID不能为空");
            }

            // 获取设备信息
            FromDevice fromDevice = getDevice();
            ToDevice toDevice = getToDevice(deviceQueryReq);

            // 发送通道查询命令
            ServerSendCmd.deviceCatalogQuery(fromDevice, toDevice);

            log.info("GB28181设备通道查询成功，设备ID：{}", deviceQueryReq.getDeviceId());
            return ResultDTOUtils.success();

        } catch (ServiceException e) {
            log.error("GB28181设备通道查询业务异常，设备ID：{}，错误：{}",
                deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "unknown", e.getMessage());
            return ResultDTOUtils.failure(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("GB28181设备通道查询系统异常，设备ID：{}，错误：{}",
                deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "unknown", e.getMessage(), e);
            return ResultDTOUtils.failure(ServiceExceptionEnum.UNKNOWN.getCode(), "设备通道查询失败：" + e.getMessage());
        }
    }

    @Override
    public ResultDTO<Void> queryDevice(DeviceQueryReq deviceQueryReq) {
        try {
            log.info("GB28181设备信息查询开始，设备ID：{}", deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "null");

            // 参数校验
            if (deviceQueryReq == null) {
                log.error("GB28181设备信息查询失败：设备查询请求不能为空");
                return ResultDTOUtils.failure(ServiceExceptionEnum.PARAM_ERROR.getCode(), "设备查询请求不能为空");
            }

            if (deviceQueryReq.getDeviceId() == null || deviceQueryReq.getDeviceId().trim().isEmpty()) {
                log.error("GB28181设备信息查询失败：设备ID不能为空");
                return ResultDTOUtils.failure(ServiceExceptionEnum.PARAM_ERROR.getCode(), "设备ID不能为空");
            }

            // 获取设备信息
            FromDevice fromDevice = getDevice();
            ToDevice toDevice = getToDevice(deviceQueryReq);

            // 发送设备信息查询命令
            ServerSendCmd.deviceInfo(fromDevice, toDevice);

            log.info("GB28181设备信息查询成功，设备ID：{}", deviceQueryReq.getDeviceId());
            return ResultDTOUtils.success();

        } catch (ServiceException e) {
            log.error("GB28181设备信息查询业务异常，设备ID：{}，错误：{}",
                deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "unknown", e.getMessage());
            return ResultDTOUtils.failure(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("GB28181设备信息查询系统异常，设备ID：{}，错误：{}",
                deviceQueryReq != null ? deviceQueryReq.getDeviceId() : "unknown", e.getMessage(), e);
            return ResultDTOUtils.failure(ServiceExceptionEnum.UNKNOWN.getCode(), "设备信息查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取目标设备信息
     *
     * @param deviceQueryReq 设备查询请求
     * @return ToDevice 目标设备
     * @throws ServiceException 当设备信息获取失败时抛出业务异常
     */
    private ToDevice getToDevice(DeviceQueryReq deviceQueryReq) {
        try {
            Device toDevice = sipUserGenerateServer.getToDevice(deviceQueryReq.getDeviceId());
            if (toDevice == null) {
                throw new ServiceException(ServiceExceptionEnum.PARAM_ERROR.getCode(), "目标设备不存在或未注册：" + deviceQueryReq.getDeviceId());
            }
            return (ToDevice)toDevice;
        } catch (ClassCastException e) {
            log.error("目标设备类型转换失败，设备ID：{}，错误：{}", deviceQueryReq.getDeviceId(), e.getMessage());
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "目标设备类型不正确");
        } catch (Exception e) {
            log.error("获取目标设备信息失败，设备ID：{}，错误：{}", deviceQueryReq.getDeviceId(), e.getMessage());
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "获取目标设备信息失败：" + e.getMessage());
        }
    }

    /**
     * 获取源设备信息
     *
     * @return FromDevice 源设备
     * @throws ServiceException 当设备信息获取失败时抛出业务异常
     */
    private FromDevice getDevice() {
        try {
            Device fromDevice = sipUserGenerateServer.getFromDevice();
            if (fromDevice == null) {
                throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "源设备信息获取失败");
            }
            return (FromDevice)fromDevice;
        } catch (ClassCastException e) {
            log.error("源设备类型转换失败，错误：{}", e.getMessage());
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "源设备类型不正确");
        } catch (Exception e) {
            log.error("获取源设备信息失败，错误：{}", e.getMessage());
            throw new ServiceException(ServiceExceptionEnum.BUSINESS_EXCEPTION.getCode(), "获取源设备信息失败：" + e.getMessage());
        }
    }
}
