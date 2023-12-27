# voglander

[![Maven Central](https://img.shields.io/maven-central/v/io.github.lunasaw/voglander)](https://mvnrepository.com/artifact/io.github.lunasaw/voglander)
[![GitHub license](https://img.shields.io/badge/MIT_License-blue.svg)](https://raw.githubusercontent.com/lunasaw/voglander/master/LICENSE)

[www.isluna.ml](http://lunasaw.github.io)

voglander

基于[sip-proxy](https://github.com/lunasaw/gb28181-proxy)等框架搭建的视频监控平台。支持模块化集群化部署，支持海康、大华、宇视、中维等主流监控设备接入，支持多种信令协议。包括但不限于GB28181、GT1078、ONVIF等协议。



> 引入

```xml

<dependency>
    <groupId>io.github.lunasaw</groupId>
    <artifactId>voglander</artifactId>
    <version>${last.version}</version>
</dependency>
```

# 代码规范

- 后端使用同一份代码格式化膜模板ali-code-style.xml，ecplise直接导入使用，idea使用Eclipse Code Formatter插件配置xml后使用。
- 前端代码使用vs插件的Beautify格式化，缩进使用TAB
- 后端代码非特殊情况准守P3C插件规范
- 注释要尽可能完整明晰，提交的代码必须要先格式化
- xml文件和前端一样，使用TAB缩进
