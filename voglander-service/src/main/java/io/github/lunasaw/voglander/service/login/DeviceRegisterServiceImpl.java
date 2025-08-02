package io.github.lunasaw.voglander.service.login;

import java.time.LocalDateTime;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import io.github.lunasaw.voglander.client.domain.device.qo.DeviceChannelReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceInfoReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceQueryReq;
import io.github.lunasaw.voglander.client.domain.device.qo.DeviceRegisterReq;
import io.github.lunasaw.voglander.client.service.device.DeviceCommandService;
import io.github.lunasaw.voglander.client.service.device.DeviceRegisterService;
import io.github.lunasaw.voglander.common.constant.device.DeviceConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceChannelDTO;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import io.github.lunasaw.voglander.manager.manager.DeviceChannelManager;
import io.github.lunasaw.voglander.manager.manager.DeviceManager;
import io.github.lunasaw.voglander.service.command.DeviceAgreementService;
import lombok.extern.slf4j.Slf4j;

/**
 * 设备注册服务实现类
 * 处理设备注册、登录、心跳等核心业务
 *
 * @author luna
 * @date 2023/12/29
 */
@Slf4j
@Service
public class DeviceRegisterServiceImpl implements DeviceRegisterService {

    @Autowired
    private DeviceManager          deviceManager;

    @Autowired
    private DeviceChannelManager   deviceChannelManager;

    @Autowired
    private DeviceAgreementService deviceAgreementService;

