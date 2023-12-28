package io.github.lunasaw.voglander.intergration.doamin.ip.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

@Data
public class IpAddressResponse {

    @JSONField(name = "regionCode")
    private String regionCode;

    @JSONField(name = "regionNames")
    private String regionNames;

    @JSONField(name = "proCode")
    private String proCode;

    @JSONField(name = "err")
    private String err;

    @JSONField(name = "city")
    private String city;

    @JSONField(name = "cityCode")
    private String cityCode;

    @JSONField(name = "ip")
    private String ip;

    @JSONField(name = "pro")
    private String pro;

    @JSONField(name = "region")
    private String region;

    @JSONField(name = "addr")
    private String addr;
}