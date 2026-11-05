package com.ehms.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.ehms.controller..*(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        log.info("Entering in Method : {}", methodName);
        log.info("Class Name : {}", className);
        log.info("Parameter Value : {}", Arrays.toString(joinPoint.getArgs()));

        Object result = joinPoint.proceed();

        log.info("Return Value : {}", result);
/**************************to show view in consol*****************************/
        if (result instanceof ModelAndView) {

            ModelAndView mav = (ModelAndView) result;

            log.info("Return Value : ModelAndView");
            log.info("View Name : {}", mav.getViewName());
            log.info("Model : {}", mav.getModel());

        } else {

            log.info("Return Value : {}", result);
        }
/********************************************************************************/
        return result;
    }
}
