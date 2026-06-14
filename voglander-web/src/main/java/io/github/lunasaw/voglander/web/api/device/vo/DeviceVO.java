package io.github.lunasaw.voglander.web.api.device.vo;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import lombok.Data;

import java.io.Serializable;

/**
 * 设备 VO 模型
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 创建时间 (unix时间戳，毫秒级)
     */
    private Long              createTime;

    /**
     * 更新时间 (unix时间戳，毫秒级)
     */
    private Long              updateTime;

    //设备ID
    private String deviceId;
    //状态 1在线 0离线
    private Integer status;
    //状态名称
    private String statusName;
    //自定义名称
    private String name;
    //IP
    private String ip;
    //端口
    private Integer port;

    /**
     * 注册时间 (unix时间戳，毫秒级)
     */
    private Long              registerTime;

    /**
     * 心跳时间 (unix时间戳，毫秒级)
     */
    private Long              keepaliveTime;
    //注册节点
    private String serverIp;
    /**
     * 协议类型
     */
    private Integer type;
    /**
     * 协议类型名称
     */
    private String typeName;

    /**
     * 设备种类
     */
    private Integer           subType;
    /**
     * 设备种类名称
     */
    private String            subTypeName;

    /**
     * 设备协议
     */
    private Integer           protocol;
    /**
     * 设备协议名称
     */
    private String            protocolName;

    /**
     * 该设备下的通道数（S1 列表展示，由 Controller 回填）
     */
    private Integer           channelCount;

    /**
     * 订阅意图开关状态（目录/位置/告警），由 Controller 批量回填。
     */
    private SubscriptionVO    subscription;

    //扩展字段
    private String extend;

    //扩展信息
    private ExtendInfoVO extendInfo;

    public static DeviceVO convertVO(DeviceDTO dto) {
        if (dto == null) {
            return null;
        }
        DeviceVO deviceVO = new DeviceVO();
        deviceVO.setId(dto.getId());
        deviceVO.setCreateTime(dto.createTimeToEpochMilli());
        deviceVO.setUpdateTime(dto.updateTimeToEpochMilli());
        deviceVO.setDeviceId(dto.getDeviceId());
        deviceVO.setStatus(dto.getStatus());
        deviceVO.setStatusName(getStatusName(dto.getStatus()));
        deviceVO.setName(dto.getName());
        deviceVO.setIp(dto.getIp());
        deviceVO.setPort(dto.getPort());
        deviceVO.setRegisterTime(dto.registerTimeToEpochMilli());
        deviceVO.setKeepaliveTime(dto.keepaliveTimeToEpochMilli());
        deviceVO.setServerIp(dto.getServerIp());
        deviceVO.setType(dto.getType());
        deviceVO.setTypeName(getTypeName(dto.getType()));

        // 解析设备种类和协议信息
        DeviceAgreementEnum agreementEnum = DeviceAgreementEnum.getByType(dto.getType());
        if (agreementEnum != null) {
            deviceVO.setSubType(agreementEnum.getSubType());
            deviceVO.setSubTypeName(getSubTypeName(agreementEnum.getSubType()));
            deviceVO.setProtocol(agreementEnum.getProtocol());
            deviceVO.setProtocolName(getProtocolName(agreementEnum.getProtocol()));
        }

        deviceVO.setExtend(dto.getExtend());

        if (dto.getExtendInfo() != null) {
            ExtendInfoVO extendInfoVO = new ExtendInfoVO();
            extendInfoVO.setSerialNumber(dto.getExtendInfo().getSerialNumber());
            extendInfoVO.setTransport(dto.getExtendInfo().getTransport());
            extendInfoVO.setExpires(dto.getExtendInfo().getExpires());
            extendInfoVO.setPassword(dto.getExtendInfo().getPassword());
            extendInfoVO.setStreamMode(dto.getExtendInfo().getStreamMode());
            extendInfoVO.setCharset(dto.getExtendInfo().getCharset());
            extendInfoVO.setDeviceInfo(dto.getExtendInfo().getDeviceInfo());
            extendInfoVO.setDeviceStatus(dto.getExtendInfo().getDeviceStatus());
            extendInfoVO.setPtzPosition(dto.getExtendInfo().getPtzPosition());
            extendInfoVO.setPresets(dto.getExtendInfo().getPresets());
            extendInfoVO.setConfig(dto.getExtendInfo().getConfig());
            extendInfoVO.setConfigDownload(dto.getExtendInfo().getConfigDownload());
            extendInfoVO.setSdCardStatus(dto.getExtendInfo().getSdCardStatus());
            deviceVO.setExtendInfo(extendInfoVO);
        }

        return deviceVO;
    }

    /**
     * 获取状态显示名称
     */
    private static String getStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case 1:
                return "在线";
            case 0:
                return "离线";
            default:
                return "异常";
        }
    }

    /**
     * 获取设备类型显示名称
     */
    private static String getTypeName(Integer type) {
        DeviceAgreementEnum agreementEnum = DeviceAgreementEnum.getByType(type);
        return agreementEnum != null ? agreementEnum.getDesc() : "未知类型";
    }

    /**
     * 获取设备种类显示名称
     */
    private static String getSubTypeName(Integer subType) {
        DeviceSubTypeEnum subTypeEnum = DeviceSubTypeEnum.getByType(subType);
        return subTypeEnum != null ? subTypeEnum.getDesc() : "未知种类";
    }

    /**
     * 获取设备协议显示名称
     */
    private static String getProtocolName(Integer protocol) {
        DeviceProtocolEnum protocolEnum = DeviceProtocolEnum.getByType(protocol);
        return protocolEnum != null ? protocolEnum.getDesc() : "未知协议";
    }

    @Data
    public static class ExtendInfoVO {
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
         * 注册有效期
         */
        private int expires;

        /**
         * 密码
         */
        private String password;

        /**
         * 数据流传输模式
         */
        private String streamMode;

        /**
         * 编码
         */
        private String charset;

        /**
         * 设备信息
         */
        private String deviceInfo;

        // ================ 设备应答快照（S3 入站响应回填）================

        /**
         * 设备状态响应快照
         */
        private String deviceStatus;

        /**
         * 云台位置响应快照
         */
        private String ptzPosition;

        /**
         * 预置位响应快照
         */
        private String presets;

        /**
         * 设备配置响应快照
         */
        private String config;

        /**
         * 配置下载响应快照
         */
        private String configDownload;

        /**
         * SD 卡状态响应快照
         */
        private String sdCardStatus;
    }

    /**
     * 订阅意图开关状态（GB28181-2022 §9.11）。
     */
    @Data
    public static class SubscriptionVO {
        /** 目录订阅意图是否开启 */
        private boolean catalog;
        /** 位置订阅意图是否开启 */
        private boolean position;
        /** 告警订阅意图是否开启 */
        private boolean alarm;
    }
}