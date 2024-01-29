package io.github.lunasaw.voglander.common.util;

import java.util.Collection;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

/**
 * @author zhuhuanhuan
 * @date 2020-02-27 下午5:18
 **/
public class AssertUtil {
    /**
     * 私有构造方法
     */
    private AssertUtil() {}

    /**
     * 校验指定对象不能为null
     *
     * @param object 对象
     * @param exceptionEnumCode 运行时异常
     */
    public static void notNull(Object object, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        isTrue(object != null, exceptionEnumCode, extendInfos);
    }

    /**
     * 校验指定对象不能为null
     *
     * @param object 对象
     * @param exceptionEnumCode 运行时异常
     */
    public static void isNull(Object object, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        isTrue(object == null, exceptionEnumCode, extendInfos);
    }

    /**
     * 校验指定集合不能为空
     *
     * @param collection 集合
     * @param exceptionEnumCode 运行时异常
     */
    public static void notEmpty(Collection collection, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        isTrue(CollectionUtils.isNotEmpty(collection),
            exceptionEnumCode, extendInfos);
    }

    /**
     * 校验指定集合不能为空
     *
     * @param collection 集合
     * @param exceptionEnumCode 运行时异常
     */
    public static void isEmpty(Collection collection, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        isTrue(CollectionUtils.isEmpty(collection),
            exceptionEnumCode, extendInfos);
    }

    // /**
    // * 校验指定对象不能为null
    // *
    // * @param map map集合
    // * @param exceptionEnumCode 运行时异常
    // */
    // public static void notEmpty(Map map, ServiceExceptionEnum exceptionEnumCode,
    // String... extendInfos) {
    // isTrue(MapUtils.isNotEmpty(map), exceptionEnumCode, extendInfos);
    // }

    /**
     * 校验字符串不能为空
     *
     * @param str 字符串
     * @param exceptionEnumCode 运行时异常
     */
    public static void notBlank(String str, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        if (StringUtils.isBlank(str)) {
            isTrue(false, exceptionEnumCode, extendInfos);
        }
    }

    /**
     * 校验指定条件为true
     *
     * @param condition 条件
     * @param exceptionEnumCode 运行时异常
     */
    public static void isFalse(boolean condition, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        if (condition) {
            fail(exceptionEnumCode, extendInfos);
        }
    }

    /**
     * 校验指定条件为true
     *
     * @param condition 条件
     * @param exceptionEnumCode 运行时异常
     */
    public static void isTrue(boolean condition, ServiceExceptionEnum exceptionEnumCode,
        String... extendInfos) {
        if (!condition) {
            fail(exceptionEnumCode, extendInfos);
        }
    }

    /**
     * 抛出指定运行时异常
     *
     * @param exceptionEnumCode 运行时异常
     */
    public static void fail(ServiceExceptionEnum exceptionEnumCode, String... extendInfos) {
        Assert.notNull(exceptionEnumCode, "exceptionEnumCode is null");

        throw new ServiceException(exceptionEnumCode, extendInfos);
    }
}