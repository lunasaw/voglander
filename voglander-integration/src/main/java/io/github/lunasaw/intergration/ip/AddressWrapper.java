package io.github.lunasaw.intergration.ip;

import com.alibaba.fastjson2.JSON;
import com.luna.common.net.HttpUtils;
import io.github.lunasaw.intergration.doamin.ip.dto.IpAddressResponse;
import io.github.lunasaw.voglander.common.util.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * 获取地址类
 *
 * @author luna
 */
@Slf4j
public class AddressWrapper {

    // IP地址查询
    public static final String IP_URL = "http://whois.pconline.com.cn";

    public static final String IP_PATH = "/ipJson.jsp";

    // 未知地址
    public static final String UNKNOWN = "XX XX";

    public static String getRealAddressByIP(String ip) {
        // 内网不查询
        if (IpUtils.internalIp(ip)) {
            return "内网IP";
        }
        IpAddressResponse response = getRealAddressResponseByIP(ip);
        if (response == null) {
            return UNKNOWN;
        }

        return response.getRegion() + " " + response.getCity();
    }

    public static IpAddressResponse getRealAddressResponseByIP(String ip) {

        try {
            String rspStr = HttpUtils.doGetHandler(IP_URL, IP_PATH + "?ip=" + ip + "&json=true", new HashMap<>(), new HashMap<>());
            if (StringUtils.isEmpty(rspStr)) {
                log.error("获取地理位置异常 {}", ip);
                return null;
            }
            return JSON.parseObject(rspStr, IpAddressResponse.class);
        } catch (Exception e) {
            log.error("获取地理位置异常 {}", ip);
        }

        return null;
    }

    public static void main(String[] args) {
        System.out.println(AddressWrapper.getRealAddressByIP("114.252.235.140"));
    }
}
