package io.github.lunasaw.voglander.web.api.device.vo;

import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.manager.domaon.dto.DeviceDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 设备 VO 模型
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Date createTime;
    private Date updateTime;
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
    //注册时间
    private Date registerTime;
    //心跳时间
    private Date keepaliveTime;
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
        deviceVO.setCreateTime(dto.getCreateTime());
        deviceVO.setUpdateTime(dto.getUpdateTime());
        deviceVO.setDeviceId(dto.getDeviceId());
        deviceVO.setStatus(dto.getStatus());
        deviceVO.setStatusName(getStatusName(dto.getStatus()));
        deviceVO.setName(dto.getName());
        deviceVO.setIp(dto.getIp());
        deviceVO.setPort(dto.getPort());
        deviceVO.setRegisterTime(dto.getRegisterTime());
        deviceVO.setKeepaliveTime(dto.getKeepaliveTime());
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
    }
}