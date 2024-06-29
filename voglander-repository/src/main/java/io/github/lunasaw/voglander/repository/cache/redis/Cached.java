package io.github.lunasaw.voglander.repository.cache.redis;

import java.lang.annotation.*;

/**
 * @author luna
 */
@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Cached {

    /**
     * 缓存key的前缀
     *
     * @return
     */
    String cacheKeyPrefix();

    /**
     * 回写 cache 时从db结果中读取的字段，仅用于返回 Collection 和 单个Object 的情况
     * 当返回值为Collection时，该字段不能为空
     * 当缓存的key由多个值拼接的时候，仅支持key使用 _ 拼接
     * 返回结果 Map 则不需要配置
     */
    String[] keyNameInReturnObject() default {};

    /**
     * 缓存过期时间，单位秒
     *
     * @return
     */
    int expireTime() default 86400;

    /**
     * 是否缓存空值，如果需要cache的对象基数比较大，如开启支持null，应将过期时间设短，防止过度膨胀。
     * 
     * @return
     */
    boolean cacheNull() default false;

    /**
     * 缓存key在参数中的下标，默认为0，即第一个参数。
     */
    int keyParamIndex() default 0;

    /**
     * 缓存value的最大值，防止redis过度膨胀，仅用于返回Collection和Map的情况
     *
     * @return
     */
    int maxValueSize() default 20;
}
