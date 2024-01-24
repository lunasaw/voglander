package io.github.lunasaw.voglander.repository.manager;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.lunasaw.voglander.common.exception.ServiceException;
import io.github.lunasaw.voglander.common.exception.ServiceExceptionEnum;
import io.github.lunasaw.voglander.repository.redis.RedisCache;

/**
 * @author luna
 **/
@Component
public class ConcurrentProcessHelper {

    private static final Logger    LOGGER          = LoggerFactory.getLogger(ConcurrentProcessHelper.class);
    /**
     * uniq的缓存失效时间,单位秒, 2小时
     */
    public static volatile Integer UNIQ_CACHE_TIME = 2 * 60 * 60;
    @Resource
    private RedisCache             redisWrapper;
    @Resource
    private TransactionTemplate    transactionTemplate;

    /**
     * 包含了分布式并发控制和超时控制
     *
     * @param bizKey
     * @param callbackHandler
     */
    public void process(String bizKey, CallbackHandler callbackHandler) {
        try {
            LOGGER.warn("bizKey start process, bizKey:{}", bizKey);
            long invalidTime = System.currentTimeMillis() + UNIQ_CACHE_TIME * 1000;
            boolean locked = redisWrapper.setCacheObjectIfAbsent(bizKey, String.valueOf(invalidTime), UNIQ_CACHE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                LOGGER.error("bizKey already processed, bizKey:{}", bizKey);
                throw new ServiceException(ServiceExceptionEnum.BIZ_KEY_ALREADY_PROCESSED_ERROR);
            }

            boolean executeRet = callbackHandler.handle();
            if (!executeRet) {
                LOGGER.error("callbackHandler processed failed, bizKey:{}", bizKey);
                throw new ServiceException(ServiceExceptionEnum.SYSTEM_ERROR);
            }

            LOGGER.warn("bizKey process success, bizKey:{} ", bizKey);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("bizKey process failed, bizKey:{}", bizKey, e);
            throw new ServiceException(ServiceExceptionEnum.SYSTEM_ERROR);
        } finally {
            // del内部已吃掉异常，所以key不存在也可尝试
            redisWrapper.deleteKey(bizKey);
        }
    }

    /**
     * 包含了分布式并发控制和超时控制
     *
     * @param bizKey
     * @param transactionCallback
     */
    public void process(String bizKey, TransactionCallback transactionCallback) {
        try {
            LOGGER.warn("bizKey start process, bizKey:{}", bizKey);
            long invalidTime = System.currentTimeMillis() + UNIQ_CACHE_TIME * 1000;
            boolean locked = redisWrapper.setCacheObjectIfAbsent(bizKey, String.valueOf(invalidTime), UNIQ_CACHE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                LOGGER.error("bizKey already processed, bizKey:{}", bizKey);
                throw new ServiceException(ServiceExceptionEnum.BIZ_KEY_ALREADY_PROCESSED_ERROR);
            }

            boolean executeRet = (Boolean)transactionTemplate.execute(transactionCallback);
            if (!executeRet) {
                LOGGER.error("transactionCallback processed failed, bizKey:{}", bizKey);
                throw new ServiceException(ServiceExceptionEnum.SYSTEM_ERROR);
            }

            LOGGER.warn("bizKey process success, bizKey:{} ", bizKey);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("bizKey process failed, bizKey:{}", bizKey, e);
            throw new ServiceException(ServiceExceptionEnum.SYSTEM_ERROR);
        } finally {
            // del内部已吃掉异常，所以key不存在也可尝试
            redisWrapper.deleteKey(bizKey);
        }
    }
}