package io.github.lunasaw.voglander.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author luna
 */
public class CacheConstants {
    /**
     * 登录用户 redis key
     */
    public static final String     LOGIN_TOKEN_KEY             = "login_tokens:";
    /**
     * 验证码 redis key
     */
    public static final String     CAPTCHA_CODE_KEY            = "captcha_codes:";
    /**
     * 参数管理 cache key
     */
    public static final String     SYS_CONFIG_KEY              = "sys_config:";
    /**
     * 字典管理 cache key
     */
    public static final String     SYS_DICT_KEY                = "sys_dict:";
    /**
     * 防重提交 redis key
     */
    public static final String     REPEAT_SUBMIT_KEY           = "repeat_submit:";
    /**
     * 限流 redis key
     */
    public static final String     RATE_LIMIT_KEY              = "rate_limit:";
    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String     PWD_ERR_CNT_KEY             = "pwd_err_cnt:";
    /**
     * 默认的redis缓存时间
     */
    public static volatile int     DEFAULT_REDIS_CACHE_SECONDS = 1800;
    /**
     * 本地缓存开关
     */
    public static volatile boolean TAIR_USE_LOCAL_CACHE        = false;

    /**
     * 是否打印TAIR的耗时日志
     */
    public static volatile boolean PRINT_TAIR_USE_TIME_LOG     = false;

    /**
     * tair 多数据key value的数量
     */
    public static volatile int     TAIR_MULTI_DATA_SIZE        = 100;
}