    @Override
    public AjaxResult<Void> login(DeviceRegisterReq deviceRegisterReq) {
        try {
            Assert.notNull(deviceRegisterReq, "设备注册请求不能为空");
            Assert.hasText(deviceRegisterReq.getDeviceId(), "设备ID不能为空");

            log.info("设备登录请求开始，设备ID：{}", deviceRegisterReq.getDeviceId());

            DeviceDTO dto = DeviceDTO.req2dto(deviceRegisterReq);
            Long deviceId = deviceManager.saveOrUpdate(dto);

            log.info("设备登录成功，设备ID：{}，数据库ID：{}", deviceRegisterReq.getDeviceId(), deviceId);

            // 获取设备命令服务并查询设备信息
            try {
                DeviceCommandService deviceCommandService = deviceAgreementService.getCommandService(dto.getType());
                if (deviceCommandService != null) {
                    DeviceQueryReq deviceQueryReq = new DeviceQueryReq();
                    deviceQueryReq.setDeviceId(dto.getDeviceId());

                    // 异步查询设备信息（避免阻塞主流程）
                    try {
                        deviceCommandService.queryDevice(deviceQueryReq);
                        log.debug("设备信息查询已发起，设备ID：{}", dto.getDeviceId());
                    } catch (Exception e) {
                        log.warn("查询设备信息失败，设备ID：{}，错误：{}", dto.getDeviceId(), e.getMessage());
                    }

                    // 异步查询通道信息
                    try {
                        deviceCommandService.queryChannel(deviceQueryReq);
                        log.debug("设备通道查询已发起，设备ID：{}", dto.getDeviceId());
                    } catch (Exception e) {
                        log.warn("查询设备通道失败，设备ID：{}，错误：{}", dto.getDeviceId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("获取设备命令服务失败，设备ID：{}，设备类型：{}，错误：{}",
                    dto.getDeviceId(), dto.getType(), e.getMessage());
            }

            return AjaxResult.success("设备登录成功");

        } catch (ServiceException e) {
            log.error("设备登录失败，设备ID：{}，业务错误：{}",
                deviceRegisterReq != null ? deviceRegisterReq.getDeviceId() : "unknown", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("设备登录失败，设备ID：{}，系统错误：{}",
                deviceRegisterReq != null ? deviceRegisterReq.getDeviceId() : "unknown", e.getMessage(), e);
            return AjaxResult.error("设备登录失败：" + e.getMessage());
        }
    }

    @Override
    public AjaxResult<Long> addChannel(DeviceChannelReq req) {
        try {
            Assert.notNull(req, "设备通道请求不能为空");
            Assert.hasText(req.getDeviceId(), "设备ID不能为空");
            Assert.hasText(req.getChannelId(), "通道ID不能为空");

            log.info("添加设备通道开始，设备ID：{}，通道ID：{}", req.getDeviceId(), req.getChannelId());

            DeviceChannelDTO deviceChannelDTO = DeviceChannelDTO.req2dto(req);
            Long channelId = deviceChannelManager.saveOrUpdate(deviceChannelDTO);

            log.info("添加设备通道成功，设备ID：{}，通道ID：{}，数据库ID：{}",
                req.getDeviceId(), req.getChannelId(), channelId);

            return AjaxResult.success("通道添加成功", channelId);

        } catch (ServiceException e) {
            log.error("添加设备通道失败，设备ID：{}，通道ID：{}，业务错误：{}",
                req != null ? req.getDeviceId() : "unknown",
                req != null ? req.getChannelId() : "unknown", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("添加设备通道失败，设备ID：{}，通道ID：{}，系统错误：{}",
                req != null ? req.getDeviceId() : "unknown",
                req != null ? req.getChannelId() : "unknown", e.getMessage(), e);
            return AjaxResult.error("通道添加失败：" + e.getMessage());
        }
    }

    @Override
    public AjaxResult<Void> updateDeviceInfo(DeviceInfoReq req) {
        try {
            Assert.notNull(req, "设备信息更新请求不能为空");
            Assert.hasText(req.getDeviceId(), "设备ID不能为空");
            Assert.hasText(req.getDeviceInfo(), "设备信息不能为空");

            log.info("更新设备信息开始，设备ID：{}", req.getDeviceId());

            DeviceDTO dtoByDeviceId = deviceManager.getDtoByDeviceId(req.getDeviceId());
            if (dtoByDeviceId == null) {
                log.warn("设备不存在，无法更新设备信息，设备ID：{}", req.getDeviceId());
                return AjaxResult.warn("设备不存在，无法更新设备信息");
            }

            DeviceDTO.ExtendInfo extendInfo = dtoByDeviceId.getExtendInfo();
            if (extendInfo == null) {
                extendInfo = new DeviceDTO.ExtendInfo();
                dtoByDeviceId.setExtendInfo(extendInfo);
            }
            extendInfo.setDeviceInfo(req.getDeviceInfo());

            Long deviceId = deviceManager.saveOrUpdate(dtoByDeviceId);

            log.info("更新设备信息成功，设备ID：{}，数据库ID：{}", req.getDeviceId(), deviceId);
            return AjaxResult.success("设备信息更新成功");

        } catch (ServiceException e) {
            log.error("更新设备信息失败，设备ID：{}，业务错误：{}",
                req != null ? req.getDeviceId() : "unknown", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新设备信息失败，设备ID：{}，系统错误：{}",
                req != null ? req.getDeviceId() : "unknown", e.getMessage(), e);
            return AjaxResult.error("设备信息更新失败：" + e.getMessage());
        }
    }

    @Override
    public AjaxResult<Boolean> keepalive(String deviceId) {
        try {
            Assert.isTrue(StringUtils.isNotBlank(deviceId), "设备ID不能为空");

            log.debug("设备心跳请求，设备ID：{}", deviceId);

            DeviceDTO byDeviceId = deviceManager.getDtoByDeviceId(deviceId);
            if (byDeviceId == null) {
                log.info("心跳失败：设备不存在，设备ID：{}", deviceId);
                return AjaxResult.success("设备不存在", false);
            }

            byDeviceId.setKeepaliveTime(LocalDateTime.now());
            byDeviceId.setStatus(DeviceConstant.Status.ONLINE);

            Long id = deviceManager.saveOrUpdate(byDeviceId);
            boolean success = id != null;

            if (success) {
                log.debug("设备心跳成功，设备ID：{}，数据库ID：{}", deviceId, id);
            } else {
                log.warn("设备心跳失败，设备ID：{}", deviceId);
            }

            return AjaxResult.success("心跳处理完成", success);

        } catch (ServiceException e) {
            log.error("设备心跳失败，设备ID：{}，业务错误：{}", deviceId, e.getMessage());
            return AjaxResult.error(e.getMessage(), false);
        } catch (Exception e) {
            log.error("设备心跳失败，设备ID：{}，系统错误：{}", deviceId, e.getMessage(), e);
            return AjaxResult.error("心跳处理失败：" + e.getMessage(), false);
        }
    }

    @Override
    public AjaxResult<Long> updateRemoteAddress(String deviceId, String ip, Integer port) {
        try {
            Assert.hasText(deviceId, "设备ID不能为空");
            Assert.hasText(ip, "IP地址不能为空");
            Assert.notNull(port, "端口不能为空");
            Assert.isTrue(port > 0 && port <= 65535, "端口范围必须在1-65535之间");

            log.info("更新设备远程地址开始，设备ID：{}，IP：{}，端口：{}", deviceId, ip, port);

            DeviceDTO byDeviceId = deviceManager.getDtoByDeviceId(deviceId);
            if (byDeviceId == null) {
                log.warn("设备不存在，无法更新远程地址，设备ID：{}", deviceId);
                return AjaxResult.error("设备不存在", null);
            }

            byDeviceId.setIp(ip);
            byDeviceId.setPort(port);
            byDeviceId.setUpdateTime(LocalDateTime.now());

            Long id = deviceManager.saveOrUpdate(byDeviceId);

            log.info("更新设备远程地址成功，设备ID：{}，IP：{}，端口：{}，数据库ID：{}",
                deviceId, ip, port, id);
            return AjaxResult.success("远程地址更新成功", id);

        } catch (ServiceException e) {
            log.error("更新设备远程地址失败，设备ID：{}，IP：{}，端口：{}，业务错误：{}",
                deviceId, ip, port, e.getMessage());
            return AjaxResult.error(e.getMessage(), null);
        } catch (Exception e) {
            log.error("更新设备远程地址失败，设备ID：{}，IP：{}，端口：{}，系统错误：{}",
                deviceId, ip, port, e.getMessage(), e);
            return AjaxResult.error("远程地址更新失败：" + e.getMessage(), null);
        }
    }

    @Override
    public AjaxResult<Void> offline(String deviceId) {
        try {
            Assert.hasText(deviceId, "设备ID不能为空");

            log.info("设备下线请求，设备ID：{}", deviceId);

            // 检查设备是否存在
            DeviceDTO deviceDTO = deviceManager.getDtoByDeviceId(deviceId);
            if (deviceDTO == null) {
                log.warn("设备不存在，无法下线，设备ID：{}", deviceId);
                return AjaxResult.warn("设备不存在，无需下线");
            }

            deviceManager.updateStatus(deviceId, DeviceConstant.Status.OFFLINE);

            log.info("设备下线成功，设备ID：{}", deviceId);
            return AjaxResult.success("设备下线成功");

        } catch (ServiceException e) {
            log.error("设备下线失败，设备ID：{}，业务错误：{}", deviceId, e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("设备下线失败，设备ID：{}，系统错误：{}", deviceId, e.getMessage(), e);
            return AjaxResult.error("设备下线失败：" + e.getMessage());
        }
    }
}
