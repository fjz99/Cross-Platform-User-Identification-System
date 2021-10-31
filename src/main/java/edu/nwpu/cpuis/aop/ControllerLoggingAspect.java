package edu.nwpu.cpuis.aop;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * @author fujiazheng
 */
@Aspect
@Slf4j
public class ControllerLoggingAspect {
    @Pointcut("execution(* edu.nwpu.cpuis.controller..*.*(..))")
    public void controllers() {
    }

    @Around("controllers()")
    public Object logAroundControllers(ProceedingJoinPoint joinPoint) {
        final String name = joinPoint.getTarget ().getClass ().getName () + "#" + joinPoint.getSignature ().getName ();
        try {
            log.info ("调用方法 {}", name);
            Object o = joinPoint.proceed ();
            log.info ("{} 方法调用成功，返回值 {}", name, o);
            return o;
        } catch (Throwable throwable) {
            log.error ("{} 方法调用失败", name);
            throwable.printStackTrace ();
            return null;
        }
    }
}
