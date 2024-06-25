package io.github.lunasaw.voglander.web.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.alibaba.fastjson2.serializer.SerializerFeature;
import com.alibaba.fastjson2.support.config.FastJsonConfig;
import com.alibaba.fastjson2.support.spring.FastJsonHttpMessageConverter;

/**
 * @author luna
 */
@Configuration
public class JsonWebConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
            // List字段如果为null,输出为[],而非nul
            SerializerFeature.WriteNullListAsEmpty,
            // 数值字段如果为null,输出为0,而非null
            SerializerFeature.WriteNullStringAsEmpty,
            // 禁用循环引用检测
            SerializerFeature.DisableCircularReferenceDetect,
            // Boolean字段如果为null,输出为false,而非null
            SerializerFeature.WriteNullBooleanAsFalse,
            // 输出为字符串的empty（""）而非null
            SerializerFeature.WriteDateUseDateFormat);
        converter.setFastJsonConfig(config);
        converters.add(converter);
    }
}
