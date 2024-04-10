package io.github.lunasaw.voglander.common.anno;


import io.github.lunasaw.voglander.common.constant.CacheConstants;
import io.github.lunasaw.voglander.common.enums.LimitType;

import java.lang.annotation.*;

/**
 * 限流注解
 * 
 * @author luna
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter
{
    /**
     * 限流key
     */
    String key() default CacheConstants.RATE_LIMIT_KEY;

    /**
     * 限流时间,单位秒
     */
    int time() default 60;

    /**
     * 限流次数
     */
    int count() default 100;

    /**
     * 限流类型
     */
    LimitType limitType() default LimitType.DEFAULT;
}
