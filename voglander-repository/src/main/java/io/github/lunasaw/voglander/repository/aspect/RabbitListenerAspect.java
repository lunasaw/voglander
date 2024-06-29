package io.github.lunasaw.voglander.repository.aspect;

import org.apache.commons.lang3.StringUtils;
import org.apache.skywalking.apm.toolkit.trace.TraceContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import io.github.lunasaw.voglander.common.constant.Constants;

@Aspect
@Component
public class RabbitListenerAspect {

    @Pointcut("@annotation(org.springframework.amqp.rabbit.annotation.RabbitHandler)")
    public void rabbitListener() {}

    @Around("rabbitListener()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return joinPoint.proceed();
        }
        // 获取请求对象
        Object req = args[0];
        if (req == null) {
            return joinPoint.proceed();
        }
        String traceId;
        try {
            JSONObject jsonObject = JSON.parseObject(req.toString());
            traceId = jsonObject.getString(Constants.SKY_WALKING_TID);
            if (traceId == null) {
                traceId = TraceContext.traceId();
            }
        } catch (Exception e) {
            traceId = TraceContext.traceId();
        }
        MDC.put(Constants.SKY_WALKING_TID, traceId);

        return joinPoint.proceed();
    }
}