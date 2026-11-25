package com.logy.pocketpay.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceExecutionTimeAspect {

    @Around("execution(public * com.logy.pocketpay..service..*(..))")
    public Object measureExecutionTime(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {

        long startTime = System.nanoTime();

        try {

            Object result = joinPoint.proceed();

            long durationMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.debug(
                    "SERVICE {} completed durationMs={}",
                    joinPoint.getSignature().toShortString(),
                    durationMs
            );

            return result;

        } catch (Throwable exception) {

            long durationMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.debug(
                    "SERVICE {} failed durationMs={} exception={}",
                    joinPoint.getSignature().toShortString(),
                    durationMs,
                    exception.getClass().getSimpleName()
            );

            throw exception;
        }
    }
}