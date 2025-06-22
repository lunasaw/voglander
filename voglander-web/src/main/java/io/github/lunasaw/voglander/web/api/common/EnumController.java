package io.github.lunasaw.voglander.web.api.common;

import io.github.lunasaw.voglander.common.constant.ApiConstant;
import io.github.lunasaw.voglander.common.domain.AjaxResult;
import io.github.lunasaw.voglander.common.enums.DeviceAgreementEnum;
import io.github.lunasaw.voglander.common.enums.DeviceProtocolEnum;
import io.github.lunasaw.voglander.common.enums.DeviceSubTypeEnum;
import io.github.lunasaw.voglander.web.api.common.vo.EnumVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 枚举控制器 - 提供各种枚举值的查询接口
 * 
 * @author luna
 * @date 2024/01/30
 */
@RestController
@RequestMapping(ApiConstant.API_INDEX_V1 + "/enum")
public class EnumController {

    /**
     * 获取设备种类枚举
     * 
     * @return 设备种类列表
     */
    @GetMapping("/device-sub-types")
    public AjaxResult getDeviceSubTypes() {
        List<EnumVO> subTypes = Arrays.stream(DeviceSubTypeEnum.values())
            .map(e -> EnumVO.of(e.getType(), e.getCode(), e.getDesc()))
            .collect(Collectors.toList());
        return AjaxResult.success(subTypes);
    }

    /**
     * 获取设备协议枚举
     * 
     * @return 设备协议列表
     */
    @GetMapping("/device-protocols")
    public AjaxResult getDeviceProtocols() {
        List<EnumVO> protocols = Arrays.stream(DeviceProtocolEnum.values())
            .map(e -> EnumVO.of(e.getType(), e.getCode(), e.getDesc()))
            .collect(Collectors.toList());
        return AjaxResult.success(protocols);
    }

    /**
     * 获取设备协议类型枚举
     * 
     * @return 设备协议类型列表
     */
    @GetMapping("/device-agreement-types")
    public AjaxResult getDeviceAgreementTypes() {
        List<EnumVO> agreementTypes = Arrays.stream(DeviceAgreementEnum.values())
            .map(e -> EnumVO.of(e.getType(), null, e.getDesc()))
            .collect(Collectors.toList());
        return AjaxResult.success(agreementTypes);
    }

    /**
     * 根据设备种类和协议计算协议类型
     * 
     * @param subType 设备种类
     * @param protocol 设备协议
     * @return 协议类型信息
     */
    @GetMapping("/device-agreement-type")
    public AjaxResult getDeviceAgreementType(Integer subType, Integer protocol) {
        if (subType == null || protocol == null) {
            return AjaxResult.error("设备种类和协议不能为空");
        }

        DeviceAgreementEnum agreementEnum = DeviceAgreementEnum.getBySubTypeAndProtocol(subType, protocol);
        if (agreementEnum == null) {
            return AjaxResult.error("不支持的设备种类和协议组合");
        }

        EnumVO result = EnumVO.of(agreementEnum.getType(), null, agreementEnum.getDesc());
        return AjaxResult.success(result);
    }

    /**
     * 获取所有枚举数据
     * 
     * @return 包含所有枚举的Map
     */
    @GetMapping("/all")
    public AjaxResult getAllEnums() {
        // 设备种类
        List<EnumVO> subTypes = Arrays.stream(DeviceSubTypeEnum.values())
            .map(e -> EnumVO.of(e.getType(), e.getCode(), e.getDesc()))
            .collect(Collectors.toList());

        // 设备协议
        List<EnumVO> protocols = Arrays.stream(DeviceProtocolEnum.values())
            .map(e -> EnumVO.of(e.getType(), e.getCode(), e.getDesc()))
            .collect(Collectors.toList());

        // 设备协议类型
        List<EnumVO> agreementTypes = Arrays.stream(DeviceAgreementEnum.values())
            .map(e -> EnumVO.of(e.getType(), null, e.getDesc()))
            .collect(Collectors.toList());

        // 传输协议
        List<EnumVO> transports = Arrays.asList(
            EnumVO.of(1, "UDP", "UDP协议"),
            EnumVO.of(2, "TCP", "TCP协议"));

        // 字符编码
        List<EnumVO> charsets = Arrays.asList(
            EnumVO.of(1, "UTF-8", "UTF-8编码"),
            EnumVO.of(2, "GBK", "GBK编码"),
            EnumVO.of(3, "GB2312", "GB2312编码"));

        // 设备状态
        List<EnumVO> deviceStatus = Arrays.asList(
            EnumVO.of(1, "ONLINE", "在线"),
            EnumVO.of(0, "OFFLINE", "离线"));

        java.util.Map<String, List<EnumVO>> allEnums = new java.util.HashMap<>();
        allEnums.put("deviceSubTypes", subTypes);
        allEnums.put("deviceProtocols", protocols);
        allEnums.put("deviceAgreementTypes", agreementTypes);
        allEnums.put("transports", transports);
        allEnums.put("charsets", charsets);
        allEnums.put("deviceStatus", deviceStatus);

        return AjaxResult.success(allEnums);
    }
}