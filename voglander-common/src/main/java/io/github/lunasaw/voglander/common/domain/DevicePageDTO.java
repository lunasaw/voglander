package io.github.lunasaw.voglander.common.domain;

import com.alibaba.fastjson2.JSON;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;

/**
 * 设备分页查询 DTO
 * @author luna
 * @date 2024/01/30
 */
@Data
public class DevicePageDTO implements Serializable {

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
    /**
     * 协议类型 {@link DeviceAgreementEnum}
     */
    private Integer type;
    //扩展字段
    private String extend;

    private ExtendInfo extendInfo;

    public static ExtendInfo parseExtendInfo(String extentInfo) {
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

        /**
         * 设备信息
         */
        private String deviceInfo;
    }
}