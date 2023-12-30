package io.github.lunasaw.voglander.repository.domain.dto;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import io.github.lunasaw.voglander.repository.domain.entity.DeviceDO;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

/**
 * @author chenzhangyue
 * @since 2023-12-28 11:11:54
 */
@SuppressWarnings("serial")
@Data
public class DeviceDTO implements Serializable {

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
    private Long id;
    private Date createTime;
    private Date updateTime;
    //设备ID
    private String deviceId;
    //状态 1在线 0离线
    private Integer status;
    //自定义名称
    private String name;
    //IP
    private String ip;
    //端口
    private Integer port;
    //注册时间
    private Date registerTime;
    //心跳时间
    private Date keepaliveTime;
    //注册节点
    private String serverIp;
    //扩展字段
    private String extend;

    private ExtendInfo extendInfo;

    public static DeviceDO convertDO(DeviceDTO dto) {
        if (dto == null) {
            return null;
        }
        DeviceDO deviceDO = new DeviceDO();
        deviceDO.setId(dto.getId());
        deviceDO.setCreateTime(dto.getCreateTime());
        deviceDO.setUpdateTime(dto.getUpdateTime());
        deviceDO.setDeviceId(dto.getDeviceId());
        deviceDO.setStatus(dto.getStatus());
        deviceDO.setName(dto.getName());
        deviceDO.setIp(dto.getIp());
        deviceDO.setPort(dto.getPort());
        deviceDO.setRegisterTime(dto.getRegisterTime());
        deviceDO.setKeepaliveTime(dto.getKeepaliveTime());
        deviceDO.setServerIp(dto.getServerIp());
        deviceDO.setExtend(JSON.toJSONString(dto.getExtendInfo()));
        return deviceDO;
    }

    public static DeviceDTO convertDTO(DeviceDO deviceDO) {
        if (deviceDO == null) {
            return null;
        }
        DeviceDTO deviceDTO = new DeviceDTO();
        deviceDTO.setId(deviceDO.getId());
        deviceDTO.setCreateTime(deviceDO.getCreateTime());
        deviceDTO.setUpdateTime(deviceDO.getUpdateTime());
        deviceDTO.setDeviceId(deviceDO.getDeviceId());
        deviceDTO.setStatus(deviceDO.getStatus());
        deviceDTO.setName(deviceDO.getName());
        deviceDTO.setIp(deviceDO.getIp());
        deviceDTO.setPort(deviceDO.getPort());
        deviceDTO.setRegisterTime(deviceDO.getRegisterTime());
        deviceDTO.setKeepaliveTime(deviceDO.getKeepaliveTime());
        deviceDTO.setServerIp(deviceDO.getServerIp());
        deviceDTO.setExtend(deviceDO.getExtend());

        ExtendInfo extendObj = getExtendObj(deviceDO.getExtend());
        if (extendObj.getCharset() == null) {
            extendObj.setCharset("UTF-8");
        }
        deviceDTO.setExtendInfo(extendObj);
        return deviceDTO;
    }

    public static ExtendInfo getExtendObj(String extentInfo) {
        if (StringUtils.isBlank(extentInfo)) {
            return new ExtendInfo();
        }
        String extend = Optional.of(extentInfo).orElse(StringUtils.EMPTY);
        return Optional.ofNullable(JSON.parseObject(extend, ExtendInfo.class)).orElse(new ExtendInfo());
    }

    @Data
    public static class ExtendInfo {

        /**
         * 设备序列号
         */
        private String serialNumber;

        /**
         * 传输协议
         * UDP/TCP
         */
        private String transport;

        /**
         * 域
         */
        private String realm;

        /**
         * 通道个数
         */
        private int channelCount;

        /**
         * 注册有效期
         */
        private int expires;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据流传输模式
         * UDP:udp传输
         * TCP-ACTIVE：tcp主动模式
         * TCP-PASSIVE：tcp被动模式
         */
        private String streamMode;

        /**
         * 编码
         */
        private String charset;

    }

}
