package com.neu.aspect;

import com.neu.annotation.AutoFill;
import com.neu.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    // 定义一个切入点
    @Pointcut("execution(* com.neu.mapper..*(..)) && @annotation(com.neu.annotation.AutoFill)")
    public void autoFillPointCut() {
    }

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行自动填充");
        // 获取被拦截的方法类型
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();// 获取方法签名
        AutoFill autoFill = methodSignature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();
        //获取被拦截的对象
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            log.info("没有需要自动填充的对象");
            return;
        }
        Object entity = args[0];
        //准备赋值的数据
        LocalDateTime now = LocalDateTime.now();
        //根据方法类型对不同对象进行赋值
        if (operationType == OperationType.INSERT) {
            try {
                Method setCreateTime = entity.getClass().getDeclaredMethod("setCreateTime", LocalDateTime.class);
                setCreateTime.invoke(entity, now);
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                log.error("自动填充字段出错", e);
            }
        } else if (operationType == OperationType.UPDATE) {
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
                setUpdateTime.invoke(entity, now);
            } catch (Exception e) {
                log.error("自动填充字段出错", e);
            }
        }
    }
}
