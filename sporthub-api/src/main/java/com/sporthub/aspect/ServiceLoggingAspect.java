package com.sporthub.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class ServiceLoggingAspect {

    @Around("execution(public * com.sporthub.service..*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        long startTime = System.nanoTime();

        try {
            return joinPoint.proceed();
        } finally {
            long endTime = System.nanoTime();
            long durationMs = (endTime - startTime) / 1_000_000;

            String className = joinPoint.getSignature()
                    .getDeclaringType()
                    .getSimpleName();

            String methodName = joinPoint.getSignature().getName();

            log.info(
                    "Service execution - {}.{} took {} ms",
                    className,
                    methodName,
                    durationMs
            );
        }
    }
}